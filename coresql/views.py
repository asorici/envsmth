# Create your views here.

"""
THIS ARE THE CORE APPLICATION VIEW FUNCTIONS WHICH HANDLE THE RESTful SERVING
OF CLIENT SIDE REQUESTS.
THESE VIEW FUNCTIONS ARE THEREFORE MOSTLY LIMITED TO IMPLIED "GET" FUNCTIONALITY.

FEATURE-SPECIFIC CREATING, UPDATING AND DELETING FUNCTIONS WILL BE IMPLEMENTED IN
VIEWS OF CORRESPONDING SUB-APPLICATION FOLDERS.  
"""

#from django.contrib.auth import authenticate, login
from django.utils import simplejson
from coresql.models import User, Environment, Area, Annotation, Announcement, History, UserContext 
from django.http import HttpResponse

## ERROR_RESPONSE_LIST
CHECKIN_FAILED = {"success": False, "code": 500, "data": "Checkin failed. "}
CHECKIN_DENIED = {"success": False, "code": 405, "data": "Checkin denied. "}


def not_found_error(model, msg = None):
    response = {"success": False, "code": 404, "data": {"msg": "Requested " + model + " not found. "}}
    if not msg is None:
        response["data"]["msg"] += msg
        
    response_json = simplejson.dumps(response)
    http_response = HttpResponse(response_json, mimetype='application/json')
    http_response.status_code = 404
    return http_response

    
def bad_request_error(model, msg = None):
    response = {"success": False, "code": 400, "data": {"msg": "Bad " + model + " request. "}}
    if not msg is None:
        response["data"]["msg"] += msg
    
    response_json = simplejson.dumps(response)
    http_response = HttpResponse(response_json, mimetype='application/json')
    http_response.status_code = 400
    return http_response


##############################################################################################################


def authenticate_user(request):
    pass

    
def logout(request):
    pass

    
def check_in_area(request):
    pass 


def checkout(request):
    pass


def dispatch_environment_request(envID = None, request):
    from features.default import views as default_views
    from features.ordering import views as ordering_views
    """
    TODO - maybe consider dynamic import
    """
     
    if envID is None:
        if "category" in request.REQUEST:
            if request.REQUEST["category"] == "default":
                return default_views.handle_environment_request(envID = envID, request)
            elif request.REQUEST["category"] == "ordering":
                return ordering_views.handle_environment_request(envID = envID, request)
            else:
                return bad_request_error("Environment", msg = "Wrong environment category specification.")
        else:
            return bad_request_error("Environment", msg = "No category specified.")
            
    else:        
        try:
            env = Environment.objects.get(id=envID)
            if env.category == "default":
                return default_views.handle_environment_request(envID = envID, request)
            elif env.category == "ordering":
                return ordering_views.handle_environment_request(envID = envID, request)
            else:
                return bad_request_error("Environment", msg = "Wrong environment category specification.")
        except Exception, ex:
            return not_found_error("Environment")
        


def dispatch_area_request(areaID = None, request):
    from features.default import views as default_views
    from features.ordering import views as ordering_views
    
    if areaID is None:
        if "category" in request.REQUEST:
            if request.REQUEST["category"] == "default":
                return default_views.handle_area_request(areaID = areaID, request)
            elif request.REQUEST["category"] == "ordering":
                return ordering_views.handle_area_request(areaID = areaID, request)
            else:
                return bad_request_error("Area", msg = "Wrong area category specification.")
                
        else:
            return bad_request_error("Area", msg = "No category specified.")
        
    else:    
        try:    
            area = Area.objects.get(id=areaID)
            if area.category == "default":
                return default_views.handle_area_request(areaID = areaID, request)
            elif area.category == "ordering":
                return ordering_views.handle_area_request(areaID = areaID, request)
            else:
                return bad_request_error("Area", msg = "Wrong area category specification.")
        except Exception, ex:
            return not_found_error("Area")



