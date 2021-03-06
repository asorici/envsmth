ENV_DOMAIN = "Environment"
AREA_DOMAIN = "Area"
ANNOTATION_DOMAIN = "Annotation"
EVENT_DOMAIN = "Event"
USER_DOMAIN = "User"
ANNOUNCEMENT_DOMAIN = "Announcement"
HISTORY_DOMAIN = "History"
CONFIG_DOMAIN = "Config"
PRIVACY_DOMAIN = "Privacy"
LAYOUT_DOMAIN = "Layout"

from core.db.query_builder import CassandraQuery


class DBObject(object):
    
    def __init__(self, domain, id, **kwargs):
        self.__dict__.update(kwargs)
        self.exists = False
        self.domain = domain
        self.id = id
    
    def set(self, key, value, add_flag=False):
        v = self.__dict__[key]
        if not v:
            self.__dict__[key] = value
        elif add_flag:
            if isinstance(v, list):
                self.__dict__[key] = v.append(value)
            else:
                self.__dict__[key] = [v, value] 
        else:
            self.__dict__[key] = value
    
    def setExists(self, exists):
        self.exists = exists
    
    def exists(self):
        return self.exists
    
    """
    must be overridden in each class sub-classing DBObject
    default function assumes a field named "id" exists within the created object
    if no such field is found None is returned
    """
    def get_pk(self):
        if self.__dict__["id"]:
            return self.__dict__["id"]
        return None
                
    def save(self):
        ins_query = CassandraQuery(self.domain, CassandraQuery.OP_INSERT, self)
        ins_query.execute_query()
    
    def delete(self, cascade = True):
        del_query = CassandraQuery(self.domain, CassandraQuery.OP_DELETE, self, cascade = cascade)
        del_query.execute_query()
    
    @staticmethod
    def getObj(domain, row_key):
        query = CassandraQuery(domain, CassandraQuery.OP_SELECT)
        query.set_fetch_limits(row_key, 1)
        
        return query.execute_query()
    
    @staticmethod
    def filterObj(domain, qObj=None, **predicate_dict):
        query = CassandraQuery(domain, CassandraQuery.OP_SELECT)
        query.add_filter_object(qObj)
        query.add_filter_statements(predicate_dict)
        query.set_fetch_limits(0, 1)
        
        return query.execute_query()
    
    
    def __eq__(self, other):
        try:
            return self.id == other.id
        except AttributeError:
            return id(self) == id(other)

    def __hash__(self):
        try:
            return self.id
        except AttributeError:
            return id(self)

    def __repr__(self):
        return str(self.__dict__)

