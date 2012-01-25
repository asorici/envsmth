from django.db import models
from coresql.db import fields

# Create your models here.

class User(models.Model):
    fbID = models.CharField(max_length=50)
    firstName = models.CharField(max_length=50)
    lastName = models.CharField(max_length=50)
    email = models.EmailField(unique = True)
    timestamp = models.DateTimeField(auto_now = True)


class Environment(models.Model):
    CATEGORY_CHOICES = (
        ("default", "Default"), 
        ("ordering", "Ordering")
    )
    
    ownerID = models.ForeignKey(User)
    name = models.CharField(max_length=140)
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    data = fields.DataField()
    parentID = models.IntegerField()
    tags = fields.TagListField(null = True, blank = True)
    width = models.IntegerField()
    height = models.IntegerField()
    latitude = models.FloatField()
    longitude = models.FloatField()
    timestamp = models.DateTimeField(auto_now = True)


class Layout(models.Model):
    envID = models.ForeignKey(Environment, related_name = "layouts")
    level = models.IntegerField()
    mapURL = models.URLField()
    timestamp = models.DateTimeField(auto_now = True)


class Area(models.Model):
    CATEGORY_CHOICES = (
        ("default", "Default"), 
        ("ordering", "Ordering")
    )
    
    TYPE_CHOICES = (
        ("interest", "Interest"), 
        ("non-interest", "Non-Interest")
    )
    
    envID = models.ForeignKey(Environment, related_name = "areas")
    areaType = models.CharField(max_length=50, choices = TYPE_CHOICES)
    name = models.CharField(max_length=140)
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    
    data = fields.DataField(null = True, blank = True)
    tags = fields.TagListField(null = True, blank = True)
    layoutID = models.ForeignKey(Layout, related_name = "areas")
    
    shape = fields.AreaShapeField()
    timestamp = models.DateTimeField(auto_now = True)


class Announcement(models.Model):
    REPEAT_EVERY_CHOICES = (
        ("none", "None"), 
        ("day", "Day"), 
        ("week", "Week")
    )
    
    areaID = models.ForeignKey(Area, null = True, blank = True, related_name = "announcements")
    envID = models.ForeignKey(Environment, null = True, blank = True, related_name = "announcements")
    
    data = fields.DataField()
    repeatEvery = models.CharField(max_length=50, choices = REPEAT_EVERY_CHOICES, default = "None")
    
    triggers = fields.DateTimeListField()
    timestamp = models.DateTimeField(auto_now = True)


class Annotation(models.Model):
    areaID = models.ForeignKey(Area, null = True, blank = True, related_name = "annotations")
    envID = models.ForeignKey(Environment, null = True, blank = True, related_name = "annotations")
    userID = models.ForeignKey(User)
    data = fields.DataField()
    timestamp = models.DateTimeField(auto_now = True)


class History(models.Model):
    userID = models.ForeignKey(User)
    areaID = models.ForeignKey(Area)
    envID = models.ForeignKey(Environment)
    timestamp = models.DateTimeField(auto_now = True)


class Privacy(models.Model):
    userID = models.ForeignKey(User)
    envID = models.ForeignKey(Environment)
    relation = models.CharField(max_length=50)
    
    