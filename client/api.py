from coresql.models import Environment, Area, Annotation, Announcement, History, UserProfile
from coresql.forms import AnnotationForm
from tastypie.resources import ModelResource, ALL, ALL_WITH_RELATIONS
from tastypie.exceptions import ImmediateHttpResponse, NotFound
#from tastypie.validation import FormValidation
from tastypie import fields, http
from tastypie.authentication import Authentication
from tastypie.api import Api
from client.authorization import AnnotationAuthorization
from client.validation import AnnotationValidation
from datetime import datetime
from django.core.exceptions import MultipleObjectsReturned
#from tastypie.utils import now


class UserResource(ModelResource):
    first_name = fields.CharField(readonly = True)
    last_name = fields.CharField(readonly = True)
    
    class Meta:
        queryset = UserProfile.objects.all()
        resource_name = 'user'
        allowed_methods = ["get"]
        excludes = ["id", "fbID", "timestamp", "is_anonymous"]
        
    def dehydrate_first_name(self, bundle):
        return bundle.obj.user.first_name
        
    def dehydrate_last_name(self, bundle):
        return bundle.obj.user.last_name
    
    def dehydrate(self, bundle):
        ## if the user is requesting his own data then return his email too as it
        ## is an identifying element
        if hasattr(bundle.request, "user") and not bundle.request.user.is_anonymous():
            user_profile = bundle.request.user.get_profile()
            if user_profile.pk == bundle.obj.pk:
                bundle.data['username'] = bundle.obj.user.username
                bundle.data['email'] = bundle.obj.user.email 
    
        return bundle
    

class EnvironmentResource(ModelResource):
    features = fields.ListField()
    parent = fields.ForeignKey('self', 'parent', null = True)
    owner = fields.ForeignKey(UserResource, 'owner', full = True)
    
    class Meta:
        queryset = Environment.objects.all()
        resource_name = 'environment'
        #api_name = 'v1/resources'
        #fields = ['name', 'data', 'tags', 'parentID', 'category', 'latitude', 'longitude', 'timestamp']
        excludes = ['id', 'width', 'height']
        detail_allowed_methods = ['get']
        list_allowed_methods = []
        authentication = Authentication()
        default_format = "application/json"
        
    def dehydrate_tags(self, bundle):
        return bundle.obj.tags.to_serializable()
    
    
    def dehydrate_features(self, bundle):
        ## return a list of dictionary values from the features of this environment
        feature_list = []
        for feature in bundle.obj.features.all():
            feature_list.append({'category': feature.category, 'data': feature.data.to_serializable()})

        return feature_list
    
    
    def dehydrate(self, bundle):
        """
        append layout url if a level filter exists in the request 
        """
        if "level" in bundle.request.GET:
            level = int(bundle.request.GET["level"])
            bundle.data["layout_url"] = bundle.obj.layouts.get(level=level).mapURL
        
        return bundle
    

