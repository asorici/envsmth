from core.models.utils import assert_arg_type, assert_arg_value
from core.models.utils import DataField
from core.models.area import Area
from core.models.dbobject import AREA_DOMAIN

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
        self._updateAreaLevel(level)
    
    def addArea(self, area):
        assert_arg_type(area, Area)
        area.setLevel(self._level)
        self._areaCollection.addObj(area)
    
    def removeArea(self, area):
        assert_arg_type(area, Area)
        self._areaCollection.removeObj(area)
    
    def getLevel(self):
        return self._level
    
    def setLevel(self, level):
        assert_arg_type(level, int)
        self._level = level
        self._updateAreaLevel(level)
    
    def _updateAreaLevel(self, level):
        for a in self._areaCollection:
            a.setLevel(level)


class Environment(DBObject, IndexableObject, TriggerObject, AnnotatedObject):
    
    CATEGORY_DEFAULT = 'Default'
    CATEGORY_ORDERING = 'Ordering'
    
    def __init__(self, ownerID, name, category=self.CATEGORY_DEFAULT,
            data=None, tags=None, parentID=None, geoLocation=None):
        pass
    
    
    def getOwnerID(self):
        return self._ownerID
    
    def setOwnerID(self, owner):
        self._ownerID = owner

    
    def getCategory(self):
        return self._category

    def setCategory(self, cat):
        assert_arg_type(cat, str)
        assert_arg_value(cat, self.CATEGORY_DEFAULT, self.CATEGORY_ORDERING)
        self._category = cat
