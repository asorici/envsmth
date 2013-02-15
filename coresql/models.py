from coresql.db import fields
from django.contrib.auth.models import User
from django.db import models
from django.db.models.fields.related import SingleRelatedObjectDescriptor
from django.db.models.signals import post_save

from django_facebook.models import FacebookProfileModel
from model_utils.managers import InheritanceManager
from coresql.exceptions import AnnotationException

import datetime

CATEGORY_CHOICES = ( 
    ("description", "description"),
    ("booth_description", "booth_description"), 
    ("order", "order"),
    ("program", "program"),
    ("people", "people")
)    

"""
####################################### UserProfile Model Classes ######################################## 
"""

class UserProfile(models.Model):
    user = models.OneToOneField(User)
    
    timestamp = models.DateTimeField()
    is_anonymous = models.BooleanField(default = False)
    c2dm_id = models.CharField(max_length=256, null = True, blank = True)
    
    def __unicode__(self):
        return self.user.username + ": anonymous=" + str(self.is_anonymous)
    
    def save(self, *args, **kwargs):
        ''' On save, update timestamp '''
        self.timestamp = datetime.datetime.now()
        super(UserProfile, self).save(*args, **kwargs)
        
    
    def copy_from_profile(self, user_profile):
        ''' copy the UserContext data and c2dm_id from the given user_profile - equivalent of a user switch '''
        self.c2dm_id = user_profile.c2dm_id
        user_context = UserContext.from_user(user_profile, self)
        if not user_context is None:
            self.context = user_context
        
        self.save()
        

def create_user_profile(sender, instance, created, **kwargs):
    if created:
        UserProfile.objects.create(user=instance)

post_save.connect(create_user_profile, sender=User)

class UserSubProfile(models.Model):
    userprofile = models.ForeignKey(UserProfile, related_name = "subprofiles")
    
    objects = InheritanceManager()
    
    def __unicode__(self):
        return self.userprofile.user.username + ": anonymous=" + str(self.userprofile.is_anonymous)
    
    @staticmethod
    def update_user_profile_timestamp(sender, instance, created, **kwargs):
        ''' update user profile timestamp when sub profile is created or changed '''
        instance.userprofile.timestamp = datetime.datetime.now()
        instance.userprofile.save()
    
    @staticmethod
    def get_subclass_list():
        subclasses = [o for o in dir(UserSubProfile)
                      if isinstance(getattr(UserSubProfile, o), SingleRelatedObjectDescriptor)\
                      and issubclass(getattr(UserSubProfile, o).related.model, UserSubProfile)]
        
        return subclasses
    
    def profile_name(self):
        return self._meta.object_name.lower()
    
    def to_serializable(self):
        return None

''' update user profile timestamp when sub profile is created or changed '''
post_save.connect(UserSubProfile.update_user_profile_timestamp, sender = UserSubProfile)
    

class ResearchProfile(UserSubProfile):
    affiliation = models.CharField(max_length = 256, null = True, blank = True)
    research_interests = fields.TagListField(null = True, blank = True)
    
    def __unicode__(self):
        return self.affiliation + " ## " + str(self.research_interests.getList())
    
    def to_serializable(self):
        profile_name = self.profile_name()
        data = { profile_name : {'affiliation' : self.affiliation,
                                 'research_interests' : self.research_interests.getList()
                                }
               }
        
        return data


class FacebookProfile(UserSubProfile, FacebookProfileModel):
    pass
    

"""
######################################### Location Model Classes #########################################
"""

class Environment(models.Model):
    owner = models.ForeignKey(UserProfile)
    name = models.CharField(max_length=140)
    
    #category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    #data = fields.DataField()
    
    parent = models.ForeignKey('self', null = True, blank = True, related_name="children")
    tags = fields.TagListField(null = True, blank = True)
    img_thumbnail_url = models.URLField(null = True, blank = True, max_length = 256)
    
    width = models.IntegerField(null = True, blank = True)
    height = models.IntegerField(null = True, blank = True)
    latitude = models.FloatField(null = True, blank = True)
    longitude = models.FloatField(null = True, blank = True)
    timestamp = models.DateTimeField(auto_now = True)

    def __unicode__(self):
        return self.name + "(" + str(self.id) + ")"
            

