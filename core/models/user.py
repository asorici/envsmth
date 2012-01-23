from core.models.utils import assert_arg_type

class User(DBObject):
    
    def __init__(self, firstName, lastName, email, fbId=None):
        self.setName(fistName, lastName)
        sef.setEmail(email)
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