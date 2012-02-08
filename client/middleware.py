from django.http import HttpResponsePermanentRedirect, HttpResponseRedirect
from django.conf import settings
from django.core import urlresolvers
import re

class ClientCheckinUrlRedirectMiddleware(object):
    """
    Searches for a client=True key-value token in the request.REQUEST.
    If no such token is found redirects the client to its landing page url
    """
    def __init__(self):
        ## set checkin url regex
        self.checkin_pattern = urlresolvers.reverse("checkin")
        print self.checkin_pattern
    
    def process_request(self, request):
        path = request.get_full_path()
        if re.match(self.checkin_pattern, path):
            if 'clientrequest' in request.REQUEST and request.REQUEST['clientrequest'] == 'true':
                return None
            
            host = "http://" + request.META['HTTP_HOST']
            redirect_url = host + settings.CLIENT_LANDING_PAGE_URL
            
            #return HttpResponsePermanentRedirect(redirect_url)
            return HttpResponseRedirect(redirect_url)
        
        return None