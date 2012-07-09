from coresql.db import fields
from django.contrib.auth.models import User
from django.db import models
from django.db.models.fields.related import SingleRelatedObjectDescriptor
from django.db.models.signals import post_save
from django_facebook.models import FacebookProfileModel
from model_utils.managers import InheritanceManager


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
    
    timestamp = models.DateTimeField(auto_now = True)
    is_anonymous = models.BooleanField(default = False)
    c2dm_id = models.CharField(max_length=256, null = True, blank = True)
    
    def __unicode__(self):
        return self.user.username + ": anonymous=" + str(self.is_anonymous)


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
    def get_subclass_list():
        subclasses = [o for o in dir(UserSubProfile)
                      if isinstance(getattr(UserSubProfile, o), SingleRelatedObjectDescriptor)\
                      and issubclass(getattr(UserSubProfile, o).related.model, UserSubProfile)]
        
        return subclasses
    
    def profile_name(self):
        return self._meta.object_name.lower()
    
    def to_serializable(self):
        return None
    

class ResearchProfile(UserSubProfile):
    affiliation = models.CharField(max_length = 256, null = True, blank = True)
    research_interests = fields.TagListField(null = True, blank = True)
    
    def __unicode__(self):
        return self.affiliation + " ## " + str(self.research_interests.getList())
    
    def to_serializable(self):
        profile_name = self.profile_name()
        data = { profile_name : {'affilitation' : self.affiliation,
                                 'research_interests' : self.research_interests
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
    
    triggers = fields.DateTimeListField()
    timestamp = models.DateTimeField(auto_now = True)


"""
#################################### Annotation Model Classes #############################################
"""
class Annotation(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "annotations")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "annotations")
    user = models.ForeignKey(UserProfile, null = True, blank = True, on_delete=models.SET_NULL)
    #data = fields.DataField()
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES, default="default")
    timestamp = models.DateTimeField(auto_now = True)
    
    # use the inheritance manager to get access directly to subclasses of Annotation when 
    # retrieving sets of annotations
    objects = InheritanceManager()
    
    def __unicode__(self):
        if self.user and self.area:
            return str(self.user) + " - " + self.area.name
        elif self.area:
            return "annotation for area " + self.area.name + " but empty user field"
        else:
            return "empty annotation object"
        
######################################## DefaultAnnotation Class ##########################################
class DescriptionAnnotation(Annotation):
    text = models.TextField()
    
##################################### PresentationAnnotation Class ########################################
class EntryAnnotation(Annotation):
    text = models.TextField()
    entry = models.ForeignKey('Entry', related_name = "annotations")
    

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


"""
####################################### Feature Model Classes ###########################################
"""

class Feature(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "features")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "features")
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    #data = fields.DataField(null = True, blank = True)
    timestamp = models.DateTimeField(auto_now = True)
    
    # use the inheritance manager to get access directly to subclasses of Feature when w
    # retrieving sets of Features
    objects = InheritanceManager()
    
    class Meta:
        unique_together = (("environment", "category"), ("area", "category"))
    
    
    def to_serializable(self):
        data = {'category' : self.category, 'data': None}
        return data
    
    def __unicode__(self):
        if self.area:
            return "feature type(" + self.category + ") for area(" + self.area.name + ")"
        elif self.environment:
            return "feature type(" + self.category + ") for env(" + self.environment.name + ")"
        else:
            return "feature type(" + self.category + ") but no location assigned -- needs fix"
    
    def get_feature_data(self, filters):
        return None

####################################### Default Feature Class #############################################
class DescriptionFeature(Feature):
    description = models.TextField(null = True, blank = True)
    
    def to_serializable(self):
        data = super(DescriptionFeature, self).to_serializable()
        data.update( {'data' : self.description} )
        
        return data
    
    def get_feature_data(self, filters):
        return self.description

###################################### Program Feature Classes ############################################
class ProgramFeature(Feature):
    QUERY_TYPES = ('entry', 'search')
    
    description = models.TextField(null = True, blank = True)
    
    def to_serializable(self):
        from client.api import EnvironmentResource, AreaResource
        
        data = super(ProgramFeature, self).to_serializable()
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
        data.update(program_dict)
        
        return data
    
    def get_feature_data(self, filters):
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
            return self.to_serializable()['data']
        
    
    
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
    
    def to_serializable(self):
        data = super(PeopleFeature, self).to_serializable()
        data.update( {'data' : self.description} )
        
        return data

