from models.utils import assert_arg_type, assert_arg_value
from models.area import Area
from models.dbobject import AREA_DOMAIN

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
    