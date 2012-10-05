from django.conf.urls.defaults import patterns, include, url
from django.views.generic.simple import direct_to_template


# Uncomment the next two lines to enable the admin:
from django.contrib import admin
from django.conf import settings
admin.autodiscover()


## before adding the patterns let's start our two c2dm helper threads
def setup_gcm_logger():
    import logging 
    
    logger = logging.getLogger("c2dm")
    logger.setLevel(logging.INFO)
    
    fh = logging.FileHandler("c2dm_requests.log")
    fh.setLevel(logging.INFO)
    formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    fh.setFormatter(formatter)
    logger.addHandler(fh)
    
def get_gcm_queue():
    from Queue import Queue
    return Queue(1024)

gcm_queue = get_gcm_queue()
setup_gcm_logger()

import c2dm
c2dm_server_thread = c2dm.GCMServerThread(gcm_queue)
c2dm_server_thread.start()

c2dm_client_thread = c2dm.GCMClientThread(gcm_queue)
#c2dm_client_thread.c2dm_login()
c2dm_client_thread.start()



urlpatterns = patterns('',
    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    url(r'^admin/', include(admin.site.urls)),
    url(r'^envsocial/media/(?P<path>.*)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT}),
    
    #(r'^envsocial/resources/client/', include(v1_api.urls)),
    (r'^envsocial/client/', include('envsocial.client.urls')),
    (r'^envsocial/test/$', direct_to_template, {'template': 'test_requests.html'}),
    
    ## connecting with facebook for registration, login, connection
    (r'^facebook/', include('django_facebook.urls')),
    
    ## normal (email, password based) registration, login, activation, password change
    (r'^accounts/', include('frontend.registration_urls')),
    
)