def dispatch_announcement_request(id = None, request):
    from features.default import views as default_views
    from features.ordering import views as ordering_views
    
    if id is None:
        if "locationID" in request.REQUEST and "type" in request.REQUEST:
            try:
                category = None
                if request.REQUEST["type"] == "ENV":
                    category = Environment.objects.get(id=request.REQUEST["locationID"]).category
                elif request.REQUEST["type"] == "AREA":
                    category = Area.objects.get(id=request.REQUEST["locationID"]).category
                
                if category is None:
                    return bad_request_error("Announcement", msg = "No zone type (area/environment) identified.")
                else:
                    if category == "default":
                        return default_views.handle_announcement_request(id = id, request)
                    elif category == "ordering":
                        return ordering_views.handle_announcement_request(id = id, request)
                    else:
                        return bad_request_error("Announcement", msg = "Wrong category specification for associated environment/area.")
                    
            except Exception, ex:
                return bad_request_error("Announcement", msg = "No zone type (area/environment) identified.")
        else:
            return bad_request_error("Announcement", msg = "No location id or zone type (area/environment) specified.")
    else:
        if request.method != "GET":
            try:
                announcement = Announcement.objects.get(id=id)
                category = None
                
                if not announcement.env is None:
                    category = announcement.env.category
                elif not announcement.area is None:
                    category = announcement.area.category
                
                if category is None:
                    return not_found_error("Announcement")
                else:
                    if category == "default":
                        return default_views.handle_announcement_request(id = id, request)
                    elif category == "ordering":
                        return ordering_views.handle_announcement_request(id = id, request)
                    else:
                        return bad_request_error("Announcement", msg = "Wrong category specification for associated environment/area.")
                    
            except Exception, ex:
                return not_found_error("Announcement")
        
        else:
            ## the given id must be an environment ID
            try:
                env = Environment.objects.get(id=id)
                if env.category == "default":
                    return default_views.handle_announcement_request(id = id, request)
                elif env.category == "ordering":
                    return ordering_views.handle_announcement_request(id = id, request)
                else:
                    return bad_request_error("Announcement", msg = "Wrong category specification for associated environment") 
            except Exception, ex:
                return not_found_error("Announcement")




def dispatch_annotation_request(annID = None, request):
    from features.default import views as default_views
    from features.ordering import views as ordering_views
    
    if annID is None:
        if "locationID" in request.REQUEST and "type" in request.REQUEST:
            try:
                category = None
                if request.REQUEST["type"] == "ENV":
                    category = Environment.objects.get(id=request.REQUEST["locationID"]).category
                elif request.REQUEST["type"] == "AREA":
                    category = Area.objects.get(id=request.REQUEST["locationID"]).category
                
                if category is None:
                    return bad_request_error("Annotation", msg = "No zone type (area/environment) identified.")
                else:
                    if category == "default":
                        return default_views.handle_annotation_request(annID = annID, request)
                    elif category == "ordering":
                        return ordering_views.handle_annotation_request(annID = annID, request)
                    else:
                        return bad_request_error("Annotation", msg = "Wrong category specification for associated environment/area.")
                
            except Exception, ex:
                return bad_request_error("Annotation", msg = "No zone type (area/environment) identified.")
        else:
            return bad_request_error("Annotation", msg = "No location id or zone type (area/environment) specified.")
        
    else:    
        try:
            annotation = Annotation.objects.get(id=id)
            category = None
                
            if not annotation.env is None:
                category = annotation.env.category
            elif not annotation.area is None:
                category = annotation.area.category
                
            if category is None:
                return not_found_error("Annotation")
            else:
                if category == "default":
                    return default_views.handle_annotation_request(annID = annID, request)
                elif category == "ordering":
                    return ordering_views.handle_annotation_request(annID = annID, request)
                else:
                    return bad_request_error("Annotation", msg = "Wrong category specification for associated environment/area.")
                    
        except Exception, ex:
            return not_found_error("Annotation")




def handle_history_request(userID, request):
    pass




def handle_user_request(userID, request):
    pass