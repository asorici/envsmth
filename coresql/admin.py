from django.contrib import admin
from coresql.models import UserProfile, Environment, Layout, Area, Announcement, Annotation, History,\
    Privacy, Feature, Entry, Session

admin.site.register([UserProfile, Environment, Layout, Area, Announcement, Annotation, History, Privacy, Feature, Entry, Session])