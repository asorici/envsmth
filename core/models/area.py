from models.dbobject import DBObject, AREA_DOMAIN
from models.announcement import Announcement
from models.annotation import Annotation
from geo.utils import Point2D, get_circle
from utils import assert_arg_type, assert_arg_value

class AreaShape(object):
    
    SHAPE_CIRCLE = "CIRCLE"
    SHAPE_POLYGON = "POLYGON"
    
    DB_SEP = " "
    
    def __init__(self, type, *args):
        self.type = type
        self.points = args
    
    def getParams(self):
        if (self.type == AreaShape.SHAPE_POLYGON):
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
    
    TYPE_INTEREST = "interest"
    TYPE_NON_INTEREST = "non_interest"
    
    CATEGORY_DEFAULT = 'Default'
    CATEGORY_ORDERING = 'Ordering'
    
    def __init__(self, envID, name, type=TYPE_INTEREST, level=0, 
                 shape=None, category=CATEGORY_DEFAULT, data=None, tags=None):
        super(Area, self).__init__(AREA_DOMAIN)
        # TODO: check envID value
        self.envID = envID
        self.setName(name)
        self.setType(type)
     
#    def __init__(self, **kwargs):
#        super(Area, self).__init__(AREA_DOMAIN, **kwargs)
    
    
    def getType(self):
        return self.type
    
    def setType(self, type):
        assert_arg_type(type, str)
        assert_arg_value(type, self.TYPE_INTEREST, self.TYPE_NON_INTEREST)
        self.type = type


    def getName(self):
        return self.name
    
    def setName(self, name):
        assert_arg_type(name, str)
        self.name = name
    
    
    def getCategory(self):
        return self.description
    
    def setCategory(self, category):
        assert_arg_type(category, str)
        self.category = category
    
    
    def getDescription(self):
        return self.description
    
    def setDescription(self, desc):
        assert_arg_type(desc, str)
        self.description = desc
    
    
    def getTags(self):
        return self.tags
    
    def setTags(self, tags):
        assert_arg_type(tags, list)
        self.tags = tags
    
    
    def getLevel(self):
        return self.level
    
    def setLevel(self, level):
        assert_arg_type(level, int)
        self.level = level
    
    
    def getShape(self):
        return AreaShape.dbDecode(self.shape)
    
    def setShape(self, shape):
        assert_arg_type(shape, AreaShape)
        self.shape = shape.dbEncode()
    
    
    def getAnnouncements(self):
        return self.announcements
    
    def addAnnouncement(self, ann):
        assert_arg_type(ann, Announcement)
        self.announcements.addObj(ann)
        
    
    def getAnnotations(self):
        return self.annotations
    
    def addAnnotation(self, ann):
        assert_arg_type(ann, Annotation)
        self.annotations.addObj(ann)
    
    
    def save(self):
        pass
    
    def delete(self):
        pass