class Layout(models.Model):
    environment = models.ForeignKey(Environment, related_name = "layouts")
    level = models.IntegerField(default = 0)
    mapURL = models.URLField(null = True, blank = True)
    timestamp = models.DateTimeField(auto_now = True)
    
    def __unicode__(self):
        return self.environment.name + " level(" + str(self.level) + ")" 


class Area(models.Model):
    TYPE_CHOICES = (
        ("interest", "interest"), 
        ("non-interest", "non-interest")
    )
    
    environment = models.ForeignKey(Environment, related_name = "areas")
    areaType = models.CharField(max_length=50, choices = TYPE_CHOICES)
    admin = models.ForeignKey(UserProfile, null = True, blank = True, related_name = "administered_areas")
    name = models.CharField(max_length=140)
    
    tags = fields.TagListField(null = True, blank = True)
    layout = models.ForeignKey(Layout, related_name = "areas", blank = True)
    img_thumbnail_url = models.URLField(null = True, blank = True, max_length = 256)
    
    shape = fields.AreaShapeField(blank = True, null = True)
    timestamp = models.DateTimeField(auto_now = True)
    
    def __unicode__(self):
        return self.name + "(" + str(self.id) + ")"



class Announcement(models.Model):
    REPEAT_EVERY_CHOICES = (
        ("none", "none"), 
        ("day", "day"), 
        ("week", "week")
    )
    
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "announcements")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "announcements")
    
    data = fields.DataField()
    repeatEvery = models.CharField(max_length=50, choices = REPEAT_EVERY_CHOICES, default = "none")
    followed_by = models.ManyToManyField(UserProfile, null = True, blank = True, related_name = "follows_announcements")
    
    triggers = fields.DateTimeListField()
    timestamp = models.DateTimeField(auto_now = True)


"""
#################################### Annotation Model Classes #############################################
"""
class Annotation(models.Model):
    DESCRIPTION = "description"
    BOOTH_DESCRIPTION = "booth_description"
    PROGRAM = "program"
    ORDER = "order"
    
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "annotations")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "annotations")
    user = models.ForeignKey(UserProfile, null = True, blank = True, on_delete=models.SET_NULL)
    #data = fields.DataField()
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES, default="default")
    timestamp = models.DateTimeField(auto_now = True)
    
    # use the inheritance manager to get access directly to subclasses of Annotation when 
    # retrieving sets of annotations
    objects = InheritanceManager()
    
    @staticmethod
    def get_subclasses():
        import sys
        mod = sys.modules[Annotation.__module__]
        
        for name in dir(mod):
            o = getattr(mod, name)
            try:
                if (o != Annotation) and issubclass(o, Annotation):
                    yield name, o
            except TypeError: 
                pass
    
    def __unicode__(self):
        location_name = None
        if self.environment:
            location_name = self.environment
        elif self.area:
            location_name = self.area
        
        
        if self.user and location_name:
            return str(self.user) + " - " + location_name
        elif location_name:
            return "annotation for location " + location_name + " but empty user field"
        else:
            return "empty annotation object"
        
    def get_annotation_data(self):
        return None
    
    @classmethod
    def is_annotation_for(cls, category, annotation_data):
        """
        - category provides type of annotation
        - currently unused, but left for future purposes, annotation_data can also discriminate
          between annotation classes
        """
        return False
    
    
    @classmethod
    def validate_data(cls, category, annotation_data):
        """
        Provide additional validation of the `data' field from an annotation according to the specific
        subclass. Returns a list of error messages which is empty by default.
        """
        return []
    
    
    @classmethod
    def get_extra_filters(cls, filters):
        return {}
        
