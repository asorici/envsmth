import string
from datetime import datetime
from django.db import models
from coresql.db.objects import TagList, Data, AreaShape, DateTimeList
from django.core.exceptions import ValidationError

class TagListField(models.TextField):
    
    description = "A semicolon separated list of tags stored as a string"
    
    __metaclass__ = models.SubfieldBase
    
    separator = ";"
    
    def __init__(self, *args, **kwargs):
        super(TagListField, self).__init__(*args, **kwargs)
        
    def to_python(self, value):
        # the object case
        if isinstance(value, TagList):
            return value
        
        # the string case which also matches the database case since we subclass TextField
        if not value is None: 
            try:
                tag_list = string.split(value, self.separator)
                return TagList(tags = tag_list)
            except Exception, ex:
                raise ValidationError("Invalid tag list. " + str(ex))
        
        ## the case where the NULL value from the DB is returned as a None python value
        return TagList()
    
    def get_prep_value(self, value):
        # value is an object of type TagList
        return self.separator.join(value.getList())
    
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



class DateTimeListField(models.TextField):
    
    description = "A semicolon separated list of datetime objects stored as a string"
    
    __metaclass__ = models.SubfieldBase
    
    separator = ";"
    
    def to_python(self, value):
        # the object case
        if isinstance(value, DateTimeList):
            return value
        
        # the string case which also matches the database case since we subclass TextField
        if value:
            try:
                datetime_str_list = string.split(value, DateTimeListField.separator)
                trigger_list = map(lambda d: datetime.strptime(d, '%Y-%m-%d %H:%M:%S'), datetime_str_list)
            
                return DateTimeList(triggers = trigger_list)
            except Exception, ex:
                raise ValidationError("Invalid DateTime list. " + str(ex))
            
        ## if an empty string is received
        return DateTimeList()
    
    def get_prep_value(self, value):
        # value is an object of type DateTimeList
        return DateTimeListField.separator.join( map(lambda d: d.strftime("%Y-%m-%d %H:%M:%S"), value.getList()) )


 
class DataField(models.TextField):
    description = "A structured data type encoded as a string"
    
    __metaclass__ = models.SubfieldBase
    
    def __init__(self, *args, **kwargs):
        super(DataField, self).__init__(*args, **kwargs)
    
    
    def to_python(self, value):
        # the object case
        if isinstance(value, Data):
            return value
        
        # the string case which also matches the database case since we subclass TextField
        if not value is None:
            try:
                return Data.dbDecode(value)
            except Exception, ex:
                raise ValidationError("Invalid encoding for data object. " + str(ex))
        else:
            return Data("")
    
    def get_prep_value(self, value):
        # value is an object of type Data
        return value.dbEncode()
    


class AreaShapeField(models.TextField):
    
    description = "A geometrical shape modeled as a list of vertices"
    
    __metaclass__ = models.SubfieldBase
    
    def __init__(self, *args, **kwargs):
        super(AreaShapeField, self).__init__(*args, **kwargs)
        
    def to_python(self, value):
        # the object case
        if isinstance(value, AreaShape):
            return value
        
        # the string case which also matches the database case since we subclass TextField
        if not value is None:
            try:
                return AreaShape.dbDecode(value)
            except Exception, ex:
                raise ValidationError("Invalid encoding for area shape object. " + str(ex))
        else:
            return AreaShape(AreaShape.TYPE_POLYGON)

    def get_prep_value(self, value):
        # value is an object of type AreaShape
        return value.dbEncode()
    
