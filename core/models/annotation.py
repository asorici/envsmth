from models.dbobject import DBObject, ANNOTATION_DOMAIN

"""
TODO: add validations
"""

class Annotation(DBObject):
    
    def __init__(self, **kwargs):
        super(Annotation, self).__init__(ANNOTATION_DOMAIN, **kwargs)
    
    def getUserID(self):
        return self.repeatEvery
    
    def getData(self):
        return self.data
    
    def getPostDate(self):
        return self.postDate
