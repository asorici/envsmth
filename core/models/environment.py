from utils import assert_arg_type, assert_arg_value, assert_arg_list_type
from utils import DBField
from area import Area
from dbobject import AREA_DOMAIN, DBObject
from dbcollection import DBCollection

class EnvLayout(object):
    
    def __init__(self, mapURL, areaCollection, level):
        self.setMap(mapURL)
        self.setAreaCollection(areaCollection)
        self.setLevel(level)
        
    def getMap(self):
        return self._mapURL
    
    def setMap(self, mapURL):
        self._mapURL = mapURL
    
    def getAreaCollection(self):
        return self._areaCollection
    
    def setAreaCollection(self, areac):
        assert_arg_type(areac, DBCollection)
        assert_arg_value(areac.domain, AREA_DOMAIN)
        self._areaCollection = areac
    
    def addArea(self, area):
        assert_arg_type(area, Area)
        self._areaCollection.addObj(area)
    
    def removeArea(self, area):
        assert_arg_type(area, Area)
        self._areaCollection.removeObj(area)
    
    def getLevel(self):
        return self._level
    
    def setLevel(self, level):
        assert_arg_type(level, int)
        self._level = level


class Environment(DBObject):
    
    CATEGORY_DEFAULT = 'Default'
    CATEGORY_ORDERING = 'Ordering'
    
    def __init__(self, ownerID, name, category=CATEGORY_DEFAULT,
            data=None, tags=None, parentID=None, geoLocation=None):
        pass
    
    def getOwnerID(self):
        return self._ownerID
    
    def setOwnerID(self, owner):
        self._ownerID = owner
    
    def getName(self):
        return self._name
    
    def setName(self, name):
        self._name = str(name)
    
    def getCategory(self):
        return self._category

    def setCategory(self, cat):
        assert_arg_type(cat, str)
        assert_arg_value(cat, self.CATEGORY_DEFAULT, self.CATEGORY_ORDERING)
        self._category = cat
    
    def getData(self):
        return self._data
    
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