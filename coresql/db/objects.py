from coresql.utils.validations import assert_arg_type, assert_arg_list_type, assert_arg_value


class ListWrapper(object):
    
    def __init__(self, argList = None, limit = 100, elemType = basestring):
        self.elemType = elemType
        
        if not argList is None:
            self.setList(argList)
        else:
            self.argList = []
        self.setLimit(limit)
        if self.isFull():
            raise TypeError('Argument list limit: ' + str(limit))
    
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
    
    
    def to_serializable(self):
        """
        should return a basic type value (int, float, boolean, string, tuple, list, dict, datetime) that can be
        easily serialized. The default is to return the contained arglist 
        """
        return self.argList
    
    
    def __unicode__(self):
        return " ; ".join(self.argList)
    


class TagList(ListWrapper):
    """ Wrapper for a list of tags. """
    
    def __init__(self, tags = None, limit = 100):
        super(TagList, self).__init__(argList = tags, limit = limit)
        

from datetime import datetime

class DateTimeList(ListWrapper):
    """ Wrapper for a list of datetime objects. """
    
    def __init__(self, triggers = None, limit = 10):
        super(DateTimeList, self).__init__(argList = triggers, limit = limit, elemType = datetime)


def dbencode(value):
    from django.utils import simplejson
    
    ## if value is just a string, save it ``as is`` in the database
    if isinstance(value, basestring):
        return value 
    
    return simplejson.dumps(value, ensure_ascii=True)


class Data(object):
    TEXT = "text"
    XML = "xml"
    JSON = "json"
    
    def __init__(self, data, encode_func=dbencode, data_format = JSON):
        self.data = data
        self.encode_func = encode_func
        self.data_format = data_format
    
    def dbEncode(self):
        #return str(self.encode_func(self.data))
        return self.encode_func(self.data)
    
    @staticmethod
    def dbDecode(dataString):
        assert_arg_type(dataString, basestring)
        
        ## this logic is poor - I believe the best way is to define a standard set of 
        ## representation formats and use try..except clauses to parse the incoming string
        ## according to the defined formats (formats: text, json, xml)
        ## For now we treat it as a default of text
        
        ## use tastypie's serializer to do the testing 
        from tastypie.serializers import Serializer
        ## instantiate a serializer to test
        serdes = Serializer()
        
        dataobj = None
        val = None
        try:
            val = serdes.from_xml(dataString)
        except Exception:
            val = None
        
        if val is None:
            try:
                from django.utils import simplejson
                #val = serdes.from_json(dataString)
                val = simplejson.loads(dataString, encoding='utf-8')
            except Exception, ex:
                val = None
            
            if not isinstance(val, (dict, list)):
                val = str(dataString)
                dataobj = Data(val, data_format = Data.TEXT)
            else:
                dataobj = Data(val, data_format = Data.JSON)
        else:
            dataobj = Data(val, data_format = Data.XML)
        
        ## then clean up after myself
        del serdes
        ## and return new data object
        return dataobj
        
    
    def to_serializable(self):
        ## we can just return data here since we try to ensure it is a serializable format (string, list, dict)
        ## beforehand
        return self.data

    def __repr__(self):
        return str(self.data)


from coresql.utils.geo import Point2D
from coresql.utils.geo import get_circle

class AreaShape(object):
    
    TYPE_CIRCLE = "CIRCLE"
    TYPE_POLYGON = "POLYGON"
    
    DB_SEP = " "
    
    def __init__(self, areaType = None, *args):
        if areaType:
            assert_arg_value(areaType, self.TYPE_CIRCLE, self.TYPE_POLYGON)
            self.areaType = areaType
            self.setPoints(*args)
        else:
            self.areaType = None
            self.points = []
            
    def getType(self):
        return self.areaType

    def getParams(self):
        if self.areaType:
            if (self.areaType == AreaShape.TYPE_POLYGON):
                return self.points
            return get_circle(*self.points)
        else:
            return None
    
    def setPoints(self, *points):
        #print points
        assert_arg_list_type(points, Point2D)
        if (type == self.TYPE_CIRCLE) and (len(points) != 3):
            raise TypeError(self.TYPE_CIRCLE + ' requires exactly 3 points.')
        elif (type == self.TYPE_POLYGON) and (len(points) < 2):
            raise TypeError(self.TYPE_POLYGON + ' requires at least 2 points.')
        else:
            self.points = points
    
    def dbEncode(self):
        if self.areaType:
            encodedPoints = map(lambda x : x.dbEncode(), self.points)
            return self.areaType + AreaShape.DB_SEP + AreaShape.DB_SEP.join(encodedPoints)
        
        return None
    
    @staticmethod
    def dbDecode(shapeString):
        if shapeString:
            params = shapeString.split(AreaShape.DB_SEP)
            args = map(lambda x : Point2D.dbDecode(x), params[1:])
            return AreaShape(params[0], *args)
        else:
            return AreaShape()
    
    def __repr__(self):
        return "AreaShape(" + self.areaType + "," + str(self.points) + ")"