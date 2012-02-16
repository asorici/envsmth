from django.conf.urls.defaults import patterns, include, url
from django.views.generic.simple import direct_to_template
#from coresql import views


# Uncomment the next two lines to enable the admin:
from django.contrib import admin
from django.conf import settings
admin.autodiscover()

## before adding the patterns let's start our two c2dm helper threads
import c2dm

c2dm_server_thread = c2dm.C2DMServerThread()
c2dm_server_thread.start()


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
