def str2bool(s):
    if s.lower() in ('true', 't', '1'):
        return True
    elif s.lower() in ('false', 'f', '0'):
        return False
    else:
        raise ValueError("Value '" + s + "' cannot be parsed to a boolean.")