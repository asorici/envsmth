from client.decorators import allow_anonymous_profile, secure_required
from coresql.forms import CheckinForm, LoginForm, ClientRegistrationForm
from coresql.models import Environment, Area, UserContext, ResearchProfile
from tastypie.exceptions import ImmediateHttpResponse


def create_anonymous(request):
    from client.decorators import create_anonymous_user_profile
    from django.contrib.auth import login, authenticate, SESSION_KEY
    
    if request.user.is_anonymous():
        # If we have an anonymous user, then we have to create a temporary user
        user, username = create_anonymous_user_profile()
        request.user = None
        user = authenticate(username=username)
            
        # Set the user id in the session here to prevent the login
        # call cycling the session key.
        request.session[SESSION_KEY] = user.id
        login(request, user)
        
    return create_anonymous_succeeded(request)
    

def delete_anonymous(request):
    if not request.user.is_anonymous():
        user_profile = request.user.get_profile()
        if user_profile.is_anonymous:
            request.user.delete()
    
    return delete_anonymous_succeeded(request)


#@secure_required
def register(request):
    #from django_facebook.connect import connect_user
    from django.contrib.auth import login
    from django.utils import simplejson
    
    
    #if request.method.upper() == "POST":
    if request.method.upper() in ["POST", "GET"]:
        new_user = None
        
        form = ClientRegistrationForm(data=request.REQUEST)
        #form = ClientRegistrationForm(data=request.POST)
        if form.is_valid():
            new_user = form.save()
            ## we have created new user, now let's log them in
            ## we already have the user instance, so for authentication 
            ## just set the backend as django.contrib.auth.backends.ModelBackend
            new_user.backend = 'django.contrib.auth.backends.ModelBackend'
            login(request, new_user)
            
            #fb_access_token = request.POST.get('fb_access_token')
            #if fb_access_token:
                ## if we have a fb_access_token then we should also try to connect the data from facebook to
                ## the user's profile
            #    connect_user(request, fb_access_token)
            
            research_profile = request.REQUEST.get('research_profile')
            if research_profile:
                ## we receive it as a json string, load it and use it
                research_profile_data = simplejson.loads(research_profile)
                rp = ResearchProfile(**research_profile_data)
                rp.save()
                new_user.get_profile().research_profile = rp
                new_user.get_profile().save()
                new_user.save()
                
            return register_succeeded(request, new_user)
        
        return register_failed(request, data = form.errors)
    
    return register_failed(request)


#@secure_required
def login(request):
    from django.contrib.auth import login
    
    ''' retain the user making the request '''
    req_user = request.user
    
    form = LoginForm(request.REQUEST)
    #form = LoginForm(request.POST)
    
    if form.is_valid():
        user = form.get_user()
        if not user is None:
            ''' 
            Before login check if the request does not come from an Envived anonymous user.
            If so, then we can copy over that user's data and then delete him. 
            '''
            if not req_user.is_anonymous():
                req_user_profile = req_user.get_profile()
                ''' copy the UserContext data and c2dm_id from the requesting user - equivalent of a user switch ''' 
                user.get_profile().copy_from_profile(req_user_profile)
                
                ''' if the requesting user profile was anonymous remove it '''
                if req_user_profile.is_anonymous:
                    req_user.delete()
            
            login(request, user)
            return login_succeeded(request, user)
    
    return login_failed(request, data = form.errors)


def logout(request):
    from django.contrib.auth import logout
    
    ## log the user out
    user = getattr(request, 'user', None)
    
    ## when logging out we also checkout from the current location and set the c2dm_id to null 
    ## (because we might check in from another device and we don't want to send notifications to the
    ## old device in the mean time)
    try:
        if not user is None and not request.user.is_anonymous():
            user_profile = request.user.get_profile()
            
            user_profile.c2dm_id = None
            user_profile.context.currentEnvironment = None
            user_profile.context.currentArea = None
            
            #user_profile.context.save()
            user_profile.save()
    except UserContext.DoesNotExist:
        ## graceful error handling, if no context exists don't freak out, just ignore
        pass
    
    
    if hasattr(user, 'is_authenticated') and user.is_authenticated():
        ## it is not a django AnonymousUser - so it has a profile
        if user.get_profile().is_anonymous:
            ## if the user was anonymous, delete that user entry
            user.delete()
    
    ## set request.user to AnonymousUser and flush the session 
    logout(request)
    
    return logout_succeeded(request)
    


@allow_anonymous_profile
def checkin(request):
    ## request.user will have been filled in by whatever backend
    ## and will correspond to the user field in UserProfile
    
    ## run check to see if all required fields are present
    #check_form = CheckinForm(request.POST)
    
    check_form = CheckinForm(request.REQUEST)
    if check_form.is_valid():
        ## access location id and virtual flag from check_form.cleaned_data
        area_id = check_form.cleaned_data['area']
        env_id = check_form.cleaned_data['environment']
        virtual = check_form.cleaned_data['virtual']
        
        area = None 
        env = None
        area_env = None     ## keep a double
        
        try:
            area = Area.objects.get(id=area_id)
        except Exception:
            pass
        
        try:
            env = Environment.objects.get(id=env_id)
            area_env = env  ## by default its the same as the environment
        except Exception:
            pass
        
        ## is no valid area and environment data has been found => fail the checkin 
        if area is None and env is None:
            return checkin_failed(request, data = {"msg": "No location identified." })
        
        ## get user and correct env
        user_profile = request.user.get_profile()   ## should point to a UserProfile model
        
        ## if a valid area was found, use it's env field as corresponding environment
        if area:
            area_env = area.environment
        
        ''' virtual check-ins don't count for the moment - we don't keep a record '''
        if not virtual:
            try:
                context = UserContext.objects.get(user=user_profile)
                context.currentArea = area
                context.currentEnvironment = area_env
                context.virtual = virtual
                context.save()
            except UserContext.DoesNotExist:
                ## an entry does not yet exist so assign one now
                context = UserContext(user=user_profile, currentArea=area, 
                                      currentEnvironment=area_env, virtual = virtual)
                context.save()
        
        return checkin_succeeded(request, user_profile, area = area, env = area_env, virtual = virtual)
    
    else:
        return checkin_failed(request, data = check_form.errors)
            

