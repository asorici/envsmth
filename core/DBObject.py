class DBObject(object):
    ENV_DOMAIN = "environment"
    AREA_DOMAIN = "area"
    ANNOTATION_DOMAIN = "annotation"
    USER_DOMAIN = "user"
    ANNOUNCEMENT_DOMAIN = "announcement" 
    
    def __init__(self, domain, *args, **kwargs):
        self.exists = False
        pass
    
    
    def set(self, key, value, add_flag=False):
        v = self.__dict__[key]
        if not v:
            self.__dict__[key] = value
        elif add_flag:
            if isinstance(v, list):
                self.__dict__[key] = v.append(value)
            else:
                self.__dict__[key] = [v, value] 
        else:
            self.__dict__[key] = value
                
    def save(self):
        pass
    
    def delete(self):
        pass
    
    @staticmethod
    def getObj(domain, **predicate_dict):
        pass
    
    @staticmethod
    def getObj(domain, *QObj, **predicate_dict):
        pass
    
    @staticmethod
    def deleteObj(domain, **predicate_dict):
        pass
    
    @staticmethod
    def deleteObj(domain, *QObj, **predicate_dict):
        pass
    
                    
            
    
        
        