from django.utils import simplejson
from django.http import HttpResponse
from coresql.models import User, Environment, Area, Annotation, Announcement
from coresql.forms import EnvironmentForm, UpdateEnvironmentForm, AreaForm, UpdateAreaForm, \
                          AnnotationForm, UpdateAnnotationForm, AnnouncementForm, UpdateAnnouncementForm  
from coresql.views import OP_READ, OP_CREATE,  OP_UPDATE, OP_DELETE
from coresql.views import bad_request_error, not_logged_error, operation_successful

def handle_environment_request(id = None, request):
    if request.method != "GET":
        if not request.user.is_authenticated():
            return not_logged_error("Environment")
    
    if request.method == "POST":
        """
        HANDLE POST REQUEST - semantics of creation
        """
        form = EnvironmentForm(request.POST)
        try:
            if form.is_valid():
                instance = form.save(commit=False)
                data = {'url': instance.get_absolute_url, 'id': str(instance.id)}
                instance.save()
                
                return operation_successful("Environment", OP_CREATE, data)
            else:
                err_text = form._get_errors().as_text()
                return bad_request_error("Environment", OP_CREATE + " operation failed. ", {"model_errors": err_text})
            
        except Exception, ex:
            err_text = form._get_errors().as_text()
            return bad_request_error("Environment", OP_CREATE + " operation failed. ", {"model_errors": err_text})
    
    elif request.method == "PUT":
        """
        HANDLE PUT REQUEST - semantics of an update
        """
        if id is None:
            return bad_request_error("Environment", msg = "Invalid update. No environment id specified.")
        else:
            instance = Environment.objects.get(id=id)
            
            ## check if user has rights
            user = request.user.get_profile()
            if user == instance.owner:
                try:
                    form = UpdateEnvironmentForm(request.PUT, instance=instance)
                    if form.is_valid():
                        instance = form.save(commit = False)
                        data = {'url': instance.get_absolute_url, 'id': str(instance.id)}
                        instance.save()
                        
                        return operation_successful("Environment", OP_UPDATE, data)
                    else:
                        err_text = form._get_errors().as_text()
                        return bad_request_error("Environment", OP_UPDATE + " operation failed. ", {"model_errors": err_text})
                except Exception, ex:
                    err_text = form._get_errors().as_text()
                    return bad_request_error("Environment", OP_UPDATE + " operation failed. ", {"model_errors": err_text})
            else:
                return bad_request_error("Environment", msg = "Invalid " + OP_UPDATE + ". You are not the owner of this environment. This incident will be reported")

    
    elif request.method == "DELETE":
        """
        HANDLE DELETE REQUEST - semantics of delete
        """
        if id is None:
            return bad_request_error("Environment", msg = "Invalid " + OP_DELETE + ". No environment id specified.")
        else:
            instance = Environment.objects.get(id=id)
            if user == instance.owner:
                instance.delete()
                return operation_successful("Environment", OP_DELETE)
            else:
                return bad_request_error("Environment", msg = "Invalid " + OP_DELETE + ". You are not the owner of this environment. This incident will be reported")

    
    elif request.method == "GET":
        """
        HANDLE GET REQUEST - semantics of read
        """
        pass



def handle_area_request(areaID = None, request):
    pass



