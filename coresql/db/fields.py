import string
from django.db import models
from coresql.db.objects import TagList

class TagField(models.TextField):
    
    description = "A semicolon separated list of tags stored as a string"
    
    __metaclass__ = models.SubfieldBase
    
    def __init__(self, separator = ";", *args, **kwargs):
        self.separator = separator
        super(TagField, self).__init__(*args, **kwargs)
        
    def to_python(self, value):
        # the object case
        if isinstance(value, TagList):
            return value
        
        # the string case which also matches the database case since we subclass TextField
        tag_list = string.split(value, TagField.separator)
        return TagList(tags = tag_list)
    
    def get_prep_value(self, value):
        # value is an object of type TagList
        return self.separator.join(value.getTags())
    
    """
    def get_prep_lookup(self, lookup_type, value):
        # We only handle 'exact' and 'in'. All others are errors.
        if lookup_type == 'exact':
            return self.get_prep_value(value)
        elif lookup_type == 'in':
            return [self.get_prep_value(v) for v in value]
        else:
            raise TypeError('Lookup type %r not supported.' % lookup_type)
    """

 
class DataField(models.TextField):
    