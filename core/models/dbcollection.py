class DBCollection(object):
    
    """
    Build collection as a set.
    """
    def __init__(self, *obj):
        self.objSet = set(obj)
    
    def addObj(self, obj):
        self.objSet.add(obj)
    
    def removeObj(self, obj):
        self.objSet.remove(obj)
    
    def filter(self, predicate_func):
        return filter(predicate_func, self.objSet)
    
    def save(self):
        # TODO: batch operation
        pass
    
    def delete(self):
        # TODO: batch operation 
        pass
    
    @staticmethod
    def getCollection(domain, **predicate_dict):
        pass
    
    @staticmethod
    def getCollection(domain, *QObj, **predicate_dict):
        pass
    
    def __repr__(self):
        return str(self.objSet)