from django.contrib import admin
from coresql.models import UserProfile, Environment, Layout, Area, Announcement, Annotation, History,\
    Privacy, Feature, Entry, Session, UserSubProfile, ResearchProfile

admin.site.register([UserProfile, Environment, Layout, 
                     Area, Announcement, Annotation, History, 
                     Privacy, Feature, Entry, Session, 
                     UserSubProfile, ResearchProfile])