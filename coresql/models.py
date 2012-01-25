from django.db import models

# Create your models here.

class Area(models.Model):
    pass


class Environment(models.Model):
    pass


class Announcement(models.Model):
    areaID = models.ForeignKey(Area)
    envID = models.ForeignKey(Environment)
    description = models.TextField()
    repeatEvery = models.CharField(max_length=50)
