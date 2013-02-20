'''
Created on Jul 11, 2012

@author: alex
'''
class AnnotationException(Exception):
    """
    An exception for the creation of new Annotations
    """
    def __init__(self, msg = "", *args, **kwargs):
        super(AnnotationException, self).__init__(*args, **kwargs)
        self.msg = msg
    
    def __str__(self):
        return self.get_message()
    
    def get_message(self):
        return self.msg
    

class DuplicateAnnotationException(AnnotationException):
    def __init__(self, msg = "Annotation already exists.", *args, **kwargs):
        super(DuplicateAnnotationException, self).__init__(*args, **kwargs)
        self.msg = msg