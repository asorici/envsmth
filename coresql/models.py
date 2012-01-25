from django.db import models
#from coresql.db import fields

# Create your models here.

class User(models.Model):
    fbID = models.CharField(max_length=50)
    firstName = models.CharField(max_length=30)
    lastName = models.CharField(max_length=30)
    email = models.EmailField(unique = True)
    timestamp = models.DateTimeField(auto_now = True)


class Environment(models.Model):
    ownerID = models.ForeignKey(User)
    name = models.CharField(max_length=140)
    category = models.CharField(max_length=50)
#    data = models.DataField()
    parentID = models.IntegerField()
#    tags = fields.TagListField()
    width = models.IntegerField()
    height = models.IntegerField()
    latitude = models.FloatField()
    longitude = models.FloatField()
    timestamp = models.DateTimeField(auto_now = True)


class Layout(models.Model):
    envID = models.ForeignKey(Environment)
    level = models.IntegerField()
    mapURL = models.URLField()
    timestamp = models.DateTimeField(auto_now = True)


class Area(models.Model):
    envID = models.ForeignKey(Environment)
    areaType = models.CharField(max_length=50)
    name = models.CharField(max_length=140)
    category = models.CharField(max_length=50)
#    data = models.DataField()
#    tags = fields.TagListField()
    layoutID = models.ForeignKey(Layout)
#    shape = fields.AreaShapeField()
    timestamp = models.DateTimeField(auto_now = True)


class Announcement(models.Model):
    areaID = models.ForeignKey(Area)
    envID = models.ForeignKey(Environment)
#    data = models.DataField()
    repeatEvery = models.CharField(max_length=50)
#    triggers = fields.DateTimeListField()
    timestamp = models.DateTimeField(auto_now = True)


class Annotation(models.Model):
    areaID = models.ForeignKey(Area)
    envID = models.ForeignKey(Environment)
    userID = models.ForeignKey(User)
#    data = models.DataField()
    timestamp = models.DateTimeField(auto_now = True)
