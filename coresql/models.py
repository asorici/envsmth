from django.db import models
from coresql.db import fields
from django.contrib.auth.models import User
from django.db.models.signals import post_save
from django_facebook.models import FacebookProfileModel

CATEGORY_CHOICES = (
    ("default", "default"), 
    ("order", "order"),
    ("program", "program")

)


#class UserProfile(models.Model):
class UserProfile(FacebookProfileModel):
    user = models.OneToOneField(User)
    
    #facebook_id = models.BigIntegerField(blank=True, unique=True, null=True)
    timestamp = models.DateTimeField(auto_now = True)
    is_anonymous = models.BooleanField(default = False)
    c2dm_id = models.CharField(max_length=256, null = True, blank = True)

    def __unicode__(self):
        return self.user.username + ": anonymous=" + str(self.is_anonymous)


def create_user_profile(sender, instance, created, **kwargs):
    if created:
        UserProfile.objects.create(user=instance)

post_save.connect(create_user_profile, sender=User)
    

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
    mapURL = models.URLField()
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


class Feature(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "features")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "features")
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    data = fields.DataField(null = True, blank = True)
    timestamp = models.DateTimeField(auto_now = True)


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



class Annotation(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "annotations")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "annotations")
    user = models.ForeignKey(UserProfile, null = True, blank = True, on_delete=models.SET_NULL)
    data = fields.DataField()
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES, default="default")
    timestamp = models.DateTimeField(auto_now = True)
    
    def __unicode__(self):
        if self.user and self.area:
            return str(self.user) + " - " + self.area.name
        elif self.area:
            return "annotation for area " + self.area.name + " but empty user field"
        else:
            return "empty annotation object"

    
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
    
