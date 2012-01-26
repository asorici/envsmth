from django.conf.urls.defaults import patterns, include, url
from coresql import views

# Uncomment the next two lines to enable the admin:
from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'envsocial.views.home', name='home'),
    # url(r'^envsocial/', include('envsocial.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    url(r'^admin/', include(admin.site.urls)),
)

urlpatterns += patterns('',
    url('r^envsocial/resources/environment/(\d+)/$', views.handleEnvironmentRequest, name="handle-env"),
    url('r^envsocial/resources/area/(\d+)/$', views.handleAreaRequest, name="handle-area"),
    url('r^envsocial/resources/area/(\d+)/$', views.handleAreaRequest, name="handle-area"),
)
