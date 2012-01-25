from coresql.utils.validations import assert_arg_type, assert_arg_list_type

class TagList(object):
    """ Wrapper for a list of tags. """
    
    def __init__(self, tags = None, limit = 100):
        if not tags is None:
            self.setTags(tags)
        else:
            self.tags = []
        self.setLimit(limit)
        if self.isFull():
            raise TypeError('Tag list limit: ' + str(limit))
    
    def isFull(self):
        return len(self.tags) >= self.limit
    
    def addTag(self, tag):
        assert_arg_type(tags, str)
        self.tags.append(tag)
    
    def removeTag(self, tag):
        assert_arg_type(tags, str)
        self.tags.remove(tag)
    
    def getTags(self):
        return self.tags
    
    def setTags(self, tags):
        assert_arg_type(tags, list)
        assert_arg_list_type(tags, str)
        self.tags = tags
    
    def getLimit(self):
        return self.limit
    
    def setLimit(self, limit):
        assert_arg_type(limit, int)
        self.limit = limit