######################################## Description Class ##########################################
class DescriptionAnnotation(Annotation):
    text = models.TextField()
    
    def __init__(self, *args, **kwargs):
        data = kwargs.pop('data', None)
        
        super(DescriptionAnnotation, self).__init__(*args, **kwargs)
        
        if not data is None:
            if 'text' in data:
                self.text = data['text']
            else:
                raise AnnotationException("Description Annotation missing text")
    
    def get_annotation_data(self):
        return { 'text' : self.text }
    
    @classmethod
    def is_annotation_for(cls, category, annotation_data):
        return category == Annotation.DESCRIPTION


######################################## BoothDescription Class ##########################################
class BoothDescriptionAnnotation(Annotation):
    BOOTH_DESCRIPTION = "booth_description"
    PRODUCT_DESCRIPTION = "product_description"
    
    TOPIC_CHOICES = (
        (BOOTH_DESCRIPTION, BOOTH_DESCRIPTION), 
        (PRODUCT_DESCRIPTION, PRODUCT_DESCRIPTION)
    )
    
    topic_type = models.CharField(max_length = 32, choices = TOPIC_CHOICES, default="booth_description")
    topic_title = models.CharField(max_length = 128)
    text = models.TextField()
    
    booth_product = models.ForeignKey('BoothProduct', null = True, blank = True, related_name = 'annotations') 
    
    def __init__(self, *args, **kwargs):
        data = kwargs.pop('data', None)
        
        super(BoothDescriptionAnnotation, self).__init__(*args, **kwargs)
        
        if not data is None:
            if 'text' in data and 'topic_type' in data and 'topic_title' in data:
                self.text = data['text']
                self.topic_type = data['topic_type']
                self.topic_title = data['topic_title']
                
                if self.topic_type == BoothDescriptionAnnotation.PRODUCT_DESCRIPTION and 'product_id' in data:
                    try:
                        product_id = data['product_id']
                        self.booth_product = BoothProduct.objects.get(id = product_id)
                    except BoothProduct.DoesNotExist:
                        raise AnnotationException("BoothDescripionAnnotation missing valid product product_id")
            else:
                raise AnnotationException("Booth Description Annotation missing text, topic_type or topic_title")
                
    
    
    def get_annotation_data(self):
        data_dict = {'topic_type' : self.topic_type,
                     'topic_title' : self.topic_title,
                     'text' : self.text 
                    }
        
        if not self.booth_product is None:
            data_dict['product_id'] = self.booth_product.id
        
        return data_dict
    
    
    @classmethod
    def is_annotation_for(cls, category, annotation_data):
        return category == Annotation.BOOTH_DESCRIPTION
    
    
    @classmethod
    def get_extra_filters(cls, filters):
        specific_filters = {}
        
        ## just this single case for now
        if "product_id" in filters:
            try:
                product = BoothProduct.objects.get(id = filters['product_id'])
                specific_filters['id__in'] = [ann.id for ann in BoothDescriptionAnnotation.objects.filter(product = product)]
            except BoothProduct.DoesNotExist:
                pass
            except Exception:
                pass 
        
        return specific_filters

    
