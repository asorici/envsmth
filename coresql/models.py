from django.db import models
from coresql.db import fields
from django.contrib.auth.models import User as DjangoUser

# Create your models here.

#class User(DjangoUser):
class User(models.Model):
    user = models.OneToOneField(DjangoUser)
    
    fbID = models.CharField(max_length=50)
    timestamp = models.DateTimeField(auto_now = True)
    is_anonymous = models.BooleanField()


class Environment(models.Model):
    CATEGORY_CHOICES = (
        ("default", "default"), 
        ("ordering", "ordering")
    )
    
    owner = models.ForeignKey(User)
    name = models.CharField(max_length=140)
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    data = fields.DataField()
    parentID = models.IntegerField(null = True, blank = True)
    tags = fields.TagListField(null = True, blank = True)
    width = models.IntegerField(null = True, blank = True)
    height = models.IntegerField(null = True, blank = True)
    latitude = models.FloatField(null = True, blank = True)
    longitude = models.FloatField(null = True, blank = True)
    timestamp = models.DateTimeField(auto_now = True)


class Layout(models.Model):
    env = models.ForeignKey(Environment, related_name = "layouts")
    level = models.IntegerField()
    mapURL = models.URLField()
    timestamp = models.DateTimeField(auto_now = True)


class Area(models.Model):
    CATEGORY_CHOICES = (
        ("default", "default"), 
        ("ordering", "ordering")
    )
    
    TYPE_CHOICES = (
        ("interest", "Interest"), 
        ("non-interest", "Non-Interest")
    )
    
    env = models.ForeignKey(Environment, related_name = "areas")
    areaType = models.CharField(max_length=50, choices = TYPE_CHOICES)
    name = models.CharField(max_length=140)
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    
    data = fields.DataField(null = True, blank = True)
    tags = fields.TagListField(null = True, blank = True)
    layout = models.ForeignKey(Layout, related_name = "areas", blank = True)
    
    shape = fields.AreaShapeField(blank = True)
    timestamp = models.DateTimeField(auto_now = True)


class Announcement(models.Model):
    REPEAT_EVERY_CHOICES = (
        ("none", "None"), 
        ("day", "Day"), 
        ("week", "Week")
    )
    
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "announcements")
    env = models.ForeignKey(Environment, null = True, blank = True, related_name = "announcements")
    
    data = fields.DataField()
    repeatEvery = models.CharField(max_length=50, choices = REPEAT_EVERY_CHOICES, default = "None")
    
    triggers = fields.DateTimeListField()
    timestamp = models.DateTimeField(auto_now = True)


class Annotation(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "annotations")
    env = models.ForeignKey(Environment, null = True, blank = True, related_name = "annotations")
    user = models.ForeignKey(User, null = True, blank = True, on_delete=models.SET_NULL)
    data = fields.DataField()
    timestamp = models.DateTimeField(auto_now = True)


class History(models.Model):
    user = models.ForeignKey(User)
    area = models.ForeignKey(Area)
    env = models.ForeignKey(Environment)
    timestamp = models.DateTimeField(auto_now = True)


class Privacy(models.Model):
    user = models.ForeignKey(User)
    env = models.ForeignKey(Environment)
    relation = models.CharField(max_length=50)
    

class UserContext(models.Model):
    user = models.ForeignKey(User)
    currentEnv = models.ForeignKey(Environment, null=True, blank = True)
    currentArea = models.ForeignKey(Area, null=True, blank = True)
    
