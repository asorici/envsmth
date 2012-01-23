from core.models.dbobject import DBObject
from core.models.dbobject import ANNOUNCEMENT_DOMAIN
from core.models.utils import assert_arg_type, assert_arg_value, assert_arg_list_type
from datetime import datetime
from time import time


class Announcement(DBObject):
    
    REPEAT_EVERY_DAY = 'day'
    REPEAT_EVERY_WEEK = 'week'
    
    def __init__(self, data, dateTimeList, repeat=None):
        super(Announcement, self).__init__(ANNOUNCEMENT_DOMAIN)
        self.setData(data)
        self.setDateTimeTriggers(dateTimeList)
        self.setRepeatEvery(repeat)
        self.setTimestamp(int(time()))
    
    def getData(self):
        return self.data
    
    def setData(self, data):
        if isinstance(data, str):
            self.data = data
        else:
            assert_arg_type(data, DataField)
            self.data = str(data.dbEncode())
    
    def getDateTimeTriggers(self):
        dateTimeList = map(lambda x : datetime.strptime(x, "%Y-%m-%d %H:%M:%S"), self.dateTimeTriggers)
        return dateTimeList
    
    def setDateTimeTriggers(self, dateTimeList):
        if not isinstance(dateTimeList, list) or len(dateTimeList) == 0:
            raise TypeError("A list with at least 1 datetime object is expected.")
        assert_arg_list_type(dateTimeList, datetime)
        self.dateTimeTriggers = map(lambda x : x.strftime("%Y-%m-%d %H:%M:%S"), dateTimeList)
    
    def addDateTime(self, dateTime):
        assert_arg_type(dateTime, datetime)
        self.dateTimeTriggers.append(dateTime.strftime("%Y-%m-%d %H:%M:%S"))
    
    def getRepeatEvery(self):
        return self.repeatEvery
    
    def setRepeatEvery(self, repeatEvery):
        if repeatEvery is None:
            self.repeatEvery = None
        else:
            assert_arg_type(repeatEvery, str)
            assert_arg_value(repeatEvery, self.REPEAT_EVERY_DAY, self.REPEAT_EVERY_WEEK)
            self.repeatEvery = repeatEvery
    
    def getTimestamp(self):
        return self.timestamp
    
    def setTimestamp(self, timestamp):
        assert_arg_type(timestamp, int)
        self.timestamp = timestamp


class TriggerObject(object):

    def getAnnouncements(self):
        return self.announcements
    
    def addAnnouncement(self, ann):
        assert_arg_type(ann, Announcement)
        self.announcements.addObj(ann)
