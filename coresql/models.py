from django.db import models
#from coresql.db import fields

# Create your models here.

class Environment(models.Model):
    pass


class Layout(models.Model):
    envID = models.ForeignKey(Environment)
    level = models.IntegerField()
    mapURL = models.URLField()


class Area(models.Model):
    envID = models.ForeignKey(Environment)
    areaType = models.CharField(max_length=50)
    name = models.CharField(max_length=100)
    category = models.CharField(max_length=50)
#    data = models.DataField()
#    tags = fields.TagListField()
    layoutID = models.ForeignKey(Layout)
#    shape = fields.AreaShapeField()


class User(models.Model):
    fbID = models.CharField(max_length=50)
    firstName = models.CharField(max_length=30)
    lastName = models.CharField(max_length=30)
    email = models.EmailField(unique = True)


""" TODO: uncomment data and triggers """
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
