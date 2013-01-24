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
        if self.user and self.area:
            return str(self.user) + " - " + self.area.name
        elif self.area:
            return "annotation for area " + self.area.name + " but empty user field"
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
        
######################################## DefaultAnnotation Class ##########################################
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
    
    
##################################### PresentationAnnotation Class ########################################
class ProgramAnnotation(Annotation):
    text = models.TextField()
    entry = models.ForeignKey('Entry', related_name = "annotations")
    
    def __init__(self, *args, **kwargs):
        data = kwargs.pop('data', None)
        
        super(ProgramAnnotation, self).__init__(*args, **kwargs)
        
        if not data is None:
            if 'text' in data and 'entry_id' in data:
                self.text = data['text']
                
                entry_id = data['entry_id']
                try:
                    self.entry = Entry.objects.get(id = entry_id)
                except Entry.DoesNotExist:
                    raise AnnotationException("ProgramAnnotation missing valid program entry_id")
            else:
                raise AnnotationException("ProgramAnnotation missing text or entry data")
    
    
    def get_annotation_data(self):
        return { 'text' : self.text }
    
    @classmethod
    def is_annotation_for(cls, category, annotation_data):
        return category == Annotation.PROGRAM
    
    @classmethod
    def get_extra_filters(cls, filters):
        specific_filters = {}
        
        ## just this single case for now
        if "entry_id" in filters:
            try:
                entry = Entry.objects.get(id = filters['entry_id'])
                specific_filters['id__in'] = [ann.id for ann in ProgramAnnotation.objects.filter(entry = entry)]
            except Entry.DoesNotExist:
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
    timestamp = models.DateTimeField(auto_now = True)
    
    
    # use the inheritance manager to get access directly to subclasses of Feature when w
    # retrieving sets of Features
    objects = InheritanceManager()
    
    class Meta:
        unique_together = (("environment", "category"), ("area", "category"))
    
    
    def to_serializable(self, virtual = False, include_data = False):
        serialized_feature = {'category' : self.category, 'version' : self.version, 'timestamp': self.timestamp}
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
    

####################################### Default Feature Class #############################################
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


###################################### Program Feature Classes ############################################
class ProgramFeature(Feature):
    QUERY_TYPES = ('entry', 'search')
    
    description = models.TextField(null = True, blank = True)
    
    def to_serializable(self, virtual = False, include_data = False):
        from client.api import EnvironmentResource, AreaResource
        
        serialized_feature = super(ProgramFeature, self).to_serializable(virtual=virtual, include_data=include_data)
        
        if include_data:
            program_dict = {'data' : {'description' : self.description} }
            
            sessions_list = []
            entries_list = []
                
            sessions = self.sessions.all()
            for s in sessions:
                session_dict = {'id' : s.id,
                                'title' : s.title,
                                'tag' : s.tag,
                               }
                if self.environment:
                    session_dict['location'] = EnvironmentResource().get_resource_uri(self.environment)
                elif self.area:
                    ## by default we return the data of the area
                    session_dict['location'] = AreaResource().get_resource_uri(self.area)
                
                sessions_list.append(session_dict)
                
                entries = s.entries.all().order_by('startTime')
                for e in entries:
                    entries_list.append({'id' : e.id,
                                        'title' : e.title,
                                        'speakers' : e.speakers,
                                        'sessionId' : s.id,
                                        'startTime' : e.startTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                        'endTime' : e.endTime.strftime("%Y-%m-%dT%H:%M:%S")})
            
            program_dict['data']['program'] = { 'description' : self.description, 
                                                'sessions' : sessions_list, 
                                                'entries' : entries_list
                                               }
           
            serialized_feature.update(program_dict)
        
        return serialized_feature
    
    
    def get_feature_data(self, virtual, filters):
        if 'querytype' in filters: 
            if filters['querytype'] in self.QUERY_TYPES:
                if filters['querytype'] == 'entry':
                    entry_id = filters.get('entry_id')
                    if not entry_id:
                        return None
                    
                    entry = Entry.objects.get(id = entry_id)
                    entry_dict = {  'id' : entry.id,
                                    'title' : entry.title,
                                    'speakers' : entry.speakers,
                                    'sessionId' : entry.session.id,
                                    'sessionTitle' : entry.session.title,
                                    'startTime' : entry.startTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                    'endTime' : entry.endTime.strftime("%Y-%m-%dT%H:%M:%S"),
                                    'abstract' : "Abstract not available."
                                 }
                    
                    if entry.abstract:
                        entry_dict['abstract'] = entry.abstract
                        
                    return entry_dict
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
    
    def __unicode__(self):
        return self.title

class Entry(models.Model):
    session = models.ForeignKey(Session, related_name = "entries")
    #speakers = models.ManyToManyField(UserProfile)
    speakers = models.CharField(max_length = 256)
    title = models.CharField(max_length = 256)
    startTime = models.DateTimeField()
    endTime = models.DateTimeField()
    abstract = models.TextField(null = True, blank = True)
    
    def __unicode__(self):
        return self.title + " >> " + self.session.title

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