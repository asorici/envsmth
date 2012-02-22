from coresql.models import Environment, Area, Annotation, Announcement, History, UserProfile
#from coresql.forms import AnnotationForm
from tastypie.resources import ModelResource
from tastypie.exceptions import ImmediateHttpResponse, NotFound
#from tastypie.validation import FormValidation
from tastypie import fields, http
from tastypie.authentication import Authentication
from tastypie.api import Api
from client.authorization import AnnotationAuthorization, UserAuthorization
from client.validation import AnnotationValidation
from datetime import datetime
from django.core.exceptions import MultipleObjectsReturned


class UserResource(ModelResource):
    first_name = fields.CharField(readonly = True)
    last_name = fields.CharField(readonly = True)
    
    class Meta:
        queryset = UserProfile.objects.all()
        resource_name = 'user'
        detail_allowed_methods = ["get", "put"]
        #fields = ['first_name']
        excludes = ["id", "facebook_id", "timestamp", "is_anonymous", "about_me", "facebook_id", "access_token",
                    "facebook_name", "facebook_profile_url", "website_url", "blog_url", "image", "date_of_birth",
                    "raw_data"]
        authentication = Authentication()
        authorization = UserAuthorization()
        
        
    def dehydrate_first_name(self, bundle):
        return bundle.obj.user.first_name
        
    def dehydrate_last_name(self, bundle):
        return bundle.obj.user.last_name
    
    def dehydrate(self, bundle):
        ## if the user is requesting his own data then return his email too as it
        ## is an identifying element
        
        ## remove c2dm data from bundle
        if 'c2dm_id' in bundle.data:
            del bundle.data['c2dm_id']
            
        if hasattr(bundle.request, "user") and not bundle.request.user.is_anonymous():
            user_profile = bundle.request.user.get_profile()
            if user_profile.pk == bundle.obj.pk:
                bundle.data['email'] = bundle.obj.user.email 
    
        return bundle

    
    
    def obj_update(self, bundle, request=None, **kwargs):
        """
        Could be an intentional bug that the default obj_update treats DoesNotExist and MultipleObjectReturned
        as acceptable exceptions which get transformed into a CREATE operation.
        We don't want such a behavior. So we catch does exceptions and throw an BadRequest message
        """ 
        from tastypie.serializers import Serializer
           
        try:
            serdes = Serializer()
            deserialized = None
            try:
                deserialized = serdes.deserialize(request.raw_post_data, format=request.META.get('CONTENT_TYPE', 'application/json'))
            except Exception:
                deserialized = None
            del serdes
                    
            if deserialized is None:
                return ImmediateHttpResponse(http.HttpBadRequest())
            
            if 'unregister_c2dm' in deserialized and deserialized['unregister_c2dm'] == True:
                bundle.data['c2dm_id'] = None
            
            updated_bundle = super(UserResource, self).obj_update(bundle, request, **kwargs)
            return updated_bundle
        except (NotFound, MultipleObjectsReturned):
            raise ImmediateHttpResponse(http.HttpBadRequest())
    


