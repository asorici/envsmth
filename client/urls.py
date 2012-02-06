from django.conf.urls.defaults import patterns, include
from tastypie.api import Api
from client.api import EnvironmentResource, AreaResource, AnnotationResource,\
                        AnnouncementResource, HistoryResource

v1_api = Api(api_name='v1')
v1_api.register(EnvironmentResource())
v1_api.register(AreaResource())
v1_api.register(AnnotationResource())
v1_api.register(AnnouncementResource())
v1_api.register(HistoryResource())

urlpatterns = patterns('',
    (r'^client/', include(v1_api.urls)),
)