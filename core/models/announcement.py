from models.dbobject import DBObject, ANNOUNCEMENT_DOMAIN
from models.utils import DateField, assert_arg_type, assert_arg_value
from time import time


class Announcement(DBObject):
    
    REPEAT_EVERY_DAY = 'day'
    REPEAT_EVERY_WEEK = 'week'
    
    def __init__(self, data, startDate, repeat=None, timestamp=None):
        super(Announcement, self).__init__(ANNOUNCEMENT_DOMAIN)
        self.setData(data)
        self.setStartDate(startDate)
        self.setRepeatEvery(repeatEvery)
        if timestamp is None:
            self.setTimestamp(int(time()))
        else:
            self.setTimestamp(timestamp)
    
    def getData(self):
        return self.data
    
    def setData(self, data):
        assert_arg_type(data, DBField)
        self.data = str(data.dbEncode())
    
    def getStartDate(self):
        return DateField.dbDecode(self.startDate)
    
    def setStartDate(self, startDate):
        assert_arg_type(data, DateField)
        self.startDate = startDate.dbEncode()
    
    def getRepeatEvery(self):
        return self.repeatEvery
    
    def setRepeatEvery(self, repeatEvery):
        if repeatEvery is None:
            self.repeatEvery = None
        else:
            assert_arg_type(repeatEvery, str)
            assert_arg_value(category, self.REPEAT_EVERY_DAY, self.REPEAT_EVERY_WEEK)
            self.repeatEvery = repeatEvery
    
    def getTimestamp(self):
        return self.timestamp
    
    def setTimestamp(self, timestamp):
        assert_arg_type(data, int)
        self.timestamp = timestamp
