from models.dbobject import DBObject, AREA_DOMAIN
from models.announcement import Announcement
from geo.utils import Point2D, get_circle
from utils import assert_arg_type

class AreaShape(object):
    
    CIRCLE = "CIRCLE"
    POLYGON = "POLYGON"
    DB_SEP = " "
    
    def __init__(self, type, *args):
        self.type = type
        self.points = args
    
    def getParams(self):
        if (self.type == AreaShape.POLYGON):
            return self.points
        return get_circle(*self.points)
    
    def setPoints(self, *points):
        self.points = points
    
    def dbEncode(self):
        encodedPoints = map(lambda x : x.dbEncode(), self.points)
        return self.type + AreaShape.DB_SEP + AreaShape.DB_SEP.join(encodedPoints)
    
    @staticmethod
    def dbDecode(shapeString):
        params = shapeString.split(AreaShape.DB_SEP)
        args = map(lambda x : Point2D.dbDecode(x), params[1:])
        return AreaShape(params[0], *args)
    
    def __repr__(self):
        return "AreaShape(" + self.type + "," + str(self.points) + ")"


class Area(DBObject):
    
    INTEREST = "interest"
    NON_INTEREST = "non_interest"
    
    def __init__(self, **kwargs):
        super(Area, self).__init__(AREA_DOMAIN, **kwargs)
    
    
    def getType(self):
        return self.type
    
    def setType(self, type):
        self.type = type


    def getName(self):
        return self.name
    
    def setName(self, name):
        self.name = name
    
    
    def getCategory(self):
        return self.description
    
    def setCategory(self, category):
        self.category = category
    
    
    def getDescription(self):
        return self.description
    
    def setDescription(self, desc):
        self.description = desc
    
    
    def getTags(self):
        return self.tags
    
    def setTags(self, tags):
        self.tags = tags
    
    
    def getLevel(self):
        return self.level
    
    def setLevel(self, level):
        self.level = level
    
    
    def getShape(self):
        return AreaShape.dbDecode(self.shape)
    
    def setShape(self, shape):
        self.shape = shape.dbEncode()
    
    
    def getAnnouncements(self):
        return self.announcements
    
    def addAnnouncement(self, ann):
        assert_arg_type(ann, Announcement)
        self.announcements.addObj(ann)
        
    
    def getAnnotations(self):
        return self.annotations
    
    def setAnnotations(self, annotations):
        self.annotations = annotations
    
    
    def save(self):
        pass
    
    def delete(self):
        pass