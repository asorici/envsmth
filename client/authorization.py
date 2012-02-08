from tastypie.authorization import Authorization
from django.db.models import Q
from coresql.models import UserContext, Environment, Area

class AnnotationAuthorization(Authorization):
    def is_authorized(self, request, object=None):
        ## check here for annotation requests that the requesting user is actually checked in   
        if hasattr(request, 'user') and not request.user.is_anonymous():
            user = request.user.get_profile()       ## will be an instance of UserProfile => available context
            env_pk = None
            area_pk = None
            
            if 'env' in request.REQUEST:
                try:
                    env_pk = int(request.REQUEST['env'])
                except:
                    pass
                    
            if 'area' in request.REQUEST:
                try:
                    area_pk = int(request.REQUEST['area'])
                except:
                    pass
            
            try:
                currentEnv = user.context.currentEnv
                currentArea = user.context.currentArea
                
                ## if the user wants to make/get an annotation on/of an area and he is checked in at that area
                if area_pk and area_pk == currentArea:
                    return True
                ## alternatively he wants to make/get an annotation for/of the environment in which he is checked in 
                elif env_pk and env_pk == currentEnv:
                    return True
                
            except UserContext.DoesNotExist:
                ## it means the user is not checked in anywhere
                return False
            
        return False
            
    
    def apply_limits(self, request, object_list):
        """
        apply restrictions to PUT and DELETE in the following manner:
        only the owner of the environment and authenticated senders of the comment
        can modify or delete a comment 
        """
        if request and (request.method.upper() in ["PUT", "DELETE"]):
            if hasattr(request, 'user') and not request.user.is_anonymous():
                user = request.user.get_profile()
                
                q1 = Q(user = user)
                q2 = Q(area__env__owner = user)
                q3 = Q(env__owner = user)
                
                return object_list.filter(q1 | q2 | q3)
            else:
                return object_list.none()
        
        return object_list