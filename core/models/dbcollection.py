from core.db.query_builder import CassandraQuery

class DBCollection(object):
    
    """
    Build collection as a set.
    """
    def __init__(self, domain, *obj):
        self.domain = domain
        self.objSet = set(obj)
    
    def addObj(self, obj):
        self.objSet.add(obj)
    
    def removeObj(self, obj):
        self.objSet.remove(obj)
    
    def filter(self, predicate_func):
        return filter(predicate_func, self.objSet)
    
    def save(self):
        # TODO: batch operation
        ins_query = CassandraQuery(self.domain, CassandraQuery.OP_BATCH_INSERT, self)
        ins_query.execute_query()
    
    def delete(self, cascade = True):
        # TODO: batch operation 
        del_query = CassandraQuery(self.domain, CassandraQuery.OP_BATCH_DELETE, self, cascade=cascade)
        del_query.execute_query()
    
    
    @staticmethod
    def getCollection(domain, offset = 0, limit = 100, qObj = None, **predicate_dict):
        query = CassandraQuery(domain, CassandraQuery.OP_FETCH)
        query.add_filter_object(qObj)
        query.add_filter_statements(predicate_dict)
        
        return query.execute_query()

    
    def __repr__(self):
        return str(self.objSet)