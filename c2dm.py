import logging
import pickle, threading, SocketServer
from Queue import Full, Empty


logger = logging.getLogger("c2dm")

class C2DMRequestEnqeueing(SocketServer.BaseRequestHandler):
    def __init__(self, request, client_address, server):
        SocketServer.BaseRequestHandler.__init__(self, request, client_address, server)
        
    def handle(self):
        ## unpickle the request of type (registration_id, annotation_uri)
        self.data = pickle.loads(self.request.recv(1024).strip())
        
        try:
            self.server.c2dm_request_queue.put(self.data, block=False)
            logger.info("[C2DMReqEnqueue] enqueued the following data: " + str(self.data))
            self.request.sendall("OK") ## signal OK
        except Full:
            logger.error("[C2DMReqEnqueue] Sorry: queue is full, this request has to be dropped: " + str(self.data))
            self.request.sendall("NOK")
        
    
class C2DMServer(SocketServer.TCPServer):
    HOST = 'localhost'
    PORT = 9999
    
    def __init__(self, c2dm_request_queue):
        self.c2dm_request_queue = c2dm_request_queue
        SocketServer.TCPServer.__init__(self, (C2DMServer.HOST, C2DMServer.PORT), C2DMRequestEnqeueing)


class C2DMServerThread(threading.Thread):
    """
    This thread will be accepting postings(annotation notifications that have to be sent to mobile devices)
    from the main django threads which handle the annotation requests.
    It accepts a connection that tries to put the notification in a queue. If the queue is full the
    notification will be discarded - this is as to protect the queue from getting to large if the thread consuming
    the queue can not operate
    """
    def __init__(self, c2dm_request_queue):
        self.server = C2DMServer(c2dm_request_queue) 
        threading.Thread.__init__(self)
                
    def run(self):
        self.server.serve_forever()
        
    def stop(self):
        self.server.shutdown()
        self.join()
        

