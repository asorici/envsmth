from django.contrib import admin
from coresql.models import UserProfile, Environment, Layout, Area, Announcement,\
    Annotation, ProgramAnnotation, History, Privacy,\
    Feature, DescriptionFeature, BoothDescriptionFeature, BoothProduct,\
    ProgramFeature, Presentation, Session, Speaker, SocialMediaFeature,\
    MenuCategory, MenuItem, UserSubProfile, ResearchProfile

admin.site.register([UserProfile, Environment, Layout, 
                     Area, Announcement, Annotation, ProgramAnnotation, History, 
                     Privacy, Feature, DescriptionFeature, BoothDescriptionFeature, BoothProduct, 
                     ProgramFeature, Presentation, Session, Speaker, SocialMediaFeature,
                     MenuCategory, MenuItem, UserSubProfile, ResearchProfile])