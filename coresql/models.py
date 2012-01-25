from django.db import models
#from coresql.db import fields

# Create your models here.

class Area(models.Model):
    pass


class Environment(models.Model):
    pass


""" TODO: de-comment data and triggers """
class Announcement(models.Model):
    areaID = models.ForeignKey(Area)
    envID = models.ForeignKey(Environment)
#    data = models.DataField()
    repeatEvery = models.CharField(max_length=50)
#    triggers = fields.DateTimeListField()
    timestamp = models.DateTimeField(auto_now = True)