class EnvironmentResource(ModelResource):
    features = fields.ListField()
    parent = fields.ForeignKey('self', 'parent', null = True)
    owner = fields.ForeignKey(UserResource, 'owner', full = True)
    
    class Meta:
        queryset = Environment.objects.all()
        resource_name = 'environment'
        #api_name = 'v1/resources'
        #fields = ['name', 'data', 'tags', 'parentID', 'category', 'latitude', 'longitude', 'timestamp']
        excludes = ['width', 'height']
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
    parent = fields.ForeignKey(EnvironmentResource, 'environment')
    
    features = fields.ListField()
    
    class Meta:
        queryset = Area.objects.all()
        resource_name = 'area'
        allowed_methods = ['get']
        excludes = ['shape', 'layout']
        filtering = {
            'parent': ['exact'],
        }
        authentication = Authentication()
        
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``environment`` filter exists
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
    environment = fields.ForeignKey(EnvironmentResource, 'environment')
    area = fields.ForeignKey(AreaResource, 'area', null = True)
    
    class Meta:
        queryset = Announcement.objects.all()
        resource_name = 'announcement'
        allowed_methods = ['get']
        fields = ['data', 'timestamp']
        excludes = ['id']
        filtering = {
            'area': ['exact'],
            'environment': ['exact'],
            'timestamp': ['gt', 'gte'],
        }
        authentication = Authentication()
    
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``environment`` or ``area`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'environment' in request.GET or 'area' in request.GET:
            return super(AnnouncementResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
        
    
    def get_obj_list(self, request):
        ## override the usual obj_list retrieval by filtering out only the yet to be given announcements 
        ## for the current environment (which we **know** must exist) 
        timestamp = datetime.now()
        
        ## get default object list
        announcement_obj_list = super(AnnouncementResource, self).get_object_list(request)
        
        if 'environment' in request.GET:
            try:
                env_id = request.GET['environment']
                environ = Environment.objects.get(id=env_id)
                announcement_obj_list = announcement_obj_list.filter(environment=environ)
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
    environment = fields.ForeignKey(EnvironmentResource, 'environment', null = True)
    area = fields.ForeignKey(AreaResource, 'area', null = True)
    user = fields.ForeignKey(UserResource, 'user')
    
    class Meta:
        queryset = Annotation.objects.all()
        resource_name = 'annotation'
        detail_allowed_methods = ['get', 'put', 'delete']
        list_allowed_methods = ['get', 'post']
        fields = ['data', 'category', 'timestamp']
        #excludes = ['id', 'area']
        filtering = {
            'area': ['exact'],
            'environment': ['exact'],
            'timestamp': ['gt', 'gte'],
            'category': ['exact'],
        }
        ordering = ['timestamp']
        authentication = Authentication()
        authorization = AnnotationAuthorization()
        #validation = FormValidation(form_class = AnnotationForm)
        validation = AnnotationValidation()
        
        
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``area`` or ``environment`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'area' in request.GET or 'environment' in request.GET:
            return super(AnnotationResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    """
    The following methods combined ensure that the environment=1&all=true query is handled successfully
    """
    def build_filters(self, filters = None):
        if filters is None:
            filters = {}
        
        if 'environment' in filters and 'all' in filters and filters['all'] == 'true':
            """
            if environment and all are in the filters, don't apply them any more because it will have
            already been handled in get_object_list
            """
            del filters['environment']
            del filters['all']
            
            orm_filters = super(AnnotationResource, self).build_filters(filters)
            return orm_filters
        else:
            orm_filters = super(AnnotationResource, self).build_filters(filters)
            return orm_filters
    
    
    def get_object_list(self, request):
        from django.db.models import Q
        
        if 'environment' in request.GET and 'all' in request.GET and request.GET['all'] == 'true':
            try:
                environment_pk = request.GET['environment']
                environment = Environment.objects.get(pk=environment_pk)
                q1 = Q(environment=environment)
                q2 = Q(area__in=list(environment.areas.all()))
                
                return super(AnnotationResource, self).get_object_list(request).filter(q1 | q2)
            except Exception:
                #print ex
                raise ImmediateHttpResponse(response=http.HttpBadRequest())
        
        return super(AnnotationResource, self).get_object_list(request)
    
    
    def dehydrate(self, bundle):
        """
        if we are treating an environment=<env_pk>&all=true request, return additionally for each annotation
        bundle the name of the environment/area for which the annotation was made
        """
        if 'environment' in bundle.request.GET and 'all' in bundle.request.GET and bundle.request.GET['all'] == 'true':
            if not bundle.obj.environment is None:
                ## make the environment response a dictionary, containing resource_uri and name
                bundle.data['environment'] = {'resource_uri': bundle.data['environment'], 'name': bundle.obj.environment.name}
            
            if not bundle.obj.area is None:
                ## make the area response a dictionary, containing resource_uri and name
                bundle.data['area'] = {'resource_uri': bundle.data['area'], 'name': bundle.obj.area.name}
        
        """
        now remove also null area/environment data
        """
        if not bundle.data['environment']:
            del bundle.data['environment']
            
        if not bundle.data['area']:
            del bundle.data['area']
        
        return bundle
            
    
    def obj_create(self, bundle, request=None, **kwargs):
        ## because of the AnnotationAuthorization class, request.user will have a profile
        user_profile = request.user.get_profile()
        updated_bundle = super(AnnotationResource, self).obj_create(bundle, request, user=user_profile)
        self._make_c2dm_notification(updated_bundle)
        return updated_bundle
        
    
    def obj_update(self, bundle, request=None, **kwargs):
        """
        Could be an intentional bug that the default obj_update treats DoesNotExist and MultipleObjectReturned
        as acceptable exceptions which get transformed into a CREATE operation.
        We don't want such a behavior. So we catch does exceptions and throw an BadRequest message
        """    
        try:
            updated_bundle = super(AnnotationResource, self).obj_update(bundle, request, **kwargs)
            self._make_c2dm_notification(updated_bundle)
            return updated_bundle
        except (NotFound, MultipleObjectsReturned):
            raise ImmediateHttpResponse(http.HttpBadRequest())
    
    
    def _make_c2dm_notification(self, bundle):
        import socket, pickle, c2dm
        
        user_profile = None
        if not bundle.obj.environment is None:
            user_profile = bundle.obj.environment.owner
            
        if not bundle.obj.area is None:
            user_profile = bundle.obj.area.environment.owner
            
        if not user_profile is None and not user_profile.c2dm_id is None:
            registration_id = user_profile.c2dm_id
            collapse_key = "annotation_" + bundle.obj.category
            resource_uri = self.get_resource_uri(bundle)
            
            environment = bundle.obj.environment
            if not bundle.obj.area is None:
                environment = bundle.obj.area.environment 
            
            location_uri = EnvironmentResource().get_resource_uri(environment)
            feature = bundle.obj.category
            
            data = pickle.dumps((registration_id, collapse_key, location_uri, resource_uri, feature))
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            try:
                # Connect to server and send data
                sock.connect((c2dm.C2DMServer.HOST, c2dm.C2DMServer.PORT))
                sock.sendall(data + "\n")
            
                # Receive data from the server and shut down
                received = sock.recv(1024)
                
                if received == "OK":
                    print "[Annotation C2DM] Notification enqueued"
                else:
                    print "[Annotation C2DM] Notification NOT enqueued"
            except Exception, ex:
                print "[Annotation C2DM] failure enqueueing annotation: ", ex
            finally:
                sock.close()
        

class HistoryResource(ModelResource):
    environment = fields.ForeignKey(EnvironmentResource, 'environment')
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
        from client.views import checkin, checkout, login, logout, register
        
        pattern_list = [
            url(r"^(?P<api_name>%s)%s$" % (self.api_name, trailing_slash()), self.wrap_view('top_level'), name="api_%s_top_level" % self.api_name),
        ]

        for name in sorted(self._registry.keys()):
            self._registry[name].api_name = self.api_name
            pattern_list.append((r"^(?P<api_name>%s)/resources/" % self.api_name, include(self._registry[name].urls)))

        ## then add the actions
        pattern_list.extend([
            url(r"^%s/actions/register/$" % self.api_name, register, name="register"),
            url(r"^%s/actions/login/$" % self.api_name, login, name="login"),
            url(r"^%s/actions/logout/$" % self.api_name, logout, name="logout"),
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
        time_format = "%Y-%m-%dT%H:%M:%S"
        timestamp = datetime.strptime(date_string, time_format)
    except ValueError:
        pass
    
    try:
        ## then try the format %Y-%m-%d %H:%M:%S
        time_format = "%Y-%m-%d %H:%M:%S"
        timestamp = datetime.strptime(date_string, time_format)
    except ValueError:
        pass
    
    return timestamp