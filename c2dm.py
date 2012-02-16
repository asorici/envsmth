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
            logger.error("Sorry: queue is full, this request has to be dropped: " + str(self.data))
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
        
        import urllib, urllib2, random, time
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
                
                logger.info("Response to c2dm request (" + str(resource_uri) + "): " + content)
            except HTTPError as e:
                if e.code == 401:
                    ## get new authentication token and try again
                    self._c2dm_login()
                    self._c2dm_request(notification, trial_ct + 1)
                elif e.code == 503:
                    ## do exponential backoff
                    wait_time = random.randint(0, 2**trial_ct)
                    if wait_time / 10 < 1:
                        wait_time = 1
                    
                    time.sleep(wait_time)
                    self._c2dm_request(notification, trial_ct + 1)
                else:
                    ## some other unexpected code - log it
                    logger.critical("Unexpected http error for c2dm request (" + str(notification) + "): " + str(e.code) + " - " + e.read())
            except Exception, e:
                logger.critical("Unexpected exception for c2dm request (" + str(notification) + "): " + str(e))
            
        else:
            logger.critical("Tried to recover 10 times from failure sending notification: " + str(notification) + ". Aborting!")
    
    
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
            logger.critical("ERROR while logging in to GData service: " + str(ex))
            
        logger.info("client.auth_token is: " + str(client.auth_token))
        print "client.auth_token is: " + str(client.auth_token)
        
        if client.auth_token:
            print "client token string:", client.auth_token.token_string
            self.update_auth_token(client.auth_token.token_string)