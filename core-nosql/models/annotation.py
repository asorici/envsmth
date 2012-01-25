from core.models.dbobject import DBObject, ANNOTATION_DOMAIN
from core.models.utils import assert_arg_type, DataField
from time import time


class Annotation(DBObject):
    
    def __init__(self, id, locID, userID, data, isEnvAnn=False):
        super(Annotation, self).__init__(ANNOTATION_DOMAIN, id)
        if isEnvAnn:
            self.setEnvID(locID)
        else:
            self.setAreaID(locID)
        self.setUserID(userID)
        self.setData(data)
        self.setTimestamp(int(time()))
    
    
    def getEnvID(self):
        return self.envID
    
    def setEnvID(self, id):
        self.envID = id
    
        
    def getAreaID(self):
        return self.areaID
    
    def setAreaID(self, id):
        self.areaID = id
    
    
    def getUserID(self):
        return self.userID
    
    def setUserID(self, userID):
        self.userID = userID
    
    
    def getData(self):
        return self.data
    
    def setData(self, data):
        if isinstance(data, str):
            self.data = data
        else:
            assert_arg_type(data, DataField)
            self.data = str(data.dbEncode())
    
    
    def getTimestamp(self):
        return self.timestamp
    
    def setTimestamp(self, timestamp):
        assert_arg_type(timestamp, int)
        self.timestamp = timestamp


class AnnotatedObject(object):
    
    def getAnnotations(self):
        return self.annotations
    
    def addAnnotation(self, ann):
        assert_arg_type(ann, Annotation)
        self.annotations.addObj(ann)