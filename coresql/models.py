from django.db import models
from coresql.db import fields
from django.contrib.auth.models import User
from django.db.models.signals import post_save


#class User(DjangoUser):
class UserProfile(models.Model):
    user = models.OneToOneField(User)
    
    fbID = models.CharField(max_length=50)
    timestamp = models.DateTimeField(auto_now = True)
    is_anonymous = models.BooleanField(default = False)
    c2dm_id = models.CharField(max_length=256, null = True, blank = True)

    def __unicode__(self):
        return self.user.username + ": anonymous=" + str(self.is_anonymous)

    @models.permalink
    def get_absolute_url(self):
        return ('handle-user', [str(self.user.id)])


def create_user_profile(sender, instance, created, **kwargs):
    if created:
        UserProfile.objects.create(user=instance)

post_save.connect(create_user_profile, sender=User)
    

class Environment(models.Model):
    CATEGORY_CHOICES = (
        ("default", "default"), 
        ("ordering", "ordering")
    )
    
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

    @models.permalink
    def get_absolute_url(self):
        return ('dispatch-env', (), {
            'id': str(self.id)})
            

class Layout(models.Model):
    environment = models.ForeignKey(Environment, related_name = "layouts")
    level = models.IntegerField(default = 0)
    mapURL = models.URLField()
    timestamp = models.DateTimeField(auto_now = True)


class Area(models.Model):
    CATEGORY_CHOICES = (
        ("default", "default"), 
        ("ordering", "ordering")
    )
    
    TYPE_CHOICES = (
        ("interest", "interest"), 
        ("non-interest", "non-interest")
    )
    
    environment = models.ForeignKey(Environment, related_name = "areas")
    areaType = models.CharField(max_length=50, choices = TYPE_CHOICES)
    name = models.CharField(max_length=140)
    
    #category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    #data = fields.DataField(null = True, blank = True)
    
    tags = fields.TagListField(null = True, blank = True)
    layout = models.ForeignKey(Layout, related_name = "areas", blank = True)
    
    shape = fields.AreaShapeField(blank = True, null = True)
    timestamp = models.DateTimeField(auto_now = True)

    @models.permalink
    def get_absolute_url(self):
        return ('dispatch-area', (), {
            'areaID': str(self.id)})


class Feature(models.Model):
    CATEGORY_CHOICES = (
        ("default", "default"), 
        ("ordering", "ordering")
    )
    
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "features")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "features")
    category = models.CharField(max_length=50, choices = CATEGORY_CHOICES)
    data = fields.DataField(null = True, blank = True)



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
    
    @models.permalink
    def get_absolute_url(self):
        return ('dispatch-announcement', (), {
            'announceID': str(self.id)})



class Annotation(models.Model):
    area = models.ForeignKey(Area, null = True, blank = True, related_name = "annotations")
    environment = models.ForeignKey(Environment, null = True, blank = True, related_name = "annotations")
    user = models.ForeignKey(UserProfile, null = True, blank = True, on_delete=models.SET_NULL)
    data = fields.DataField()
    timestamp = models.DateTimeField(auto_now = True)
    
    @models.permalink
    def get_absolute_url(self):
        return ('dispatch-annotation', (), {
            'annID': str(self.id)})
    
    
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
    
    @models.permalink
    def get_absolute_url(self):
        return ('handle-history', [str(self.user.id)])



class Privacy(models.Model):
    user = models.ForeignKey(UserProfile)
    environment = models.ForeignKey(Environment)
    relation = models.CharField(max_length=50)
    

class UserContext(models.Model):
    user = models.OneToOneField(UserProfile, related_name='context')
    currentEnvironment = models.ForeignKey(Environment, null = True, blank = True)
    currentArea = models.ForeignKey(Area, null = True, blank = True)
    
