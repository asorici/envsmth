def assert_arg_type(givenObj, requiredType):
    if not isinstance(givenObj, requiredType):
        raise TypeError('Given ' + str(givenObj.__class__) + ' argument, expected ' + str(requiredType))