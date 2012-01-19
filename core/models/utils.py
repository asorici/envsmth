def assert_arg_type(givenObj, requiredType):
    if not isinstance(givenObj, requiredType):
        raise TypeError('Given ' + str(givenObj.__class__) + ' argument, expected ' + str(requiredType))

def assert_arg_value(givenArg, *values):
    if givenArg not in values:
        raise ValueError("Value '" + str(givenArg) + "' is invalid. Possible values are " + str(values))