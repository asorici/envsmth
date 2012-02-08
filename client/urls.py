from django.conf.urls.defaults import patterns, include, url
from tastypie.api import Api
from client.api import EnvironmentResource, AreaResource, AnnotationResource,\
                        AnnouncementResource, HistoryResource
from client.views import checkin, checkout

v1_api = Api(api_name='v1')
v1_api.register(EnvironmentResource())
v1_api.register(AreaResource())
v1_api.register(AnnotationResource())
v1_api.register(AnnouncementResource())
v1_api.register(HistoryResource())

urlpatterns = patterns('',
    url(r'^checkin/$', checkin, name="checkin"),
    url(r'^checkout/$', checkout, name="checkout"),
    (r'^resources/', include(v1_api.urls)),
)