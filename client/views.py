from django.db import models
from coresql.forms import CheckinForm
from coresql.models import Environment, Area, UserContext
from client.decorators import allow_anonymous_profile

@allow_anonymous_profile
def checkin(request):
    ## request.user will have been filled in by whatever backend
    ## and will correspond to the user field in UserProfile
    
    ## run check to see if all required fields are present
    check_form = CheckinForm(request.POST)
    if check_form.is_valid():
        ## access area id from check_f.cleaned_data
        area_id = check_form.cleaned_data['area']
        env_id = check_form.cleaned_data['env']
        area = None 
        env = None
        
        try:
            area = Area.objects.get(id=area_id)
        except Exception:
            pass
        
        try:
            env = Environment.objects.get(id=env_id)
        except Exception:
            pass
        
        ## is no valid area and environment data has been found => fail the checkin 
        if area is None and env is None:
            return checkin_failed(msg = "No area ("+ str(area_id) +") or environment (" + str(env_id) + ") found.")
        
        ## get user and correct env
        user = request.user.get_profile()   ## should point to a UserProfile model
        
        ## if a valid area was found, use it's env field as corresponding environment
        if area:
            env = area.env
        
        ## update UserContext entry
        try:
            context = UserContext.objects.get(user=user)
            context.currentEnv = env
            context.currentArea = area
            context.save()
        except UserContext.DoesNotExist:
            ## an entry does not yet exist so assign one now
            context = UserContext(user=user, currentAarea=area, currentEnv=env)
            context.save()
        
        return checkin_succeeded(request, area = area, env = env)
            

def checkout(request):
    from django.contrib.auth import logout
    
    """
    TODO: Don't know if anonymous users should be deleted here
    but at least we log them out
    """
    ## checkout is done by default from the current area in the user context
    user = request.user.get_profile()
    try:
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
    
    ## default is to just return an 200 OK http response
    return HttpResponse(status = 200)


def checkout_succeeded(request):
    from django.http import HttpResponse
    
    ## default is to just return an 200 OK http response
    return HttpResponse(status = 200)


def checkin_failed(area_id = None):
    from django.utils import simplejson
    from django.http import HttpResponse
    
    response = {"success": False, "code": 400, "data": {"msg": "Area checkin failed."}}
    
    response_json = simplejson.dumps(response)
    http_response = HttpResponse(response_json, mimetype='application/json')
    http_response.status_code = 400
    return http_response