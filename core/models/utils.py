def assert_arg_type(givenObj, requiredType):
    if not isinstance(givenObj, requiredType):
        raise TypeError('Given ' + str(givenObj.__class__) + ' argument, expected ' + str(requiredType))

def assert_arg_list_type(argList, requiredType):
    if not reduce(lambda a,b : a and b, map(lambda e : isinstance(e, requiredType), argList)):
        raise TypeError('Expected list of ' + str(requiredType))

def assert_arg_value(givenArg, *values):
    if givenArg not in values:
        raise ValueError("Value '" + str(givenArg) + "' is invalid. Possible values are " + str(values))

class DataField(object):
    
    def __init__(self, data, encode_func=str):
        self.data = data
        self.encode_func = encode_func
    
    def dbEncode(self):
        return self.encode_func(self.data)
    
    @staticmethod
    def dbDecode(dataString, decode_func=None):
        assert_arg_type(dataString, str)
        if decode_func is None:
            return dataString
        else:
            return decode_func(dataString)

    def __repr__(self):
        return str(self.data)


class IndexableObject(object):
    
    def getName(self):
        return self.name
    
    def setName(self, name):
        assert_arg_type(name, str)
        self.name = name
    
    def getData(self):
        return self.data
    
    def setData(self, data):
        if data is None:
            self.data = None
        else:
            assert_arg_type(data, DataField)
            # the encoded data must either be a type 'str' or having a repr
            self.data = str(data.dbEncode())
    
    def getTags(self):
        return self.tags
    
    def setTags(self, tags):
        if tags is None:
            self.tags = []
        else:
            assert_arg_type(tags, list)
            assert_arg_list_type(tags, str)
            self.tags = tags
    
    def addTag(self, tag):
        self.tags.append(str(tag))
