from models.dbobject import DBObject, AREA_DOMAIN


class Point2D(object):
    
    DB_SEP = ":"
    
    def __init__(self, x, y):
        self._x = x
        self._y = y
    
    def dbEncode(self):
        return str(self._x) + self.DB_SEP + str(self._y)
    
    @staticmethod
    def dbDecode(pointString):
        coord = map(lambda x : float(x), pointString.split("%"))
        return Point2D(*coord)
    
    def __repr__(self):
        return "Point2D(" + str(self._x) + "," + str(self._y) + ")"


class Shape(object):
    
    CIRCLE = "CIRCLE"
    POLYGON = "POLYGON"
    DB_SEP = " "
    
    def __init__(self, type, *args):
        self.type = type
        self.points = args
    
    def getParams(self):
        if (self.type == Shape.POLYGON):
            return self.points
        return _getCircleParams()
    
    def setPoints(self, *points):
        self.points = points
    
    def dbEncode(self):
        encodedPoints = map(lambda x : x.dbEncode(), self.points)
        return self.type + Shape.DB_SEP + Shape.DB_SEP.join(encodedPoints)
    
    @staticmethod
    def dbDecode(shapeString):
        params = shapeString.split(Shape.DB_SEP)
        args = map(lambda x : Point2D.dbDecode(x), params[1:])
        return Shape(params[0], *args)
    
    @staticmethod
    def getCircleParams(self):
        pass
    
    def __repr__(self):
        return "Shape(" + self.type + "," + str(self.points) + ")"


class Area(DBObject):
    
    def __init__(self, **kwargs):
        super(Area, self).__init__(AREA_DOMAIN, **kwargs)
    

    def getName(self):
        return self.name
    
    def setName(self, name):
        self.name = name
    
    
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
    
    def setLeve(self, level):
        self.level = level
    
    
    def getShape(self):
        return Shape.dbDecode(self.shape)
    
    def setShape(self, shape):
        self.shape = shape.dbEncode()
    
    
    def save(self):
        pass
    
    def delete(self):
        pass