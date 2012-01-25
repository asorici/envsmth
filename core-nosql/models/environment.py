from core.models.dbobject import DBObject, ENV_DOMAIN, LAYOUT_DOMAIN
from core.models.dbcollection import DBCollection
from core.models.utils import assert_arg_type, assert_arg_value
from core.models.utils import IndexableObject
from core.models.annotation import AnnotatedObject
from core.models.announcement import TriggerObject
from core.models.area import Area
from core.models.dbobject import AREA_DOMAIN
from core.db.query_builder import CassandraQuery

class EnvLayout(DBObject):
    
    def __init__(self, id, envID, mapURL, level, areaCollection = None):
        super(EnvLayout, self).__init__(LAYOUT_DOMAIN, id)
        self.setEnvID(envID)
        self.setMap(mapURL)
        self.setAreaCollefction(areaCollection)
        self.setLevel(level)
        
    def getEnvID(self):
        return self.envID
        
    def setEnvID(self, envID):
        self.envID = envID
    
    def getMap(self):
        return self.mapURL
    
    def setMap(self, mapURL):
        self.mapURL = mapURL
    
    def getAreaCollection(self):
        return self.areaCollection
    
    def setAreaCollection(self, areac):
        assert_arg_type(areac, DBCollection)
        assert_arg_value(areac.domain, AREA_DOMAIN)
        self.areaCollection = areac
        self._updateAreaLevel(self.level)
    
    def addArea(self, area):
        assert_arg_type(area, Area)
        area.setLevel(self.level)
        self.areaCollection.addObj(area)
    
    def removeArea(self, area):
        assert_arg_type(area, Area)
        self.areaCollection.removeObj(area)
    
    def getLevel(self):
        return self.level
    
    def setLevel(self, level):
        assert_arg_type(level, int)
        self.level = level
        self._updateAreaLevel(level)
    
    def _updateAreaLevel(self, level):
        for a in self.areaCollection:
            a.setLevel(level)


class Environment(DBObject, IndexableObject, TriggerObject, AnnotatedObject):
    
    CATEGORY_DEFAULT = 'Default'
    CATEGORY_ORDERING = 'Ordering'
    
    def __init__(self, id, ownerID, name, category=CATEGORY_DEFAULT,
            data=None, tags=None, parentID=None, geoLocation=None):
        super(Environment, self).__init__(ENV_DOMAIN, id)
    
    
    def getOwnerID(self):
        return self.ownerID
    
    def setOwnerID(self, owner):
        self.ownerID = owner

    def getCategory(self):
        return self.category

    def setCategory(self, cat):
        assert_arg_type(cat, str)
        assert_arg_value(cat, self.CATEGORY_DEFAULT, self.CATEGORY_ORDERING)
        self.category = cat

    @staticmethod
    def getObj(domain, row_key):
        query = CassandraQuery(domain, CassandraQuery.OP_SELECT)
        query.set_fetch_limits(row_key, 1)
        
        try:
            ## result is a dict of {column : value}
            result = query.execute_query()
            env = Environment(row_key, result['ownerID'], result['name'], result['category'])
            
            return env
        except Exception, ex:
            print ex
            return None