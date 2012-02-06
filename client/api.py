from coresql.models import Environment, Area, Annotation, Announcement, History, UserProfile
from coresql.forms import AnnotationForm
from tastypie.resources import ModelResource, ALL, ALL_WITH_RELATIONS
from tastypie.exceptions import ImmediateHttpResponse
from tastypie.validation import FormValidation
from tastypie import fields, http
from tastypie.authentication import Authentication
from client.authorization import AnnotationAuthorization
from datetime import datetime
#from tastypie.utils import now


class UserResource(ModelResource):
    class Meta:
        queryset = UserProfile.objects.all()
        resource_name = 'user'
        allowed_methods = []
        
    

class EnvironmentResource(ModelResource):
    features = fields.ListField()
    
    class Meta:
        queryset = Environment.objects.all()
        resource_name = 'environment'
        #fields = ['name', 'data', 'tags', 'parentID', 'category', 'latitude', 'longitude', 'timestamp']
        excludes = ['id', 'owner', 'width', 'height']
        detail_allowed_methods = ['get']
        list_allowed_methods = []
        authentication = Authentication()
        
    def dehydrate_tags(self, bundle):
        return bundle.obj.tags.to_serializable()
    
    def dehydrate_features(self, bundle):
        ## return a list of dictionary values from the features of this environment
        feature_list = []
        for feature in bundle.obj.features.all():
            feature_list.append({'category': feature.category, 'data': feature.data.to_serializable()})

        return feature_list
    
    

class AreaResource(ModelResource):
    env = fields.ForeignKey(EnvironmentResource, 'env')
    level = fields.IntegerField()
    layout_url = fields.CharField()
    
    features = fields.ListField()
    
    class Meta:
        queryset = Area.objects.all()
        resource_name = 'area'
        allowed_methods = ['get']
        #fields = ['name', 'data', 'category', 'tags']
        excludes = ['id', 'env', 'shape', 'layout']
        filtering = {
            'env': ['exact'],
            'level': ['exact']
        }
        authentication = Authentication()
        
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``env`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'env' in request.GET or 'q' in request.GET:
            return super(AreaResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    
    def dehydrate_level(self, bundle):
        ## get level data from the layout reference of the Area obj
        return bundle.obj.layout.level
            
    
    def dehydrate_layout_url(self, bundle):
        ## get layout-url data from the layout reference of the Area obj
        return bundle.obj.layout.mapURL
    
    
    def dehydrate_tags(self, bundle):
        return bundle.obj.tags.to_serializable()
    
    
    def dehydrate_features(self, bundle):
        ## return a list of dictionary values from the features of this environment
        feature_list = []
        for feature in bundle.obj.features.all():
            feature_list.append({'category': feature.category, 'data': feature.data.to_serializable()})

        return feature_list
        
    

class AnnouncementResource(ModelResource):
    env = fields.ForeignKey(EnvironmentResource, 'env')
    area = fields.ForeignKey(AreaResource, 'area', null = True)
    
    class Meta:
        queryset = Announcement.objects.all()
        resource_name = 'announcement'
        allowed_methods = ['get']
        fields = ['data', 'timestamp']
        excludes = ['id']
        filtering = {
            #'area': ['exact'],
            'env': ['exact'],
            'timestamp': ['gt', 'gte'],
        }
        authentication = Authentication()
    
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``env`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'env' in request.GET:
            return super(AnnouncementResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
        
    
    def get_obj_list(self, request):
        ## override the usual obj_list retrieval by filtering out only the yet to be given announcements 
        ## for the current environment (which we **know** must exist) 
        timestamp = datetime.now()
        env_id = request.GET['env']
        
        try:
            environ = Environment.objects.get(id=env_id)
            obj_list = super(AnnouncementResource, self).get_object_list(request)
            env_obj_list = obj_list.filter(env=environ)
                    
            id_list = []
            ## loop through each announcement and see if any of its
            ## triggers are greater than the current timestamp
            for obj in env_obj_list:
                triggers = obj.triggers.getList()
                        
                ## maybe make the following a little less hardcoded
                if obj.repeatEvery == "day":
                    for trig in triggers:
                        trig.replace(year=timestamp.year, month = timestamp.month, day = timestamp.day)
                        
                elif obj.repeatEvery == "week":
                    ## see which triggers are within "weeks" of the timestamp
                    for trig in triggers:
                        diff = timestamp.date() - trig.date()
                        if diff.days % 7 != 0:
                            triggers.remove(trig)
                            
                    ## and then update the day only for those
                    for trig in triggers:
                        trig.replace(year=timestamp.year, month = timestamp.month, day = timestamp.day)
                        
                ## and now we can do easy comparisons
                should_be_included = False
                for dt in obj.triggers.getList():
                    if dt >= timestamp:
                        should_be_included = True
                        break
                        
                if should_be_included:
                    id_list.append(obj.id)
                    
            
            return env_obj_list.filter(id__in = id_list)
                    
        except Exception:
                raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    

class AnnotationResource(ModelResource):
    env = fields.ForeignKey(EnvironmentResource, 'env', null = True)
    area = fields.ForeignKey(AreaResource, 'area')
    
    class Meta:
        queryset = Annotation.objects.all()
        resource_name = 'annotation'
        allowed_methods = ['get', 'post', 'put', 'delete']
        fields = ['data', 'timestamp']
        #excludes = ['id', 'area']
        filtering = {
            'area': ['exact'],
            'env': ['exact'],
            'timestamp': ['gt', 'gte'],
        }
        ordering = ['timestamp']
        authentication = Authentication()
        authorization = AnnotationAuthorization()
        validation = FormValidation(form_class = AnnotationForm)
        
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``area`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'area' in request.GET:
            return super(AnnotationResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
        



class HistoryResource(ModelResource):
    env = fields.ForeignKey(EnvironmentResource, 'env')
    area = fields.ForeignKey(AreaResource, 'area')
    user = fields.ForeignKey(UserResource, 'user')
    
    class Meta:
        resource_name = 'history'
        queryset = History.objects.all()
        excludes = ['user']
        allowed_methods = ['get']
        filtering = {
            'user': ['exact'],
        }
        ordering = ['-timestamp']
    
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``user`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'user' in request.GET:
            return super(AnnotationResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    

#############################################################################################################
#############################################################################################################

def get_timestamp_from_url(date_string):
    timestamp = None
    try:
        ## first try the format %Y-%m-%dT%H:%M:%S
        format = "%Y-%m-%dT%H:%M:%S"
        timestamp = datetime.strptime(date_string, format)
    except ValueError:
        pass
    
    try:
        ## then try the format %Y-%m-%d %H:%M:%S
        format = "%Y-%m-%d %H:%M:%S"
        timestamp = datetime.strptime(date_string, format)
    except ValueError:
        pass
    
    return timestamp