from settings import DATABASES
from pycassa.pool import ConnectionPool
from django.db.utils import DatabaseError

class CassandraConnection(object):
    def __init__(self, host, port, keyspace, username, password, pool_size = 5):
        self.host = host
        self.port = port
        self.keyspace = keyspace
        self.username = username
        self.password = password
        
        self.logged_in = False
        self.pool = None
        
    def commit(self):
        pass
    
    
    def open(self):
        server_list = [self.host + ":" + self.port]
        connected = False
        
        try:
            if user and password:
                credentials = {'username': username, 'password': password}
                self.pool = ConnectionPool(keyspace=self.keyspace, server_list=server_list, credentials=credentials, pool_size=pool_size)
                if self.pool:
                    self.logged_in = True
                    connected = True
            else:
                self.pool = ConnectionPool(keyspace=self.keyspace, server_list=server_list, credentials=credentials, pool_size=pool_size)
                if self.pool:
                    connected = True
                
        except Exception, e:
            pass
        
        if not connected:
            raise DatabaseError('Error connecting to keyspace: %s; %s' % (self.keyspace, str(e)))
                
                
    def close(self):
        if self.pool:
            try:
                self.pool.dispose()
            except Exception, e:
                pass
            
            self.logged_in = False
            self.pool = None
            
    def is_connected(self):
        return self.pool != None
    
    def get_client(self):
        if self.pool == None:
            self.open()
        return self.pool
    
    def reopen(self):
        self.close()
        self.open()

        
def connect_to_db():
    try:
        keyspace = DATABASES['cassandra']['NAME']
        username = DATABASES['cassandra']['USER']
        password = DATABASES['cassandra']['PASSWORD']
        host = DATABASES['cassandra']['HOST']
        port = DATABASES['cassandra']['PORT']
        pool_size = DATABASES['cassandra']['POOL_SIZE']
        
        con = CassandraConnection(host, port, keyspace, username, password, pool_size=pool_size)
        
        return con
    except Exception, e:
        raise DatabaseError('Error connecting to database. Incorrect configuration in settings file: %s' % str(e))
    
db_connection = connect_to_db()        