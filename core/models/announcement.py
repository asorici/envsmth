from models.dbobject import DBObject, ANNOUNCEMENT_DOMAIN

class Announcement(DBObject):
    
    def __init__(self, **kwargs):
        super(Announcement, self).__init__(ANNOUNCEMENT_DOMAIN, **kwargs)
    
    def getDescription(self):
        return self.description
    
    def setDescription(self, desc):
        self.description = desc
    
    def getStartDate(self):
        return self.startDate
    
    def setStartDate(self, startDate):
        self.startDate = startDate
    
    def getRepeatEvery(self):
        return self.repeatEvery
    
    def setRepeatEvery(self, repeatEvery):
        self.repeatEvery = repeatEvery
    
    def getTimestamp(self):
        return self.repeatEvery