def handle_announcement_request(id = None, request):
    if request.method != "GET":
        if not request.user.is_authenticated():
            return not_logged_error("Announcement")
        
    if request.method == "POST":
        form = AnnouncementForm(request.POST)
        try:
            if form.is_valid():
                instance = form.save(commit=False)
                data = {'url': instance.get_absolute_url, 'id': str(instance.id)}
                instance.save()
                
                return operation_successful("Announcement", OP_CREATE, data)
            else:
                err_text = form._get_errors().as_text()
                return bad_request_error("Announcement", OP_CREATE + " operation failed. ", {"model_errors": err_text})
            
        except Exception, ex:
            err_text = form._get_errors().as_text()
            return bad_request_error("Announcement", OP_CREATE + " operation failed. ", {"model_errors": err_text})

    elif request.method == "PUT":
        """
        HANDLE PUT REQUEST - semantics of an update
        """
        if id is None:
            return bad_request_error("Announcement", msg = "Invalid update. No environment id specified.")
        else:
            instance = Announcement.objects.get(id=id)
            
            ## check if user has rights
            user = request.user.get_profile()
            owner = None
            
            if not instance.env is None:
                owner = instance.env.owner
            elif not instance.area is None:
                owner = instance.area.owner 
            
            if user == owner:
                try:
                    form = UpdateAnnouncementForm(request.PUT, instance=instance)
                    if form.is_valid():
                        instance = form.save(commit = False)
                        data = {'url': instance.get_absolute_url, 'id': str(instance.id)}
                        instance.save()
                        
                        return operation_successful("Announcement", OP_UPDATE, data)
                    else:
                        err_text = form._get_errors().as_text()
                        return bad_request_error("Announcement", OP_UPDATE + " operation failed. ", {"model_errors": err_text})
                except Exception, ex:
                    err_text = form._get_errors().as_text()
                    return bad_request_error("Announcement", OP_UPDATE + " operation failed. ", {"model_errors": err_text})
            else:
                return bad_request_error("Announcement", msg = "Invalid " + OP_UPDATE + ". You are not the owner of this environment. This incident will be reported")

    elif request.method == "DELETE":
        """
        HANDLE DELETE REQUEST - semantics of delete
        """
        if id is None:
            return bad_request_error("Announcement", msg = "Invalid " + OP_DELETE + ". No environment id specified.")
        else:
            instance = Announcement.objects.get(id=id)
            if user == instance.owner:
                instance.delete()
                return operation_successful("Announcement", OP_DELETE)
            else:
                return bad_request_error("Announcement", msg = "Invalid " + OP_DELETE + ". You are not the owner of this environment. This incident will be reported")

    
    elif request.method == "GET":
        """
        HANDLE GET REQUEST - semantics of read
        """
        pass



def handle_annotation_request(annID = None, request):    
    
    if request.method == "POST":
        form = AnnotationForm(request.POST)
        try:
            if form.is_valid():
                instance = form.save(commit=False)
                data = {'url': instance.get_absolute_url, 'id': str(instance.id)}
                instance.save()
                
                return operation_successful("Annotation", OP_CREATE, data)
            else:
                err_text = form._get_errors().as_text()
                return bad_request_error("Annotation", OP_CREATE + " operation failed. ", {"model_errors": err_text})
            
        except Exception, ex:
            err_text = form._get_errors().as_text()
            return bad_request_error("Annotation", OP_CREATE + " operation failed. ", {"model_errors": err_text})

    elif request.method == "PUT":
        """
        HANDLE PUT REQUEST - semantics of an update
        """
        if id is None:
            return bad_request_error("Annotation", msg = "Invalid update. No environment id specified.")
        else:
            instance = Annotation.objects.get(id=id)
            
            ## check if user has rights
            has_rights = True
            if request.user.is_anonymous:
                has_rights = False
            else:
                user = request.user.get_profile()
                owner = None
                if not instance.env is None:
                    owner = instance.env.owner
                elif not instance.area is None:
                    owner = instance.area.owner
                    
                if owner is None:
                    has_rights = False
            
            if has_rights:
                try:
                    form = UpdateAnnotationForm(request.PUT, instance=instance)
                    if form.is_valid():
                        instance = form.save(commit = False)
                        data = {'url': instance.get_absolute_url, 'id': str(instance.id)}
                        instance.save()
                        
                        return operation_successful("Annotation", OP_UPDATE, data)
                    else:
                        err_text = form._get_errors().as_text()
                        return bad_request_error("Annotation", OP_UPDATE + " operation failed. ", {"model_errors": err_text})
                except Exception, ex:
                    err_text = form._get_errors().as_text()
                    return bad_request_error("Annotation", OP_UPDATE + " operation failed. ", {"model_errors": err_text})
            else:
                return bad_request_error("Annotation", msg = "Invalid " + OP_UPDATE + ". You are not allowed to perform this operation. This incident will be reported")

    elif request.method == "DELETE":
        """
        HANDLE DELETE REQUEST - semantics of delete
        """
        if id is None:
            return bad_request_error("Annotation", msg = "Invalid " + OP_DELETE + ". No environment id specified.")
        else:
            instance = Annotation.objects.get(id=id)
            ## check if user has rights
            has_rights = True
            if request.user.is_anonymous():
                has_rights = False
            else:
                user = request.user.get_profile()
                owner = None
                if not instance.env is None:
                    owner = instance.env.owner
                elif not instance.area is None:
                    owner = instance.area.owner
                    
                if owner is None:
                    has_rights = False
                    
            if has_rights:
                instance.delete()
                return operation_successful("Annotation", OP_DELETE)
            else:
                return bad_request_error("Annotation", msg = "Invalid " + OP_DELETE + ". You are not allowed to perform this operation. This incident will be reported")

    
    elif request.method == "GET":
        """
        HANDLE GET REQUEST - semantics of read
        """
        pass



##############################################################################################################
##############################################################################################################


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