##################################### PresentationAnnotation Class ########################################
class ProgramAnnotation(Annotation):
    text = models.TextField()
    presentation = models.ForeignKey('Presentation', related_name = "annotations")
    
    def __init__(self, *args, **kwargs):
        data = kwargs.pop('data', None)
        
        super(ProgramAnnotation, self).__init__(*args, **kwargs)
        
        if not data is None:
            if 'text' in data and 'presentation_id' in data:
                self.text = data['text']
                
                presentation_id = data['presentation_id']
                try:
                    self.presentation = Presentation.objects.get(id = presentation_id)
                except Presentation.DoesNotExist:
                    raise AnnotationException("ProgramAnnotation missing valid program presentation_id")
            else:
                raise AnnotationException("ProgramAnnotation missing text or presentation data")
    
    
    def get_annotation_data(self):
        return { 'text' : self.text }
    
    @classmethod
    def is_annotation_for(cls, category, annotation_data):
        return category == Annotation.PROGRAM
    
    @classmethod
    def get_extra_filters(cls, filters):
        specific_filters = {}
        
        ## just this single case for now
        if "presentation_id" in filters:
            try:
                presentation = Presentation.objects.get(id = filters['presentation_id'])
                specific_filters['id__in'] = [ann.id for ann in ProgramAnnotation.objects.filter(presentation = presentation)]
            except Presentation.DoesNotExist:
                pass
            except Exception:
                pass 
        
        return specific_filters
   
######################################## OrderAnnotation Class ###########################################
class OrderAnnotation(Annotation):
    order = fields.DataField() 
    
    def __init__(self, *args, **kwargs):
        data = kwargs.pop('data', None)
        super(OrderAnnotation, self).__init__(*args, **kwargs)
        
        """
        if not data is None:
            if 'order' in data:
                self.order = data
            else:
                raise AnnotationException("OrderAnnotation missing order data")
        """
        if not data is None:
            self.order = data
            
            
    def get_annotation_data(self):
        return self.order.to_serializable()
        
    
    @classmethod
    def is_annotation_for(cls, category, annotation_data):
        return category == Annotation.ORDER
    
    
    @classmethod
    def validate_data(cls, category, annotation_data):
        if not isinstance(annotation_data, dict):
            return ["Annotation data is required to be a dictionary."]
        else:
            req_type = annotation_data.get('order_request_type')
            
            if req_type is None:
                return ["No order_request_type specified in annotation data."]
            elif not req_type in [OrderFeature.CALL_WAITER, OrderFeature.CALL_CHECK, OrderFeature.NEW_ORDER]:
                return ["Unknown value `" + str(req_type) + "' for order request type."]
            
        return [] 
    
    
    @staticmethod
    def post_save_action(sender, instance, created, **kwargs):
        import sys
        
        ## if the instance was newly created
        if created:
            order_instance = instance.order.data
            
            ## if the instance is a dictionary as it is supposed to be
            if isinstance(order_instance, dict) and 'item_id_list' in order_instance:
                try:
                    order_items = order_instance['item_id_list']
                    for item_dict in order_items:
                        item_id = item_dict['id']
                        item_quantity = item_dict['quantity']
                        
                        menu_item = MenuItem.objects.get(id = item_id)
                        menu_item.num_orders_current += item_quantity
                        
                        menu_categ = menu_item.category
                        menu_categ.num_orders_current += item_quantity
                        
                        menu_item.save()
                        menu_categ.save()
                        
                except Exception, ex:
                    print >> sys.stderr, ex
                except KeyError, ke:
                    print >> sys.stderr, ke
                    
post_save.connect(OrderAnnotation.post_save_action, sender = OrderAnnotation)
            
"""
############################### History and Context Model Classes ########################################
"""    

class History(models.Model):
    user = models.ForeignKey(UserProfile)
    area = models.ForeignKey(Area)
    environment = models.ForeignKey(Environment)
    timestamp = models.DateTimeField(auto_now = True)
    

class Privacy(models.Model):
    user = models.ForeignKey(UserProfile)
    environment = models.ForeignKey(Environment)
    relation = models.CharField(max_length=50)
    

class UserContext(models.Model):
    user = models.OneToOneField(UserProfile, related_name='context')
    currentEnvironment = models.ForeignKey(Environment, null = True, blank = True)
    currentArea = models.ForeignKey(Area, null = True, blank = True)
    virtual = models.BooleanField()
    
    @staticmethod
    def from_user(from_user_profile, to_user_profile):
        if hasattr(from_user_profile, "context"):
            from_context = from_user_profile.context
            return UserContext(user = to_user_profile,
                           currentArea = from_context.currentArea, 
                           currentEnvironment = from_context.currentEnvironment, 
                           virtual = from_context.virtual)
        return None


