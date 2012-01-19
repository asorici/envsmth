from models.dbobject import DBObject, ANNOUNCEMENT_DOMAIN

"""
TODO: add validations
"""

class Announcement(DBObject):
    
    REPEAT_EVERY_DAY = 'day'
    REPEAT_EVERY_WEEK = 'week'
    
    def __init__(self, **kwargs):
        super(Announcement, self).__init__(ANNOUNCEMENT_DOMAIN, **kwargs)
    
    def getDescription(self):
        return self.description
    
    def setDescription(self, desc):
        assert_arg_type(desc, str)
        self.description = desc
    
    def getStartDate(self):
        return self.startDate
    
    def setStartDate(self, startDate):
        self.startDate = startDate
    
    def getRepeatEvery(self):
        return self.repeatEvery
    
    def setRepeatEvery(self, repeatEvery):
        assert_arg_type(repeatEvery, str)
        self.repeatEvery = repeatEvery
    
    def getTimestamp(self):
        return self.repeatEvery
