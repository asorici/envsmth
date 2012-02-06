from tastypie.authorization import Authorization
from django.db.models import Q

class AnnotationAuthorization(Authorization):
    def is_authorized(self, request, object=None):
        ## for now, since we are using the session middleware and every user has a session just return True 
        return True
        
    
    def apply_limits(self, request, object_list):
        """
        apply restrictions to PUT and DELETE in the following manner:
        only the owner of the environment and authenticated senders of the comment
        can modify or delete a comment 
        """
        if request and (request.method.upper() in ["PUT", "DELETE"]):
            if hasattr(request, 'user') and not request.user.is_anonymous():
                q1 = Q(user = request.user.get_profile())
                q2 = Q(area__env__owner = request.user.get_profile())
                q3 = Q(env__owner = request.user.get_profile())
                
                return object_list.filter(q1 | q2 | q3)
            else:
                return object_list.none()
        
        return object_list