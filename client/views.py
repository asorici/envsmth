from coresql.forms import CheckinForm
from coresql.models import Environment, Area, UserContext
from client.decorators import allow_anonymous_profile

@allow_anonymous_profile
def checkin(request):
    ## request.user will have been filled in by whatever backend
    ## and will correspond to the user field in UserProfile
    
    ## run check to see if all required fields are present
    check_form = CheckinForm(request.POST)
    #check_form = CheckinForm(request.REQUEST)
    if check_form.is_valid():
        ## access area id from check_f.cleaned_data
        area_id = check_form.cleaned_data['area']
        env_id = check_form.cleaned_data['env']
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
            return checkin_failed(data = {"msg": "No area ("+ str(area_id) +") or environment (" + str(env_id) + ") found." })
        
        ## get user and correct env
        user = request.user.get_profile()   ## should point to a UserProfile model
        
        ## if a valid area was found, use it's env field as corresponding environment
        if area:
            area_env = area.env
        
        ## update UserContext entry
        try:
            context = UserContext.objects.get(user=user)
            context.currentArea = area
            context.currentEnv = area_env
            context.save()
        except UserContext.DoesNotExist:
            ## an entry does not yet exist so assign one now
            context = UserContext(user=user, currentArea=area, currentEnv=area_env)
            context.save()
        
        return checkin_succeeded(request, area = area, env = env)
    
    else:
        return checkin_failed(request, data = check_form.errors)
            

def checkout(request):
    from django.contrib.auth import logout
    
    """
    TODO: Don't know if anonymous users should be deleted here
    but at least we log them out
    """
    ## checkout is done by default from the current area in the user context
    try:
        if not request.user.is_anonymous():
            user = request.user.get_profile()
            
            user.context.currentEnv = None
            user.context.currentArea = None
            user.context.save()
            
    except UserContext.DoesNotExist:
        ## graceful error handling, if no context exists don't freak out, just ignore
        pass
    
    ## let's also logout the user
    logout(request)
    
    return checkout_succeeded(request)
    
###############################################################################################################
###############################################################################################################

def checkin_succeeded(request, area = None, env = None):
    from django.http import HttpResponse
    from client.api import AreaResource, EnvironmentResource
    from tastypie.serializers import Serializer
    from tastypie.utils.mime import determine_format
    
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
    
    serdes = Serializer(formats = ['json', 'xml'])
    mimetype = determine_format(request, serdes)
    response = serdes.serialize(response, mimetype)
    del serdes
    
    http_response = HttpResponse(response, mimetype=mimetype)
    http_response.status_code = 200
    return http_response


def checkout_succeeded(request):
    from django.http import HttpResponse
    from tastypie.serializers import Serializer
    from tastypie.utils.mime import determine_format
    
    ## default is to just return an 200 OK http response
    response = {"success": True, "code": 200, "data" : {}}
    serdes = Serializer(formats = ['json', 'xml'])
    mimetype = determine_format(request, serdes)
    response = serdes.serialize(response, mimetype)
    del serdes
    
    http_response = HttpResponse(response, mimetype=mimetype)
    http_response.status_code = 200
    return http_response


def checkin_failed(request, data = None):
    from django.http import HttpResponse
    from tastypie.serializers import Serializer
    from tastypie.utils.mime import determine_format
    
    response = {"success": False, "code": 400, "data": {"msg": "Checkin failed."}}
    
    if not data is None:
        response['data'].update(data)
    
    serdes = Serializer(formats = ['json', 'xml'])
    mimetype = determine_format(request, serdes)
    response = serdes.serialize(response, mimetype)
    del serdes
    
    http_response = HttpResponse(response, mimetype=mimetype)
    http_response.status_code = 400
    return http_response

    