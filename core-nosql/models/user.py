from core.models.utils import assert_arg_type
from core.models.dbobject import DBObject, USER_DOMAIN

class User(DBObject):
    
    def __init__(self, id, firstName, lastName, email, fbId=None):
        super(User, self).__init__(USER_DOMAIN, id)
        
        self.setName(firstName, lastName)
        self.setEmail(email)
        self.setFbId(fbId)
    
    def getName(self):
        return (self.firstName, self.lastName)
    
    def setName(self, firstName, lastName):
        assert_arg_type(firstName, str)
        assert_arg_type(lastName, str)
        self.firstName = firstName
        self.lastName = lastName
    
    def getEmail(self):
        return self.email
    
    def setEmail(self, email):
        assert_arg_type(email, str)
        self.email = email
    
    def getFbId(self):
        return self.fbId
    
    def setFbId(self, fbId):
        assert_arg_type(fbId, str)
        self.fbId = fbId