"""
####################################### Feature Model Classes ###########################################
"""

class Feature(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "features")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "features")
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    is_general = models.BooleanField(default = False)
    #data = fields.DataField(null = True, blank = True)
    version = models.SmallIntegerField(default = 1)
    timestamp = models.DateTimeField()
    
    def save(self, *args, **kwargs):
        ''' On save, update timestamp '''
        self.timestamp = datetime.datetime.now()
        super(Feature, self).save(*args, **kwargs)
    
    
    # use the inheritance manager to get access directly to subclasses of Feature when w
    # retrieving sets of Features
    objects = InheritanceManager()
    
    class Meta:
        unique_together = (("environment", "category"), ("area", "category"))
    
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = {'category' : self.category, 
                              'version' : self.version, 
                              'timestamp': self.timestamp,
                              'is_general': self.is_general
                              }
        if include_data:
            serialized_feature['data'] = None
        
        return serialized_feature
    
    def __unicode__(self):
        if self.area:
            return "feature type(" + self.category + ") for area(" + self.area.name + ")"
        elif self.environment:
            return "feature type(" + self.category + ") for env(" + self.environment.name + ")"
        else:
            return "feature type(" + self.category + ") but no location assigned -- needs fix"
    
    def get_feature_data(self, virtual, filters):
        return self.to_serializable(virtual = virtual, include_data = True)['data']
    

