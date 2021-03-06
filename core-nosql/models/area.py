from core.models.dbobject import DBObject
from core.models.dbobject import AREA_DOMAIN, ANNOUNCEMENT_DOMAIN, ANNOTATION_DOMAIN
from core.models.dbcollection import DBCollection
from core.models.announcement import TriggerObject
from core.models.annotation import AnnotatedObject
from core.models.utils import IndexableObject
from core.geo.utils import Point2D, get_circle
from core.models.utils import assert_arg_type, assert_arg_value, assert_arg_list_type


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
class Area(DBObject, IndexableObject, TriggerObject, AnnotatedObject):
    
    TYPE_INTEREST = "interest"
    TYPE_NON_INTEREST = "non_interest"
    
    CATEGORY_DEFAULT = 'Default'
    CATEGORY_ORDERING = 'Ordering'
    
    def __init__(self, id, envID, name, type=TYPE_INTEREST, level=0, 
                 shape=None, category=CATEGORY_DEFAULT, data=None, tags=None):
        super(Area, self).__init__(AREA_DOMAIN, id)
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
    
    @staticmethod
    def getObj(**predicate_dict):
        pass