class AreaResource(ModelResource):
    parent = fields.ForeignKey(EnvironmentResource, 'env')
    
    features = fields.ListField()
    
    class Meta:
        queryset = Area.objects.all()
        resource_name = 'area'
        allowed_methods = ['get']
        excludes = ['id', 'shape', 'layout']
        filtering = {
            'parent': ['exact'],
            ##'level': ['exact']
            ## TODO - fix filtering by level as it has to be done manually !!!
        }
        authentication = Authentication()
        
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``env`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'parent' in request.GET or 'q' in request.GET:
            return super(AreaResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    
    def build_filters(self, filters = None):
        """
        enable filtering by level (which does not have its own field)
        """
        if filters is None:
            filters = {}
        
        orm_filters = super(AreaResource, self).build_filters(filters)
        
        if "level" in filters:
            orm_filters["layout__level"] = int(filters["level"])
        
        return orm_filters
    
    
    def dehydrate_tags(self, bundle):
        return bundle.obj.tags.to_serializable()
    
    
    def dehydrate_features(self, bundle):
        ## return a list of dictionary values from the features of this environment
        feature_list = []
        for feature in bundle.obj.features.all():
            feature_list.append({'category': feature.category, 'data': feature.data.to_serializable()})

        return feature_list
    
    
    def dehydrate(self, bundle):
        """
        append level data from the layout reference of the Area obj
        """
        bundle.data['level'] = bundle.obj.layout.level
        return bundle
    
        
    
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
            'area': ['exact'],
            'env': ['exact'],
            'timestamp': ['gt', 'gte'],
        }
        authentication = Authentication()
    
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``env`` or ``area`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'env' in request.GET or 'area' in request.GET:
            return super(AnnouncementResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
        
    
    def get_obj_list(self, request):
        ## override the usual obj_list retrieval by filtering out only the yet to be given announcements 
        ## for the current environment (which we **know** must exist) 
        timestamp = datetime.now()
        
        ## get default object list
        announcement_obj_list = super(AnnouncementResource, self).get_object_list(request)
        
        if 'env' in request.GET:
            try:
                env_id = request.GET['env']
                environ = Environment.objects.get(id=env_id)
                announcement_obj_list = announcement_obj_list.filter(env=environ)
            except Exception:
                pass
            
        if 'area' in request.GET:
            try:
                area_id = request.GET['area']
                area = Area.objects.get(id=area_id)
                announcement_obj_list = announcement_obj_list.filter(area=area)
            except Exception:
                pass
        
        try:
            id_list = []
            ## loop through each announcement and see if any of its
            ## triggers are greater than the current timestamp
            for obj in announcement_obj_list:
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
                    
            return announcement_obj_list.filter(id__in = id_list)
                    
        except Exception:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    

class AnnotationResource(ModelResource):
    env = fields.ForeignKey(EnvironmentResource, 'env', null = True)
    area = fields.ForeignKey(AreaResource, 'area', null = True)
    
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
        #validation = FormValidation(form_class = AnnotationForm)
        validation = AnnotationValidation()
        
        
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``area`` or ``env`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'area' in request.GET or 'env' in request.GET:
            return super(AnnotationResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
        
    
    def obj_create(self, bundle, request=None, **kwargs):
        ## because of the AnnotationAuthorization class, request.user will have a profile
        user_profile = request.user.get_profile()
        return super(AnnotationResource, self).obj_create(bundle, request, user=user_profile)
        
    """
    def obj_update(self, bundle, request=None, **kwargs):
        ## because of the AnnotationAuthorization class, request.user will have a profile
        user_profile = request.user.get_profile()
        print "obj update UserProfile: " + str(user_profile)
        kwargs['user'] = user_profile
        bundle = super(AnnotationResource, self).obj_update(bundle, request, **kwargs)
        print bundle.data
        return bundle
    """
    
    def obj_get(self, request=None, **kwargs):
        """
        A ORM-specific implementation of ``obj_get``.

        Takes optional ``kwargs``, which are used to narrow the query to find
        the instance.
        """
        try:
            print kwargs
            base_object_list = self.get_object_list(request).filter(**kwargs)
            
            print base_object_list
            object_list = self.apply_authorization_limits(request, base_object_list)
            stringified_kwargs = ', '.join(["%s=%s" % (k, v) for k, v in kwargs.items()])

            if len(object_list) <= 0:
                raise self._meta.object_class.DoesNotExist("Couldn't find an instance of '%s' which matched '%s'." % (self._meta.object_class.__name__, stringified_kwargs))
            elif len(object_list) > 1:
                raise MultipleObjectsReturned("More than '%s' matched '%s'." % (self._meta.object_class.__name__, stringified_kwargs))

            return object_list[0]
        except ValueError:
            raise NotFound("Invalid resource lookup data provided (mismatched type).")
    
    
        

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

class ClientApi(Api):
    
    def __init__(self, *args, **kwargs):
        super(ClientApi, self).__init__(*args, **kwargs)

    
    @property
    def urls(self):
        """
        Provides URLconf details for the ``Api`` and all registered
        ``Resources`` beneath it.
        """
        
        from django.conf.urls.defaults import url, include, patterns
        from tastypie.utils import trailing_slash
        from client.views import checkin, checkout
        
        pattern_list = [
            url(r"^(?P<api_name>%s)%s$" % (self.api_name, trailing_slash()), self.wrap_view('top_level'), name="api_%s_top_level" % self.api_name),
        ]

        for name in sorted(self._registry.keys()):
            self._registry[name].api_name = self.api_name
            pattern_list.append((r"^(?P<api_name>%s)/resources/" % self.api_name, include(self._registry[name].urls)))

        ## then add the actions
        pattern_list.extend([
            url(r"^%s/actions/checkin/$" % self.api_name, checkin, name="checkin"),
            url(r"^%s/actions/checkout/$" % self.api_name, checkout, name="checkout")
        ])

        urlpatterns = self.override_urls() + patterns('',
            *pattern_list
        )
        return urlpatterns


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