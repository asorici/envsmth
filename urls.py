from django.conf.urls.defaults import patterns, include, url
from django.views.generic.simple import direct_to_template
#from coresql import views

from tastypie.api import Api
from client.api import EnvironmentResource, AreaResource, AnnotationResource,\
                        AnnouncementResource, HistoryResource

v1_api = Api(api_name='v1')
v1_api.register(EnvironmentResource())
v1_api.register(AreaResource())
v1_api.register(AnnotationResource())
v1_api.register(AnnouncementResource())
v1_api.register(HistoryResource())


# Uncomment the next two lines to enable the admin:
from django.contrib import admin
from django.conf import settings
admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'envsocial.views.home', name='home'),
    # url(r'^envsocial/', include('envsocial.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    url(r'^admin/', include(admin.site.urls)),
    url(r'^envsocial/media/(?P<path>.*)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT}),
    
    #(r'^envsocial/resources/client/', include(v1_api.urls)),
    (r'^envsocial/client/', include('envsocial.client.urls')),
    (r'^envsocial/test/$', direct_to_template, {'template': 'test_requests.html'})
)
