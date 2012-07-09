from coresql.models import Environment, Area, Feature, Annotation,\
                           Announcement, History, UserProfile,\
                           ResearchProfile, UserContext, UserSubProfile
#from coresql.forms import AnnotationForm
from tastypie.resources import ModelResource
from tastypie.exceptions import ImmediateHttpResponse, NotFound
#from tastypie.validation import FormValidation
from tastypie import fields, http
from tastypie.authentication import Authentication
from tastypie.api import Api
from client.authorization import AnnotationAuthorization, UserAuthorization,\
    FeatureAuthorization
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
        list_allowed_methods = ["get"]
        #fields = ['first_name']
        excludes = ["id", "timestamp", "is_anonymous"]
        authentication = Authentication()
        authorization = UserAuthorization()
        
    
    def build_filters(self, filters = None):
        """
        enable filtering by environment and area (which do not have their own fields in this resource)
        """
        if filters is None:
            filters = {}
        
        orm_filters = super(UserResource, self).build_filters(filters)
        
        if "area" in filters:
            area_id = filters['area']
            area = Area.objects.get(id = area_id)
            
            #checked_in_user_profiles = [user_ctx.user for user_ctx in UserContext.objects.filter(currentArea = area)]
            orm_filters["pk__in"] = [user_ctx.user.pk 
                                     for user_ctx in UserContext.objects.filter(currentArea = area)]
        
        elif "environment" in filters:
            environment_id = filters['environment']
            environment = Environment.objects.get(id = environment_id)
            
            #checked_in_user_profiles = [user_ctx.user for user_ctx in UserContext.objects.filter(currentArea = area)]
            orm_filters["pk__in"] = [user_ctx.user.pk 
                                     for user_ctx in UserContext.objects.filter(currentEnvironment = environment)]
        
        return orm_filters
    
    def dehydrate_first_name(self, bundle):
        return bundle.obj.user.first_name
        
    def dehydrate_last_name(self, bundle):
        return bundle.obj.user.last_name
    
    def dehydrate_research_profile(self, bundle):
        import inspect, sys
        
        research_dict = {}
        if bundle.obj.research_profile:
            for f in ResearchProfile._meta.fields:
                if not f.primary_key and not hasattr(f, 'foreign_key'):
                    field_class = f.__class__
                    extension_classes = inspect.getmembers(sys.modules["coresql.db.fields"], 
                        lambda c: inspect.isclass(c) and c.__module__ == "coresql.db.fields")
                    
                    if (field_class.__name__, field_class) in extension_classes:
                        research_dict[f.name] = getattr(bundle.obj.research_profile, f.name).to_serializable()
                    else:
                        research_dict[f.name] = getattr(bundle.obj.research_profile, f.name)
        
        return research_dict

    
    def dehydrate(self, bundle):
        #if 'research_profile' in bundle.data and not bundle.obj.research_profile:
        #    del bundle.data['research_profile']
        
        """ dehydrate UserSubProfiles if requested """
        if 'showprofile' in bundle.request.GET and \
            bundle.request.GET['showprofile'] in UserSubProfile.get_subclass_list() + ['all']:
            
            ## get downcasted versions directly of all the subprofiles associated with this userprofile
            profile_type = bundle.request.GET['showprofile']
            subprofiles = []
            
            if profile_type == 'all':
                subprofiles = bundle.obj.subprofiles.all().select_subclasses()
            else:
                subprofiles = bundle.obj.subprofiles.all().select_subclasses(profile_type)
            
            subprofiles_dict = {}
            for profile in subprofiles:
                data = profile.to_serializable()
                if data:
                    subprofiles_dict.update(data)
                    
            if subprofiles_dict:
                bundle.data['subprofiles'] = subprofiles_dict
            
        """ if the user is requesting his own data then return his email too as it
            is an identifying element """    
        if hasattr(bundle.request, "user") and not bundle.request.user.is_anonymous():
            user_profile = bundle.request.user.get_profile()
            if user_profile.pk == bundle.obj.pk:
                bundle.data['email'] = bundle.obj.user.email 
    
        """ remove c2dm data from bundle """
        if 'c2dm_id' in bundle.data:
            del bundle.data['c2dm_id']
        
        return bundle
    
    
    def get_list(self, request, **kwargs):
        ## override the list retrieval part to verify additionally that an ``environment`` or ``area`` filter exists
        ## otherwise reject the call with a HttpMethodNotAllowed
        if 'environment' in request.GET or 'area' in request.GET:
            return super(UserResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    def apply_sorting(self, obj_list, options=None):
        ## apply a default sorting of user by their last_name
        return obj_list.order_by("user__last_name")
    
    def obj_update(self, bundle, request=None, **kwargs):
        """
        Could be an intentional action that the default obj_update treats DoesNotExist and MultipleObjectReturned
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
        if "entryfeaturequery" in bundle.request.GET and bundle.request.GET['entryfeaturequery'] == 'true':
            feature_list = self._entry_feature_query(bundle)
            return feature_list
        else:
            ## return a list of dictionary values from the features of this environment
            feature_list = []
            for feature in bundle.obj.features.all().select_subclasses():
                feat_dict = feature.to_serializable()
                if feat_dict:
                    feature_list.append(feat_dict)
                    
            return feature_list
    
    
    def dehydrate(self, bundle):
        """
        append layout url if a level filter exists in the request 
        """
        if "level" in bundle.request.GET:
            level = int(bundle.request.GET["level"])
            bundle.data["layout_url"] = bundle.obj.layouts.get(level=level).mapURL
        
        """
        make bundle consistent for location parsing on mobile client: 
            add a location_type entry in the bundle.data
            put all the rest of the data under location_data
        """
        location_data = bundle.data.copy()
        bundle.data.clear()
        bundle.data['location_type'] = self._meta.resource_name
        bundle.data['location_data'] = location_data
        
        return bundle
    
    
    def _entry_feature_query(self, bundle):
        from coresql.models import Entry
        
        entry_id = bundle.request.GET['entry_id']
        entry = Entry.objects.get(id = entry_id)
        
        entry_dict = {'category' : "program",
                      'data' : {'id' : entry.id,
                                'title' : entry.title,
                                'speakers' : entry.speakers,
                                'sessionId' : entry.session.id,
                                'sessionTitle' : entry.session.title,
                                'startTime' : entry.startTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                'endTime' : entry.endTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                'abstract' : "Abstract not available."
                                }
                      }
        
        if entry.abstract:
            entry_dict['data']['abstract'] = entry.abstract
            
        return [entry_dict]
        

class AreaResource(ModelResource):
    parent = fields.ForeignKey(EnvironmentResource, 'environment')
    features = fields.ListField()
    owner = fields.DictField()
    
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
    
    def dehydrate_owner(self, bundle):
        user_res = UserResource()
        user_bundle = user_res.build_bundle(bundle.obj.environment.owner, request=bundle.request)
        user_bundle = user_res.full_dehydrate(user_bundle)
        
        return user_bundle.data
    
    def dehydrate_features(self, bundle):
        if "entryfeaturequery" in bundle.request.GET and bundle.request.GET['entryfeaturequery'] == 'true':
            feature_list = self._entry_feature_query(bundle)
            return feature_list
        else:
            feature_list = []
            for feature in bundle.obj.features.all().select_subclasses():
                feat_dict = feature.to_serializable()
                if feat_dict:
                    feature_list.append(feat_dict)
    
            ## then see if environment features which also apply to the area are available - e.g. program, order
            ## we handle the "program" case for now
            environment = bundle.obj.environment
            environment_features = environment.features.all().select_subclasses()
            
            for env_feat in environment_features:
                feat_dict = env_feat.to_serializable()
                if feat_dict:
                    feature_list.append(feat_dict)
                    
            return feature_list
    
    
    def dehydrate(self, bundle):
        """
        append level data from the layout reference of the Area obj
        """
        bundle.data['level'] = bundle.obj.layout.level
        
        """
        make bundle consistent for location parsing on mobile client: 
            add a location_type entry in the bundle.data
            put all the rest of the data under location_data
        """
        location_data = bundle.data.copy()
        bundle.data.clear()
        bundle.data['location_type'] = self._meta.resource_name
        bundle.data['location_data'] = location_data
        
        return bundle
    
    
    def _entry_feature_query(self, bundle):
        from coresql.models import Entry
        
        entry_id = bundle.request.GET['entry_id']
        entry = Entry.objects.get(id = entry_id)
        
        entry_dict = {'category' : "program",
                      'data' : {'id' : entry.id,
                                'title' : entry.title,
                                'speakers' : entry.speakers,
                                'sessionId' : entry.session.id,
                                'sessionTitle' : entry.session.title,
                                'startTime' : entry.startTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                'endTime' : entry.endTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                'abstract' : "Abstract not available."
                                }
                      }
        
        if entry.abstract:
            entry_dict['data']['abstract'] = entry.abstract
            
        return [entry_dict]


class FeatureResource(ModelResource):
    environment = fields.ForeignKey(EnvironmentResource, 'environment', null = True)
    area = fields.ForeignKey(AreaResource, 'area', null = True)
    category = fields.CharField(attribute = 'category')
    data = fields.DictField()
    
    class Meta:
        queryset = Feature.objects.all().select_subclasses()
        resource_name = 'feature'
        allowed_methods = ['get']
        excludes = ['id', 'timestamp']
        filtering = {
            'area' : ['exact'],
            'environment' : ['exact'],
            'category' : ['exact']
        }
        authentication = Authentication()
        authorization = FeatureAuthorization()
    
    
    def get_list(self, request, **kwargs):
        """
        override the list retrieval part to verify additionally that an ``area`` or ``environment`` 
        and a ``category`` filter exist otherwise reject the call with a HttpMethodNotAllowed
        """
        if ('area' in request.GET or 'environment' in request.GET) and 'category' in request.GET:
            return super(FeatureResource, self).get_list(request, **kwargs)
        else:
            raise ImmediateHttpResponse(response=http.HttpMethodNotAllowed())
    
    
    def dehydrate_data(self, bundle):
        filters = bundle.request.GET.copy()
        return bundle.obj.get_feature_data(filters)
    

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
    data = fields.DictField()
    
    class Meta:
        queryset = Annotation.objects.all()
        resource_name = 'annotation'
        detail_allowed_methods = ['get', 'put', 'delete']
        list_allowed_methods = ['get', 'post']
        #fields = ['data', 'category', 'timestamp']
        fields = ['category', 'timestamp']
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
        
        if "category" in filters:
            """
            if annotations are filtered by category see if any category specific filters also apply
            """
            categ_specific_filters = self._category_filtering(filters)
            orm_filters.update(categ_specific_filters)
            
        return orm_filters
        
    
    def _category_filtering(self, filters):
        specific_filters = {}
        
        ## switch by category
        if filters['category'] == "program":
            ## program feature specific filtering
            if "entry_id" in filters:
                specific_filters['entryannotation__entry__id'] = int(filters['entry_id'])
    
        return specific_filters
    
    
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
    
    
    def dehydrate_data(self, bundle):
        ## return the data representation of this annotation according to its type
        annotation_category = bundle.obj.category
        if annotation_category == 'default':
            return {'text' : bundle.obj.descriptionannotation.text}
        elif annotation_category == 'program':
            return {'text' : bundle.obj.entryannotation.text}
        else:
            return None    
    
    
    def dehydrate(self, bundle):
        """
        return additionally for each annotation
        bundle the name of the environment/area for which the annotation was made
        """
        if not bundle.obj.environment is None:
            ## make the environment response a dictionary, containing resource_uri and name
            bundle.data['environment'] = {'resource_uri': bundle.data['environment'], 'name': bundle.obj.environment.name}
        
        if not bundle.obj.area is None:
            ## make the area response a dictionary, containing resource_uri and name
            bundle.data['area'] = {'resource_uri': bundle.data['area'], 'name': bundle.obj.area.name}
        
        
        """
        bundle in the user's first and last name under the ['data']['user'] entry 
        """
        first_name = "Anonymous"
        last_name = "Guest"
            
        user_profile = bundle.obj.user
        
        if not user_profile.is_anonymous:
            first_name = user_profile.user.first_name
            last_name = user_profile.user.last_name
                
        bundle.data['data']['user'] = {'first_name' : first_name,
                                       'last_name': last_name }
        
        """
        now remove also null area/environment data
        """
        if not bundle.data['environment']:
            del bundle.data['environment']
            
        if not bundle.data['area']:
            del bundle.data['area']
    
        """
        if no data is found remove the 'data' attribute from the bundle to avoid useless processing on
        the mobile side 
        """
        if not bundle.data['data']:
            del bundle.data['data']
            
        
        return bundle
   
   
    
    def hydrate(self, bundle):
        from coresql.models import DescriptionAnnotation, EntryAnnotation, Entry
        
        """
        switch after the annotation category and construct the appropriate annotation type object with
        the given data
        """
        if bundle.data['category'] == 'default':
            bundle.obj = DescriptionAnnotation(user = bundle.request.user.get_profile(), 
                                               environment = bundle.obj.environment,
                                               area = bundle.obj.area,
                                               category = bundle.obj.category,
                                               text = bundle.data['data']['text'])
        elif bundle.data['category'] == 'program':
            entry_id = bundle.data['data']['entry_id']
            entry = Entry.objects.get(id = entry_id);
            
            bundle.obj = EntryAnnotation(user = bundle.request.user.get_profile(), 
                                         environment = bundle.obj.environment,
                                         area = bundle.obj.area,
                                         category = bundle.obj.category,
                                         entry = entry,
                                         text = bundle.data['data']['text'])
        
            
        return bundle
        
    
    def obj_create(self, bundle, request=None, **kwargs):
        ## because of the AnnotationAuthorization class, request.user will have a profile
        user_profile = request.user.get_profile()
        updated_bundle = super(AnnotationResource, self).obj_create(bundle, request, user=user_profile)
        
        ## make notification for 'order' type annotations
        if updated_bundle.data['category'] == 'order':
            self._make_c2dm_notification(updated_bundle)
        
        return updated_bundle
        
    
    def obj_update(self, bundle, request=None, **kwargs):
        """
        Could be an intentional bug that the default obj_update treats DoesNotExist and MultipleObjectReturned
        as acceptable exceptions which get transformed into a CREATE operation.
        We don't want such a behavior. So we catch those exceptions and throw a BadRequest message
        """    
        try:
            updated_bundle = super(AnnotationResource, self).obj_update(bundle, request, **kwargs)
            
            ## make notification for 'order' type annotations
            if updated_bundle.data['category'] == 'order':
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