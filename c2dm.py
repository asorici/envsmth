import logging
import pickle, threading, SocketServer
from Queue import Full, Empty


logger = logging.getLogger("c2dm")

class GCMRequestEnqeueing(SocketServer.BaseRequestHandler):
    def __init__(self, request, client_address, server):
        SocketServer.BaseRequestHandler.__init__(self, request, client_address, server)
        
    def handle(self):
        ## unpickle the request of type (registration_id, annotation_uri)
        self.data = pickle.loads(self.request.recv(1024).strip())
        
        try:
            self.server.gcm_request_queue.put(self.data, block=False)
            logger.info("[GCMReqEnqueue] enqueued the following data: " + str(self.data))
            self.request.sendall("OK") ## signal OK
        except Full:
            logger.error("[GCMReqEnqueue] Sorry: queue is full, this request has to be dropped: " + str(self.data))
            self.request.sendall("NOK")
        
    
class GCMServer(SocketServer.TCPServer):
    HOST = 'localhost'
    PORT = 9999
    
    def __init__(self, gcm_request_queue):
        self.gcm_request_queue = gcm_request_queue
        SocketServer.TCPServer.__init__(self, (GCMServer.HOST, GCMServer.PORT), GCMRequestEnqeueing)


class GCMServerThread(threading.Thread):
    """
    This thread will be accepting postings(annotation notifications that have to be sent to mobile devices)
    from the main django threads which handle the annotation requests.
    It accepts a connection that tries to put the notification in a queue. If the queue is full the
    notification will be discarded - this is as to protect the queue from getting to large if the thread consuming
    the queue can not operate
    """
    def __init__(self, gcm_request_queue):
        self.server = GCMServer(gcm_request_queue) 
        threading.Thread.__init__(self)
                
    def run(self):
        self.server.serve_forever()
        
    def stop(self):
        self.server.shutdown()
        self.join()
        

class GCMClientThread(threading.Thread):
    """
    This thread will consume the queue and do the actual transmission of the requests
    """
    def __init__(self, gcm_request_queue, c2dm_auth_token = None):
        self.gcm_request_queue = gcm_request_queue
        self.c2dm_auth_token = c2dm_auth_token
        self.running = True
        threading.Thread.__init__(self)
    
    def run(self):
        while self.running:
            ## try to read from the queue
            try:
                notification = self.gcm_request_queue.get(timeout=15)
                self.gcm_request(notification)
            except Empty:
                ## check if still running and if so wait some more
                pass
            
    def stop(self):
        self.running = False
        
        
    def gcm_request(self, notification, max_retries = 10):
        from gcm.gcm import GCM, GCMConnectionException, GCMUnavailableException, GCMAuthenticationException
        from settings import GOOGLE_GCM_API_KEY 
        from coresql.models import UserProfile
        
        logger.info("[GCMClientThread] Handling request for notification: " + str(notification))
        
        gcm_client = GCM(GOOGLE_GCM_API_KEY)
        registration_ids, collapse_key, delay_while_idle, ttl, data = notification
        
        try:
            response = gcm_client.json_request(registration_ids, data = data,
                                           collapse_key = collapse_key, 
                                           delay_while_idle = delay_while_idle,
                                           time_to_live = ttl,
                                           retries = max_retries)
            
            # Handling errors
            if 'errors' in response:
                for error, reg_ids in response['errors'].items():
                    # Check for errors and act accordingly
                    if error in ['NotRegistered', 'InvalidRegistration']:
                        # Remove reg_ids from database
                        for reg_id in reg_ids:
                            """
                            Either the owner has turned off notifications (NotRegistered) or 
                            there is a problem with the registration_id (InvalidRegistration).
                            In both cases, log the error and set the corresponding registration_id to null in the UserProfile
                            """
                            try:
                                user_profile = UserProfile.objects.get(c2dm_id = reg_id)
                                user_profile.c2dm_id = None
                                user_profile.save()
                            except Exception:
                                pass
                            
                            logger.error("[GCM " + error + " ERROR] Encountered at notification request: " 
                                         + str(notification))
                    else:
                        """
                        Any other error that should normally not be encountered because of the internal business logic: 
                        MissingRegistration, MismatchSenderId, MessageTooBig, MissingCollapseKey 
                        """
                        logger.error("[GCM " + error + " ERROR] Encountered at notification request: " 
                                     + str(notification))
                
            if 'canonical' in response:
                for canonical_id, reg_id in response['canonical'].items():
                    # Repace reg_id with canonical_id in your database
                    try:
                        user_profile = UserProfile.objects.get(c2dm_id=reg_id)
                        user_profile.c2dm_id = canonical_id
                        user_profile.save()
                    except Exception:
                        pass
            
            logger.info("[GCM INFO] Finished processing GCM notifications: " + str(notification))
            
        except GCMConnectionException, e:
            logger.critical("[GCM EXCEPTION] Connection error at GCM request (" + str(notification) + "): " + str(e))
        except GCMAuthenticationException, e:
            logger.critical("[GCM EXCEPTION] Authentication error at GCM request (" + str(notification) + "): " + str(e) 
                            + ". Revise SENDER_ID !!!")
        except GCMUnavailableException, e:
            logger.error("[GCM EXCEPTION] GCM Server Unaivailable even after 10 retries " + 
                         "during request (" + str(notification) + "): " + str(e))
        except Exception, e:
            logger.error("[GCM EXCEPTION] Error at GCM request (" + str(notification) + "): " + str(e))
        
