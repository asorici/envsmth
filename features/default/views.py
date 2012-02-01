from django.utils import simplejson
from django.http import HttpResponse
from coresql.models import User, Environment, Area, Annotation, Announcement, History, UserContext
from coresql.forms import EnvironmentForm,  


OP_READ = "Read"
OP_CREATE = "Create"
OP_UPDATE = "Update"
OP_DELETE = "Delete"


def handle_environment_request(envID = None, request):
    from coresql.views import bad_request_error
    
    if request.method != "GET":
        if not request.user.is_authenticated():
            return not_logged_error("Environment")
    
    """
    HANDLE POST REQUEST
    """
    if request.method == "POST":
        ## a new environment must me created
        
        try:
            env.save()
            return operation_successful("Environment", OP_CREATE, )
        except Exception, ex:
            return bad_request_error("Environment", msg = "Save failed. Invalid data supplied.")
    
    """
    HANDLE PUT REQUEST
    """ 
    elif request.method == "PUT":
        ## semantics of an update
        ## first call 
        if envID is None:
            return bad_request_error("Environment", msg = "Invalid update. No environment id specified.")
        else:
            env = Environment.objects.get(id=envID)
            
            ## check if user is owner of environment
            user = request.user.get_profile()
            if user == env.owner:
                if "category" in request.PUT:
                    env.category = request.PUT["category"]
                
                if "name" in request.PUT:
                    env.name = request.PUT["name"]
                
                if "description" in request.PUT:
                    env.data = request.PUT["description"]
                
                if "parentID" in request.PUT:
                    env.parentID = int(request.PUT["parentID"])
                    
                if "geolocation" in request.PUT:
                    try:
                        ll = request.PUT["geolocation"].split(":")
                        longitude = float(ll[0])
                        latitude = float(ll[1])
                            
                        env.longitude = longitude
                        env.latitude = latitude
                    except Exception, ex:
                        return bad_request_error("Environment", msg = "Invalid update. Geolocation data not valid.")
                    
                """
                TODO : treat image upload and URL creation 
                
                if "layout_level" in request.PUT and "layout_image" in request.PUT:
                    try:
                        pass
                    except Exception, ex:
                        return bad_request_error("Environment", msg = "Invalid update. Level does not exist.")
                """
                
            else:
                return bad_request_error("Environment", msg = "Invalid update. You are not the owner of this environment. This incident will be reported")

            
    """
    HANDLE DELETE REQUEST
    """    
    elif request.method == "DELETE":
        ## semantics of deletion
        pass

    
    """
    HANDLE GET REQUEST
    """
    elif request.method == "GET":
        ## semantics of retrieval
        pass


def handle_area_request(areaID = None, request):
    pass


def handle_announcement_request(id = None, request):
    pass


def handle_annotation_request(annID = None, request):
    pass



##############################################################################################################
##############################################################################################################

def operation_successful(model, op, data = None):
    response = {"success": True, "data": {"msg": op + "operation successful for " + model + ". "}}
    if op == OP_CREATE:
        response["code"] = 201
    else:
        response["code"] = 200
    
    if not data is None:
        response["data"].update(data)
        
    response_json = simplejson.dumps(response)
    http_response = HttpResponse(response_json, mimetype='application/json')
    http_response.status_code = response["code"]
    return http_response


def not_logged_error(model):
    response = {"success": False, "code": 401, "data": {"msg": "Unauthenticated modification request for " + model + ". "}}
    response_json = simplejson.dumps(response)
    http_response = HttpResponse(response_json, mimetype='application/json')
    http_response.status_code = 401
    return http_response
    


def load_put_and_files(request):
    """
    Populates request.PUT and request.FILES from
    request.raw_post_data. PUT and POST requests differ 
    only in REQUEST_METHOD, not in the way data is encoded. 
    Therefore we can use Django's POST data retrieval method 
    for PUT.
    """
    if request.method == 'PUT':
        request.method = 'POST'
        request._load_post_and_files()
        request.method = 'PUT'
        request.PUT = request.POST
        del request._post