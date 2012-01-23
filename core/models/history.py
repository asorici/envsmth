class History(DBObject):
    
    def __init__(self, userID, envID, areaID=None):
        self.setUserID(userID)
#        if areaID is None:
        self.setAreaID(areaID)
#        else:
        self.setEnvID(envID)
    
    def getUserID(self):
        return self.userID
    
    def setUserID(self, userID):
        self.userID = userID
    
    def getEnvID(self):
        return self.envID
    
    def setEnvID(self, envID):
        self.envID = envID
    
    def getAreaID(self):
        return self.areaID
    
    def setAreaID(self, areaID):
        self.areaID = areaID