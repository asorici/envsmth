from coresql.utils.validations import assert_arg_type, assert_arg_list_type

class TagList(object):
    """ Wrapper for a list of tags. """
    
    def __init__(self, tags = None, limit = 100):
        if not list is None:
            assert_arg_type(tags, list)
            assert_arg_list_type(tags, str)
        if isinstance(limit, int):
            self.limit = limit
        if (len(tags) > limit):
            raise TypeError('Tag list limit: ' + str(limit))