class C2DMClientThread(threading.Thread):
    """
    This thread will consume the queue and do the actual transmission of the requests
    """
    def __init__(self, c2dm_request_queue, c2dm_auth_token = None):
        self.c2dm_request_queue = c2dm_request_queue
        self.c2dm_auth_token = c2dm_auth_token
        self.running = True
        threading.Thread.__init__(self)
    
    def update_auth_token(self, c2dm_auth_token):
        self.c2dm_auth_token = c2dm_auth_token
        
    def run(self):
        while self.running:
            ## try to read from the queue
            try:
                notification = self.c2dm_request_queue.get(timeout=15)
                self.c2dm_request(notification, 0)
            except Empty:
                ## check if still running and if so wait some more
                pass
            
    def stop(self):
        self.running = False
        
        
    def c2dm_request(self, notification, trial_ct):
        #from gdata.client import GDClient
        #from gdata.auth import ClientLoginToken
        
        import urllib, urllib2
        from urllib2 import HTTPError
        
        if trial_ct < 10:
            """
            Add params and headers
            """
            logger.info("[C2DMClientThread] Handling request for notification: " + str(notification) + " :: trial_ct = " + str(trial_ct))
            
            registration_id, collapse_key, location_uri, resource_uri, feature = notification
            params = {'registration_id': registration_id, 'collapse_key': collapse_key, 
                      'data.location_uri': location_uri,
                      'data.resource_uri': resource_uri,
                      'data.feature': feature
                      }
            
            credentials = 'GoogleLogin auth=' + self.c2dm_auth_token
            headers = { 'Authorization' : credentials }
            data = urllib.urlencode(params)
            
            """
            Make request
            """
            try:
                r = urllib2.Request('https://android.apis.google.com/c2dm/send', data, headers)
                f = urllib2.urlopen(r)
                content = f.read()
                
                self._handle_content(content, notification, trial_ct)
            except HTTPError as e:
                if e.code == 401:
                    ## get new authentication token and try again
                    self.c2dm_login()
                    self.c2dm_request(notification, trial_ct + 1)
                elif e.code == 503:
                    self._handle503(e, notification, trial_ct)
                else:
                    ## some other unexpected code - log it
                    logger.critical("[C2DM HTTP ERROR] Encountered at c2dm request (" + str(notification) + "): " + str(e.code) + " - " + e.read())
            except Exception, e:
                logger.critical("[C2DM EXCEPTION] Encountered at c2dm request (" + str(notification) + "): " + str(e))
            
        else:
            logger.critical("[C2DM TIMEOUT ERROR] Tried to recover 10 times from failure sending notification: " + str(notification) + ". Aborting!")
    
    
    def c2dm_login(self):
        from gdata.client import GDClient
        
        email = 'aqua.envsocial@gmail.com'
        password = '3nvsocial'
        application_name = 'dev-test-v1'
        
        client = GDClient()
        try:
            client.client_login(email=email, password=password, source=application_name, 
                            service="ac2dm", account_type="GOOGLE", captcha_token=None, captcha_response=None)
        except Exception, ex:
            logger.critical("[C2DM LOGIN ERROR] Error while logging into GData service: " + str(ex))
            
        
        if client.auth_token:
            #print "client token string:", client.auth_token.token_string
            logger.info("client.auth_token is: " + str(client.auth_token.token_string))
            self.update_auth_token(client.auth_token.token_string)
       
    
    def _handle_content(self, content, notification, trial_ct):
        import time
        
        content = content.strip()
        ## parse the simple content string
        response_bits = content.split("=")
        
        if response_bits[0] == "id":
            ## if we got a message id it means the transmission was successful
            logger.info("[C2DM SUCCESS] Successful transmission of notification (" + str(notification) + "). Response: " + content)
        
        elif response_bits[0].lower() == "error":
            ## an error occurred, let's see if we can handle it
            if response_bits[1] == "QuotaExceeded" or response_bits[1] == "DeviceQuotaExceeded":
                """
                We're in trouble here. We won't block the thread until the next day, but instead log the message
                as critical. 
                """
                logger.critical("[C2DM " + response_bits[1] + " ERROR] Encountered at notification request: " + str(notification))
                time.sleep(30)
                
            elif response_bits[1] == "InvalidRegistration" or response_bits[1] == "NotRegistered":
                """
                Either the owner has turned off notifications (NotRegistered) or 
                there is a problem with the registration_id (InvalidRegistration).
                In both cases, log the error and set the corresponding registration_id to null in the UserProfile
                """
                from coresql.models import UserProfile
                
                registration_id = notification[0]
                try:
                    user_profile = UserProfile.objects.get(c2dm_id = registration_id)
                    user_profile.c2dm_id = None
                    user_profile.save()
                except Exception:
                    pass
                
                logger.error("[C2DM " + response_bits[1] + " ERROR] Encountered at notification request: " + str(notification))
            else:
                """
                Any other error that should normally not be encountered because of the internal business logic: 
                MissingRegistration, MismatchSenderId, MessageTooBig, MissingCollapseKey 
                """
                logger.error("[C2DM " + response_bits[1] + " ERROR] Encountered at notification request: " + str(notification))
    
        
    def _handle503(self, e, notification, trial_ct):
        import random, time
        import email.utils as eut
        
        ## first try and see if there is a Retry-After header
        header_info = e.info()
        retry_after_header = header_info.get("Retry-After")
        
        perform_retry_after = False
        
        if not retry_after_header is None:
            ## see if it's a number
            retry_after = None
            try:
                retry_after = float(retry_after_header)
            except ValueError:
                pass
            
            if retry_after:
                perform_retry_after = True
                time.sleep(int(retry_after) + 1)
            else:
                t_tuple = None
                try:
                    t_tuple = eut.parsedate_tz(retry_after_header)
                except Exception:
                    pass
                
                if t_tuple:
                    t = time.mktime(t_tuple[:9])
                    t += t_tuple[9]                     ## this should be seconds since epoch in UTC timezone
                    
                    now = time.mktime(time.gmtime())    ## this should be current time ins seoconds since epoch UTC
                    t_diff = t - now
                    if t_diff > 0 and t_diff < 100:    ## if a valid time diff results - perform retry-after
                        perform_retry_after = True
                        time.sleep(t_diff)
                    
        
        if not perform_retry_after:    
            ## if not, do exponential backoff
            wait_time = random.randint(0, 2**trial_ct)
            wait_time = wait_time / 5
            if wait_time < 1:
                wait_time = 1
            
            time.sleep(wait_time)
        
        self.c2dm_request(notification, trial_ct + 1)