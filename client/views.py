from coresql.forms import CheckinForm, LoginForm, ClientRegistrationForm
from coresql.models import Environment, Area, UserContext
from client.decorators import allow_anonymous_profile, secure_required

@secure_required
def register(request):
    from django_facebook.connect import connect_user
    from django.contrib.auth import login
    
    #if request.method.upper() == "POST":
    if request.method.upper() in ["POST", "GET"]:
        new_user = None
        
        form = ClientRegistrationForm(data=request.REQUEST)
        #form = ClientRegistrationForm(data=request.POST)
        if form.is_valid():
            new_user = form.save()
            ## we have created new user, now let's log them in
            ## we already have the user instance so for authentication 
            ## just set the backend as django.contrib.auth.backends.ModelBackend
            new_user.backend = 'django.contrib.auth.backends.ModelBackend'
            login(request, new_user)
            
            fb_access_token = request.POST.get('fb_access_token')
            if fb_access_token:
                ## if we have a fb_access_token then we should also try to connect the data from facebook to
                ## the user's profile
                connect_user(request, fb_access_token)
                
            return register_succeeded(request, new_user)
    
        return register_failed(request, data = form.errors)
    
    return register_failed(request)


@secure_required
def login(request):
    from django.contrib.auth import login
    
    form = LoginForm(request.REQUEST)
    #form = LoginForm(request.POST)
    
    if form.is_valid():
        user = form.get_user()
        if not user is None:
            login(request, user)
            return login_succeeded(request, user)
    
    return login_failed(request, data = form.errors)


def logout(request):
    from django.contrib.auth import logout
    
    ## log the user out
    user = getattr(request, 'user', None)
    
    ## when logging out we also checkout from the current location
    try:
        if not user is None and not request.user.is_anonymous():
            user = request.user.get_profile()
            
            user.context.currentEnv = None
            user.context.currentArea = None
            user.context.save()
            
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
        ## access area id from check_f.cleaned_data
        area_id = check_form.cleaned_data['area']
        env_id = check_form.cleaned_data['environment']
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
            return checkin_failed(request, data = {"msg": "No area ("+ str(area_id) +") or environment (" + str(env_id) + ") found." })
        
        ## get user and correct env
        user_profile = request.user.get_profile()   ## should point to a UserProfile model
        
        ## if a valid area was found, use it's env field as corresponding environment
        if area:
            area_env = area.env
        
        ## update UserContext entry
        try:
            context = UserContext.objects.get(user=user_profile)
            context.currentArea = area
            context.currentEnvironment = area_env
            context.save()
        except UserContext.DoesNotExist:
            ## an entry does not yet exist so assign one now
            context = UserContext(user=user_profile, currentArea=area, currentEnvironment=area_env)
            context.save()
        
        return checkin_succeeded(request, area = area, env = env)
    
    else:
        return checkin_failed(request, data = check_form.errors)
            

def checkout(request):
    """
    TODO: Don't know if anonymous users should be deleted here
    but at least we log them out
    """
    ## checkout is done by default from the current area or current env in the user context
    try:
        if not request.user.is_anonymous():
            user = request.user.get_profile()
            
            user.context.currentEnv = None
            user.context.currentArea = None
            user.context.save()
            
    except UserContext.DoesNotExist:
        ## graceful error handling, if no context exists don't freak out, just ignore
        pass
    
    return checkout_succeeded(request)

    
###############################################################################################################
###############################################################################################################

def register_succeeded(request, user):
    from client.api import UserResource
    response = {"success": True, "code": 200, "data" : {}}
    
    if not user.get_profile().is_anonymous:
        user_res_uri = UserResource().get_resource_uri(user.get_profile())
        response['data'].update({'resource_uri' : user_res_uri})
        
    return view_response(request, response, 200)


def register_failed(request, data = None):
    response = {"success": False, "code": 400, "data" : {}}
    if not data is None:
        response['data'].update(data)
    
    return view_response(request, response, 400)


def login_succeeded(request, user):
    from client.api import UserResource
    
    response = {"success": True, "code": 200, "data" : {}}
    
    if not user.get_profile().is_anonymous:
        user_res_uri = UserResource().get_resource_uri(user.get_profile())
        response['data'].update({'resource_uri' : user_res_uri})
        
    ## TODO maybe include data about user
    return view_response(request, response, 200)


def logout_succeeded(request):
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    return view_response(request, response, 200)


def login_failed(request, data = None):
    response = {"success": False, "code": 401, "data": {"msg": "Login failed."}}
    
    if not data is None:
        response['data'].update(data)
    
    return view_response(request, response, 401)


def checkin_succeeded(request, area = None, env = None):
    from client.api import AreaResource, EnvironmentResource
    
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    
    if area:
        ## return data about the area resource
        ar = AreaResource()
        ar_item = ar.obj_get(pk=area.id)
        ar_bundle = ar.build_bundle(obj = ar_item, request=request)
        
        area_data = {"location_type" : "area", "location_data" : ar.full_dehydrate(ar_bundle).data}
        response['data'].update(area_data)
        
    elif env:
        ## return data about the area resource
        envr = EnvironmentResource()
        envr_item = envr.obj_get(pk=env.id)
        envr_bundle = envr.build_bundle(obj = envr_item, request=request)
        
        env_data = {"location_type" : "environment", "location_data" : envr.full_dehydrate(envr_bundle).data}
        response['data'].update(env_data)
    
    return view_response(request, response, 200)


def checkin_failed(request, data = None):
    response = {"success": False, "code": 400, "data": {"msg": "Checkin failed."}}
    
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
    