from core.models.dbobject import DBObject
from core.models.dbobject import AREA_DOMAIN, ANNOUNCEMENT_DOMAIN, ANNOTATION_DOMAIN
from core.models.dbcollection import DBCollection
from core.models.announcement import Announcement
from core.models.annotation import Annotation
from core.models.utils import DBField, IndexableObject
from core.geo.utils import Point2D, get_circle
from core.models.utils import assert_arg_type, assert_arg_value, assert_arg_list_type

"""
class AreaData(object):
    
    def __init__(self, data, encode_func=str):
        self.data = data
        self.encode_func = encode_func
    
    def dbEncode(self):
        return self.encode_func(self.data)
    
    @staticmethod
    def dbDecode(dataString, decode_func=None):
        assert_arg_type(dataString, str)
        if decode_func is None:
            return AreaData(dataString)
        else:
            return AreaData(decode_func(dataString))

    def __repr__(self):
        return str(self.data)
"""    

class AreaShape(object):
    
    TYPE_CIRCLE = "CIRCLE"
    TYPE_POLYGON = "POLYGON"
    
    DB_SEP = " "
    
    def __init__(self, type, *args):
        assert_arg_value(type, self.TYPE_CIRCLE, self.TYPE_POLYGON)
        self.type = type
        self.setPoints(*args)

    def getParams(self):
        if (self.type == AreaShape.TYPE_POLYGON):
            return self.points
        return get_circle(*self.points)
    
    def setPoints(self, *points):
        print points
        assert_arg_list_type(points, Point2D)
        if (type == self.TYPE_CIRCLE) and (len(points) != 3):
            raise TypeError(self.TYPE_CIRCLE + ' requires exactly 3 points.')
        elif (type == self.TYPE_POLYGON) and (len(points) < 2):
            raise TypeError(self.TYPE_POLYGON + ' requires at least 2 points.')
        else:
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

"""
TODO: add id to DBObject; add domain to DBCollection
"""
class Area(DBObject, IndexableObject):
    
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
        self.setLevel(level)
        self.setShape(shape)
        self.setCategory(category)
        self.setData(data)
        self.setTags(tags)
        self.announcements = DBCollection(ANNOUNCEMENT_DOMAIN)
        self.annotations = DBCollection(ANNOTATION_DOMAIN)
     
#    def __init__(self, **kwargs):
#        super(Area, self).__init__(AREA_DOMAIN, **kwargs)
    """
    def getName(self):
        return self.name
    
    def setName(self, name):
        assert_arg_type(name, str)
        self.name = name
"""

    def getType(self):
        return self.type
    
    def setType(self, type):
        assert_arg_type(type, str)
        assert_arg_value(type, self.TYPE_INTEREST, self.TYPE_NON_INTEREST)
        self.type = type
    
    
    def getLevel(self):
        return int(self.level)
    
    def setLevel(self, level):
        assert_arg_type(level, int)
        # TODO: check max level
        self.level = str(level)
    
    
    def getShape(self):
        return AreaShape.dbDecode(self.shape)
    
    def setShape(self, shape):
        if shape is None:
            self.shape = None
        else:
            assert_arg_type(shape, AreaShape)
            # the encoded data must always be a type 'str'
            self.shape = str(shape.dbEncode())
    
    
    def getCategory(self):
        return self.category
    
    def setCategory(self, category):
        assert_arg_type(category, str)
        assert_arg_value(category, self.CATEGORY_DEFAULT, self.CATEGORY_ORDERING)
        self.category = category
    
    """
    def getData(self):
        return self.data
    
    def setData(self, data):
        if data is None:
            self.data = None
        else:
            assert_arg_type(data, DBField)
            # the encoded data must either be a type 'str' or having a repr
            self.data = str(data.dbEncode())
    
    
    def getTags(self):
        return self.tags
    
    def setTags(self, tags):
        if tags is None:
            self.tags = []
        else:
            assert_arg_type(tags, list)
            assert_arg_list_type(tags, str)
            self.tags = tags
    
    def addTag(self, tag):
        self.tags.append(str(tag))
    """
    
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
    
    @staticmethod
    def getObj(**predicate_dict):
        pass