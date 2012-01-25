from coresql.utils.validations import assert_arg_type, assert_arg_list_type


class ListWrapper(object):
    
    def __init__(self, argList = None, limit = 100, elemType = str):
        if not argList is None:
            self.setList(argList)
        else:
            self.argList = []
        self.setLimit(limit)
        if self.isFull():
            raise TypeError('Argument list limit: ' + str(limit))
        self.elemType = elemType
    
    def isFull(self):
        return len(self.argList) >= self.limit
    
    def addItem(self, item):
        assert_arg_type(item, self.elemType)
        self.argList.append(item)
    
    def removeItem(self, item):
        assert_arg_type(item, self.elemType)
        self.argList.remove(item)
    
    def getList(self):
        return self.argList
    
    def setList(self, argList):
        assert_arg_type(argList, list)
        assert_arg_list_type(argList, self.elemType)
        self.argList = argList
    
    def getLimit(self):
        return self.limit
    
    def setLimit(self, limit):
        assert_arg_type(limit, int)
        self.limit = limit


class TagList(object):
    """ Wrapper for a list of tags. """
    
    def __init__(self, tags = None, limit = 100):
        super(TagList, self).__init__(tags, limit)


from datetime import datetime

class DateTimeList(ListWrapper):
    """ Wrapper for a list of datetime objects. """
    
    def __init__(self, triggers = None, limit = 10):
        super(DateTimeList, self).__init__(triggers, limit, datetime)


class Data(object):
    
    def __init__(self, data, encode_func=str):
        self.data = data
        self.encode_func = encode_func
    
    def dbEncode(self):
        return str(self.encode_func(self.data))
    
    @staticmethod
    def dbDecode(dataString, decode_func=None):
        assert_arg_type(dataString, str)
        if decode_func is None:
            return dataString
        else:
            return decode_func(dataString)
    
    def __repr__(self):
        return str(self.data)