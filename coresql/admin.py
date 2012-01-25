from django.contrib import admin
from coresql.models import User, Environment, Layout, Area, Announcement, Annotation, History, Privacy

admin.site.register([User, Environment, Layout, Area, Announcement, Annotation, History, Privacy])