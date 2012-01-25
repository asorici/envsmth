from coresql.objects.listwrapper import ListWrapper
from coresql.utils.validations import assert_arg_type, assert_arg_list_type

from datetime import datetime


class DateTimeList(ListWrapper):
    """ Wrapper for a list of datetime objects. """
    
    def __init__(self, triggers = None, limit = 10):
        super(DateTimeList, self).__init__(triggers, limit, datetime)