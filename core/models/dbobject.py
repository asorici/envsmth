ENV_DOMAIN = "environment"
AREA_DOMAIN = "area"
ANNOTATION_DOMAIN = "annotation"
USER_DOMAIN = "user"
ANNOUNCEMENT_DOMAIN = "announcement"

from core.db.query_builder import CassandraQuery

class DBObject(object):
    
    def __init__(self, domain, **kwargs):
        self.__dict__ = kwargs
        self.exists = False
        self.domain = domain
    
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
                
    def save(self):
        ins_query = CassandraQuery(self.domain, CassandraQuery.OP_INSERT, self)
        ins_query.execute_query()
    
    def delete(self):
        del_query = CassandraQuery(self.domain, CassandraQuery.OP_DELETE, self)
        del_query.execute_query()
    
    @staticmethod
    def getObj(domain, qObj=None, **predicate_dict):
        query = CassandraQuery(domain, CassandraQuery.OP_SELECT)
        query.add_filter_object(qObj)
        query.add_filter_statements(predicate_dict)
        
        return query.execute_query()
    
    
    @staticmethod
    def deleteObj(domain, qObj=None, **predicate_dict):
        query = CassandraQuery(domain, CassandraQuery.OP_DELETE)
        query.add_filter_object(qObj)
        query.add_filter_statements(predicate_dict)
        
        pass
    

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