####################################### Description Feature Class #############################################
class DescriptionFeature(Feature):
    description = models.TextField(null = True, blank = True)
    newest_info = models.TextField(null = True, blank = True)
    img_url = models.URLField(null = True, blank = True, max_length = 256)
    
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = super(DescriptionFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        
        if include_data:
            data_dict = {}
            
            if self.description:
                data_dict['description'] = self.description
                
            if self.newest_info:
                data_dict['newest_info'] = self.newest_info
            
            if self.img_url:
                data_dict['img_url'] = self.img_url
            
            serialized_feature.update( {'data' : data_dict} )
        
        return serialized_feature
    
    
    def get_feature_data(self, virtual, filters):
        return self.to_serializable(virtual = virtual, include_data = True)['data']


class BoothDescriptionFeature(Feature):
    ## contact details
    description = models.TextField(null = True, blank = True)
    image_url = models.URLField(null = True, blank = True, max_length = 256)
    
    contact_email = models.EmailField(null = True, blank = True, max_length = 128)
    contact_website = models.URLField(null = True, blank = True, max_length = 256)
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = super(BoothDescriptionFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        
        if include_data:
            data_dict = { 'id' : self.id }
            
            if self.description:
                data_dict['description'] = self.description
            
            ''' take tags from location to which this feature is attached '''
            location = None
            if not self.environment is None:
                location = self.environment
            elif not self.area is None:
                location = self.area
                
            if location and location.tags:
                data_dict['tags'] = location.tags.getList()
            
            if self.image_url:
                data_dict['image_url'] = self.image_url
            
            if self.contact_email:
                data_dict['contact_email'] = self.contact_email
                
            if self.contact_website:
                data_dict['contact_website'] = self.contact_website
            
            
            product_list = []
            for product in self.products.all():
                product_dict = {'product_id' : product.id,
                                'product_name' : product.name,
                                'product_description' : product.description}
                
                if product.image_url:
                    product_dict['product_image_url'] = product.image_url
                
                if product.website_url:
                    product_dict['product_website_url'] = product.website_url
                    
                product_list.append(product_dict)
            
            if product_list:
                data_dict['products'] = product_list
            
            serialized_feature.update( {'data' : data_dict} )
        
        return serialized_feature
    
    def get_feature_data(self, virtual, filters):
        return self.to_serializable(virtual = virtual, include_data = True)['data']
    
    
class BoothProduct(models.Model):
    booth = models.ForeignKey(BoothDescriptionFeature, related_name = "products")
    name = models.CharField(max_length = 256)
    description = models.TextField()
    
    image_url = models.URLField(null = True, blank = True, max_length = 256)
    website_url = models.URLField(null = True, blank = True, max_length = 256)
    
    def __unicode__(self):
        return self.name + " @ " + str(self.booth)
    
    
###################################### Program Feature Classes ############################################
class ProgramFeature(Feature):
    QUERY_TYPES = ('presentation', 'speaker', 'search')
    
    description = models.TextField(null = True, blank = True)
    
    def to_serializable(self, virtual = False, include_data = False):
        from client.api import EnvironmentResource, AreaResource
        
        serialized_feature = super(ProgramFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        
        if include_data:
            program_dict = {'data' : {'description' : self.description} }
            
            sessions_list = []
            presentation_list = []
            speaker_list = []
            presentation_speakers_list = []
            
            sessions = self.sessions.all()
            for s in sessions:
                session_dict = {'id' : s.id,
                                'title' : s.title,
                                'tag' : s.tag,
                               }
                
                ## we add the data of the area in which this session of presentations is to take place
                session_dict['location_url'] = AreaResource().get_resource_uri(s.location)
                session_dict['location_name'] = s.location.name
                
                sessions_list.append(session_dict)
                
                presentations = s.presentations.all().order_by('startTime')
                for pres in presentations:
                    presentation_dict = {'id' : pres.id,
                                        'title' : pres.title,
                                        'sessionId' : s.id,
                                        'startTime' : pres.startTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                        'endTime' : pres.endTime.strftime("%Y-%m-%dT%H:%M:%S")
                                        }
                    if pres.abstract:
                        presentation_dict['abstract'] = pres.abstract
                    
                    if pres.tags:
                        presentation_dict['tags'] = ";".join(pres.tags.getList())
                    
                    presentation_list.append(presentation_dict)
                    
                    speakers = pres.speakers.all().order_by('last_name')
                    for speaker in speakers:
                        presentation_speaker_dict = {'presentation_id' : pres.id,
                                                    'speaker_id': speaker.id
                                                    }
                        presentation_speakers_list.append(presentation_speaker_dict)
                        
                        if not any(d.get('id', None) == speaker.id for d in speaker_list):
                            speaker_dict = {'id': speaker.id,
                                            'first_name': speaker.first_name,
                                            'last_name': speaker.last_name,
                                            'affiliation': speaker.affiliation,
                                            'position': speaker.position
                                            }
                                    
                            if speaker.biography:
                                speaker_dict['biography'] = speaker.biography
                            
                            if speaker.email:
                                speaker_dict['email'] = speaker.email
                            
                            if speaker.online_profile_link:
                                speaker_dict['online_profile_link'] = speaker.online_profile_link
                            
                            if speaker.image_url:
                                speaker_dict['image_url'] = speaker.image_url
                            
                            speaker_list.append(speaker_dict)
                        
            """
            distinct_program_days_list =\
                Presentation.objects.values('startTime').\
                    extra({'start_date' : "date(startTime)"}).values('start_date').distinct()
            
            program_days = map(lambda x: x['start_date'].strftime("%Y-%m-%dT%H:%M:%S"), 
                                distinct_program_days_list)
            """
            program_dict['data']['program'] =  {#'program_days': program_days,
                                                'sessions' : sessions_list, 
                                                'presentations' : presentation_list,
                                                'speakers': speaker_list,
                                                'presentation_speakers' : presentation_speakers_list
                                               }
            
            serialized_feature.update(program_dict)
        
        return serialized_feature
    
    
    def get_feature_data(self, virtual, filters):
        if 'querytype' in filters:
            if filters['querytype'] in self.QUERY_TYPES:
                if filters['querytype'] == 'presentation':
                    presentation_id = filters.get('presentation_id')
                    if not presentation_id:
                        return None
                    
                    presentation = Presentation.objects.get(id = presentation_id)
                    presentation_dict = {'id' : presentation.id,
                                    'title' : presentation.title,
                                    'sessionId' : presentation.session.id,
                                    'sessionTitle' : presentation.session.title,
                                    'startTime' : presentation.startTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                    'endTime' : presentation.endTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                 }
                    
                    if presentation.abstract:
                        presentation_dict['abstract'] = presentation.abstract
                        
                    return presentation_dict
                
                elif filters['querytype'] == 'speaker':
                    speaker_id = filters.get('speaker_id')
                    if not speaker_id:
                        return None
                    
                    speaker = Speaker.objects.get(id = speaker_id)
                    speaker_dict = {'id': speaker.id,
                                        'first_name': speaker.first_name,
                                        'last_name': speaker.last_name,
                                        'affiliation': speaker.affiliation,
                                        'position': speaker.position
                                        }
                    
                    if speaker.biography:
                        speaker_dict['biography'] = speaker.biography
                            
                    if speaker.email:
                        speaker_dict['email'] = speaker.email
                            
                    if speaker.online_profile_link:
                        speaker_dict['online_profile_link'] = speaker.online_profile_link
                        
                    if speaker.image_url:
                        speaker_dict['image_url'] = speaker.image_url
                        
                    return speaker_dict
                    
            else:
                ## return None if the querytype is un-defined
                return None
        
        else:
            ## return the entire to_serializable data on program features; 
            ## this can be the case when we query for the list of all features on the FeatureResource
            return self.to_serializable(virtual = virtual, include_data = True)['data']
        
    
    
class Session(models.Model):
    title = models.CharField(max_length = 256)
    tag = models.CharField(max_length = 8)
    program = models.ForeignKey(ProgramFeature, related_name = "sessions")
    location = models.ForeignKey(Area, null = False)
    
    def save(self, *args, **kwargs):
        ''' On save, update timestamp for associated program feature'''
        self.program.timestamp = datetime.datetime.now()
        self.program.save()
        super(Session, self).save(*args, **kwargs)
    
    def __unicode__(self):
        return self.title + " @ " + str(self.program)
    

class Presentation(models.Model):
    session = models.ForeignKey(Session, related_name = "presentations")
    speakers = models.ManyToManyField("Speaker", related_name = "presentations")
    
    title = models.CharField(max_length = 256)
    startTime = models.DateTimeField()
    endTime = models.DateTimeField()
    
    abstract = models.TextField(null = True, blank = True)
    tags = fields.TagListField(null = True, blank = True)
    
    def save(self, *args, **kwargs):
        ''' On save, update timestamp for associated program feature'''
        self.session.program.timestamp = datetime.datetime.now()
        self.session.program.save()
        super(Presentation, self).save(*args, **kwargs)
    
    def __unicode__(self):
        return self.title + " >> " + self.session.title

        
class Speaker(models.Model):
    first_name = models.CharField(max_length = 64)
    last_name = models.CharField(max_length = 64)
    affiliation = models.CharField(max_length = 128)
    position = models.CharField(max_length = 64)
    
    biography = models.TextField(null = True, blank = True)
    email = models.EmailField(null = True, blank = True)
    online_profile_link = models.URLField(null = True, blank = True)
    image_url = models.URLField(null = True, blank = True)
    
    class Meta:
        unique_together = ("first_name", "last_name")
        
    def __unicode__(self):
        return self.first_name + " " + self.last_name + " (" + self.position + ", " + self.affiliation + ")"

###################################### People Feature Classes ############################################
"""
This is mainly a hack so as to keep the programming model from the Android Application
"""
class PeopleFeature(Feature):
    description = models.TextField(null = True, blank = True)
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = super(PeopleFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        if include_data:
            serialized_feature.update( {'data' : self.description} )
        
        return serialized_feature

####################################### Order Feature Classes #############################################
class OrderFeature(Feature):
    NEW_REQUEST         = "new_request"
    RESOLVED_REQUEST    = "resolved_request"
    
    NEW_ORDER           = "new_order"
    CALL_WAITER         = "call_waiter"
    CALL_CHECK          = "call_check"
    UPDATE_CONTENT      = "update_content"
    UPDATE_STRUCTURE    = "update_structure"
    
    """
    For now replicates the old functionality and data model - a single level: category with items
    """
    description = models.TextField(null = True, blank = True)
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = super(OrderFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        
        if include_data:
            order_dict = {'data' : {'description' : self.description} }
            
            categ_list = []
            for menu_categ in self.menu_categories.all().order_by('name'):
                menu_categ_dict = {'category' : {'id': menu_categ.id, 
                                                 'name' : menu_categ.name, 
                                                 'type' : menu_categ.categ_type}
                                   }
                
                item_list = []
                for menu_item in menu_categ.menu_items.all().order_by('-num_orders_prev', 'name'):
                    menu_item_dict = {  'id' : menu_item.id,
                                        'category_id' : menu_item.category_id,
                                        'name' : menu_item.name,
                                        'description' : menu_item.description,
                                        'price' : str(menu_item.price),
                                     }
                    if menu_categ.num_orders_prev > 0:
                        menu_item_dict['usage_rank'] = menu_item.num_orders_prev * 10 / menu_categ.num_orders_prev
                    else:
                        menu_item_dict['usage_rank'] = 0
                    
                    item_list.append(menu_item_dict)
                
                menu_categ_dict['items'] = item_list
                
                categ_list.append(menu_categ_dict)
            
            order_dict['data']['order_menu'] = categ_list             
            serialized_feature.update(order_dict)
        
        return serialized_feature


        
class MenuCategory(models.Model):
    TYPE_CHOICES = (
        ("food", "food"), 
        ("drinks", "drinks"),
        ("desert", "desert")
    )
    
    menu = models.ForeignKey(OrderFeature, related_name = "menu_categories")
    name = models.CharField(max_length = 256)
    categ_type = models.CharField(max_length = 32, choices = TYPE_CHOICES, default = "drinks")
    
    ## the following fields are for consumption based ranking
    num_orders_current = models.IntegerField(default = 0)
    num_orders_prev = models.IntegerField(default = 0)
    
    def __unicode__(self):
        return self.name + " in menu -> " + self.menu.description
    
    
class MenuItem(models.Model):
    category = models.ForeignKey(MenuCategory, related_name = "menu_items")
    name = models.CharField(max_length = 256)
    description = models.TextField(null = True, blank = True)
    price = models.FloatField()
    
    ## the following fields are for consumption based ranking
    num_orders_current = models.IntegerField(default = 0)
    num_orders_prev = models.IntegerField(default = 0)
    
    def __unicode__(self):
        return self.name + " in category -> " + self.category.name


####################################### Social Media Feature #############################################
class SocialMediaFeature(Feature):
    facebook_url = models.URLField(null = True, blank = True, max_length = 256)
    twitter_url = models.URLField(null = True, blank = True, max_length = 256)
    internal_forum_url = models.URLField(null = True, blank = True, max_length = 256)
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = super(SocialMediaFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        
        if include_data:
            data_dict = {}
            
            if self.facebook_url:
                data_dict['facebook_url'] = self.facebook_url
                
            if self.twitter_url:
                data_dict['twitter_url'] = self.twitter_url
            
            if self.internal_forum_url:
                data_dict['internal_forum_url'] = self.internal_forum_url
            
            serialized_feature.update( {'data' : data_dict} )
        
        return serialized_feature
    
    
    def get_feature_data(self, virtual, filters):
        return self.to_serializable(virtual = virtual, include_data = True)['data']