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
        
    def get_message(self):
        return self.msg