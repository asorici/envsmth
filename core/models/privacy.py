from core.models.utils import assert_arg_type, assert_arg_value

class Privacy(DBObject):
    
    RELATION_PUBLIC = "public"
    RELATION_PRIVATE = "private"
    
    def __init__(self, userID, envID, relation):
        self.setUserID(userID)
        self.setEnvID(envID)
        self.setRelation(relation)
    
    def getUserID(self):
        return self.userID
    
    def setUserID(self, userID):
        self.userID = userID
    
    def getEnvID(self):
        return self.envID
    
    def setEnvID(self, envID):
        self.envID = envID
            
    def getRelation(self):
        return self.relation
    
    def setRelation(self, relation):
        assert_arg_type(relation, str)
        assert_arg_value(relation, self.RELATION_PUBLIC, self.RELATION_PRIVATE)
        self.relation = relation