def checkout(request):
    """
    Anonymous users aren't deleted here, but in /delete_anonymous
    Here we just check them out.
    """
    ## checkout is done by default from the current area or current env in the user_profile context
    try:
        if not request.user.is_anonymous():
            user_profile = request.user.get_profile()
            
            ## clear user context
            user_profile.context.currentEnvironment = None
            user_profile.context.currentArea = None
            user_profile.context.save()
        
    except UserContext.DoesNotExist:
        ## graceful error handling, if no context exists don't freak out, just ignore
        pass
    
    return checkout_succeeded(request)

    
###############################################################################################################
###############################################################################################################

def create_anonymous_succeeded(request):
    from client.api import UserResource
    response = {"success": True, "code": 200, "data" : {}}
    
    new_user = request.user
    if not new_user is None and not new_user.get_profile() is None:
        user_res_uri = UserResource().get_resource_uri(new_user.get_profile())
        response['data'].update({'user_uri' : user_res_uri})
    
    return view_response(request, response, 200)


def delete_anonymous_succeeded(request):
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    return view_response(request, response, 200)


def register_succeeded(request, user):
    from client.api import UserResource
    response = {"success": True, "code": 200, "data" : {}}
    
    if not user.get_profile().is_anonymous:
        user_res_uri = UserResource().get_resource_uri(user.get_profile())
        response['data'].update({'resource_uri' : user_res_uri})
        
        if user.first_name and user.last_name:
            response['data'].update({'first_name' : user.first_name, 'last_name' : user.last_name})
        
    return view_response(request, response, 200)


def register_failed(request, data = None):
    response = {"success": False, "code": 400, "data" : {"msg" : "Registration failed"}}
    if not data is None:
        response['data'].update(data)
    
    return view_response(request, response, 400)


def login_succeeded(request, user):
    from client.api import UserResource
    
    response = {"success": True, "code": 200, "data" : {}}
    
    if not user.get_profile().is_anonymous:
        user_res_uri = UserResource().get_resource_uri(user.get_profile())
        response['data'].update({'resource_uri' : user_res_uri})
        
        if user.first_name and user.last_name:
            response['data'].update({'first_name' : user.first_name, 'last_name' : user.last_name})
        
    ## TODO maybe include data about user
    return view_response(request, response, 200)


def logout_succeeded(request):
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    return view_response(request, response, 200)


def login_failed(request, data = None):
    response = {"success": False, "code": 401, "data": {"msg": "Login failed"}}
    
    if not data is None:
        response['data'].update(data)
    
    print "[DEBUG]>> LOGIN FAILED response: ", response
    
    return view_response(request, response, 401)


def checkin_succeeded(request, user, area = None, env = None, virtual = False):
    from client.api import AreaResource, EnvironmentResource, UserResource
    
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    
    ## add the value of the virtual checkin flag
    response['data'].update({'virtual': virtual})
    
    ## add the user URI as data payload - will be used for all notifications
    user_uri = UserResource().get_resource_uri(user)
    response['data'].update({'user_uri' : user_uri})
    
    if area:
        ## return data about the area resource
        ar = AreaResource()
        ar_item = ar.obj_get(pk=area.id)
        ar_bundle = ar.build_bundle(obj = ar_item, request=request)
        
        try:
            area_data = ar.full_dehydrate(ar_bundle).data
            response['data'].update(area_data)
        except ImmediateHttpResponse, error:
            return error.response
        
    elif env:
        ## return data about the environment resource
        envr = EnvironmentResource()
        envr_item = envr.obj_get(pk=env.id)
        envr_bundle = envr.build_bundle(obj = envr_item, request=request)
        
        try:
            env_data = envr.full_dehydrate(envr_bundle).data
            response['data'].update(env_data)
        except ImmediateHttpResponse, error:
            return error.response
        
        ## include the list of all areas that belong to this environment
        area_list = []
        for ar in env.areas.all():
            area_dict = {'name': ar.name,
                         'resource_uri': AreaResource().get_resource_uri(ar)
                         }
            
            ## check if there are tags for this area
            if ar.tags:
                area_dict['tags'] = ar.tags.getList()
            
            ## check if there is an image thumbnail url for the area
            if ar.img_thumbnail_url:
                area_dict['image_url'] = ar.img_thumbnail_url
            
            ## see how many people there are physically checked in at this area
            area_person_count = UserContext.objects.filter(currentArea=ar).count()
            area_dict['person_count'] = area_person_count
            
            area_list.append(area_dict)
            
        response['data'].update({'area_list': area_list})
    return view_response(request, response, 200)


def checkin_failed(request, data = None):
    response = {"success": False, "code": 400, "data": {"msg": "Checkin failed"}}
    
    if not data is None:
        response['data'].update(data)
    
    return view_response(request, response, 400)


def checkout_succeeded(request):
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    return view_response(request, response, 200)


def view_response(request, response, code):
    from django.http import HttpResponse
    from tastypie.serializers import Serializer
    from tastypie.utils.mime import determine_format
    
    serdes = Serializer(formats = ['json', 'xml'])
    mimetype = determine_format(request, serdes)
    response = serdes.serialize(response, mimetype)
    del serdes
    
    http_response = HttpResponse(response, mimetype=mimetype)
    http_response.status_code = code
    return http_response
    