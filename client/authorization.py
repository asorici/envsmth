from tastypie.authorization import Authorization
from tastypie.serializers import Serializer
from django.db.models import Q

class AnnotationAuthorization(Authorization):
    def is_authorized(self, request, object=None):
        from client.api import EnvironmentResource, AreaResource, AnnotationResource
        from coresql.models import UserContext, Environment, Area
        
        if hasattr(request, 'user') and not request.user.is_anonymous():
            ## check here for annotation requests that the requesting user is actually checked in   
            env_obj = None
            area_obj = None
                
            if request.method.upper() == "POST":
                serdes = Serializer()
                deserialized = None
                try:
                    deserialized = serdes.deserialize(request.raw_post_data, format=request.META.get('CONTENT_TYPE', 'application/json'))
                except Exception:
                    return False
                    
                if deserialized is None:
                    return False
                    
                if 'environment' in deserialized:
                    try:
                        #env_pk = int(deserialized['env'])
                        env_obj = EnvironmentResource().get_via_uri(deserialized['environment']) 
                    except:
                        env_obj = None
                            
                if 'area' in deserialized:
                    try:
                        #area_pk = int(deserialized['area'])
                        area_obj = AreaResource().get_via_uri(deserialized['area'])
                    except:
                        area_obj = None
            
            elif request.method.upper() == "GET":
                if 'environment' in request.GET:
                    try:
                        env_obj = Environment.objects.get(pk=request.GET['environment'])
                    except:
                        env_obj = None
                        
                if 'area' in request.GET:
                    try:
                        area_obj = Area.objects.get(pk=request.GET['area'])
                    except:
                        area_obj = None
            
            elif request.method.upper() in ["DELETE", "PUT"]:
                ann_res_uri = request.path
                try:
                    ann_obj = AnnotationResource().get_via_uri(ann_res_uri)
                    env_obj = ann_obj.environment
                    area_obj = ann_obj.area
                except:
                    env_obj = None
                    area_obj = None
                
            
            user = request.user.get_profile()       ## will be an instance of UserProfile => available context
            try:
                currentEnvironment = user.context.currentEnvironment
                currentArea = user.context.currentArea
                    
                ## if the user wants to make/get an annotation on/of an area and he is checked in at that area
                #if area_pk and area_pk == currentArea.pk:
                if not area_obj is None and area_obj == currentArea:
                    return True
                ## alternatively he wants to make/get an annotation for/of the environment in which he is checked in 
                #elif env_pk and env_pk == currentEnv.pk:
                elif not env_obj is None and env_obj == currentEnvironment:  
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
                q2 = Q(area__environment__owner = user)
                q3 = Q(environment__owner = user)
                
                object_list = object_list.filter(q1 | q2 | q3)
                #return object_list.filter(q1 | q2 | q3)
                
                return object_list
            else:
                return object_list.none()
        
        return object_list