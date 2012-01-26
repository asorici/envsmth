# Create your views here.

"""
THIS ARE THE CORE APPLICATION VIEW FUNCTIONS WHICH HANDLE THE RESTful SERVING
OF CLIENT SIDE REQUESTS.
THESE VIEW FUNCTIONS ARE THEREFORE MOSTLY LIMITED TO IMPLIED "GET" FUNCTIONALITY.

FEATURE-SPECIFIC CREATING, UPDATING AND DELETING FUNCTIONS WILL BE IMPLEMENTED IN
VIEWS OF CORRESPONDING SUB-APPLICATION FOLDERS.  
"""

from django.utils import simplejson

## ERROR_RESPONSE_LIST
CHECKIN_FAILED = (500, "Checkin failed")
CHECKIN_DENIED = (405, "Checkin denied")
AREA_NOT_FOUND = (404, "Requested Area not found")
ENVIRONMENT_NOT_FOUND = (404, "Requested Environment not found")


def authenticateUser(request):
    pass

    
def logout(request):
    pass

    
def checkInArea(request):
    pass 


def checkout(request):
    pass


def handleEnvironmentRequest(envID, request):
    pass


def handleAreaRequest(areaID, request):
    pass


def handleAnnouncementRequest(envID, request):
    pass


def handleAnnotationRequest(request):
    pass


def handleHistoryRequest(request):
    pass


def handlePrivacyRequest(request):
    pass