from coresql.utils.validations import assert_arg_type, assert_arg_list_type, assert_arg_value


class ListWrapper(object):
    
    def __init__(self, argList = None, limit = 100, elemType = str):
        if not argList is None:
            self.setList(argList)
        else:
            self.argList = []
        self.setLimit(limit)
        if self.isFull():
            raise TypeError('Argument list limit: ' + str(limit))
        self.elemType = elemType
    
    def isFull(self):
        return len(self.argList) >= self.limit
    
    def addItem(self, item):
        assert_arg_type(item, self.elemType)
        self.argList.append(item)
    
    def removeItem(self, item):
        assert_arg_type(item, self.elemType)
        self.argList.remove(item)
    
    def getList(self):
        return self.argList
    
    def setList(self, argList):
        assert_arg_type(argList, list)
        assert_arg_list_type(argList, self.elemType)
        self.argList = argList
    
    def getLimit(self):
        return self.limit
    
    def setLimit(self, limit):
        assert_arg_type(limit, int)
        self.limit = limit


class TagList(ListWrapper):
    """ Wrapper for a list of tags. """
    
    def __init__(self, tags = None, limit = 100):
        super(TagList, self).__init__(argList = tags, limit = limit)
        

from datetime import datetime

class DateTimeList(ListWrapper):
    """ Wrapper for a list of datetime objects. """
    
    def __init__(self, triggers = None, limit = 10):
        super(DateTimeList, self).__init__(argList = triggers, limit = limit, elemType = datetime)


class Data(object):
    
    def __init__(self, data, encode_func=str):
        self.data = data
        self.encode_func = encode_func
    
    def dbEncode(self):
        return str(self.encode_func(self.data))
    
    @staticmethod
    def dbDecode(dataString, decode_func=None):
        assert_arg_type(dataString, str)
        if decode_func is None:
            return dataString
        else:
            return decode_func(dataString)

    def __repr__(self):
        return str(self.data)


from coresql.utils.geo import Point2D
from coresql.utils.geo import get_circle

class AreaShape(object):
    
    TYPE_CIRCLE = "CIRCLE"
    TYPE_POLYGON = "POLYGON"
    
    DB_SEP = " "
    
    def __init__(self, areaType, *args):
        assert_arg_value(areaType, self.TYPE_CIRCLE, self.TYPE_POLYGON)
        self.areaType = areaType
        self.setPoints(*args)

    def getParams(self):
        if (self.areaType == AreaShape.TYPE_POLYGON):
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
        return self.areaType + AreaShape.DB_SEP + AreaShape.DB_SEP.join(encodedPoints)
    
    @staticmethod
    def dbDecode(shapeString):
        params = shapeString.split(AreaShape.DB_SEP)
        args = map(lambda x : Point2D.dbDecode(x), params[1:])
        return AreaShape(params[0], *args)
    
    def __repr__(self):
        return "AreaShape(" + self.areaType + "," + str(self.points) + ")"