from math import sqrt
from coresql.utils.validations import assert_arg_type

def get_line_segment_midpoint(point1, point2):
    return Point2D((point1._x + point2._x) / float(2),
            (point1._y + point2._y) / float(2))

def get_line_eq(point1, point2):
    A = point2._y - point1._y
    B = point1._x - point2._x
    C = A * point1._x + B * point1._y
    return (A, B, C)

def get_line_intersection(A1, B1, C1, A2, B2, C2):
    det = A1 * B2 - A2 * B1
    if (det == 0):
        return None
    return Point2D((B2 * C1 - B1 * C2) / float(det),
                (A1 * C2 - A2 * C1) / float(det))

def get_distance_2D(p1, p2):
    return sqrt( (p1._x - p2._x) * (p1._x - p2._x) +
                 (p1._y - p2._y) * (p1._y - p2._y))

def get_circle(p1, p2, p3):
    # get eq of line given by p1 and p2
    A1, B1, _ = get_line_eq(p1, p2)
    # get eq of line given by p2 and p3
    A2, B2, _ = get_line_eq(p2, p3)
    
    # get eq of perpendicular bisector of segment p1p2
    m_p1p2 = get_line_segment_midpoint(p1, p2)
    D1 = -B1 * m_p1p2._x + A1 * m_p1p2._y
    
    # get eq of perpendicular bisector of segment p2p3
    m_p2p3 = get_line_segment_midpoint(p2, p3)
    D2 = -B2 * m_p2p3._x + A2 * m_p2p3._y
    
    # get the center of the circle
    center = get_line_intersection(-B1, A1, D1, -B2, A2, D2)
    # get the radius of the circle
    radius = get_distance_2D(p1, center)
    
    return (center, radius)


class Point2D(object):
    
    DB_SEP = ":"
    
    def __init__(self, x, y):
        assert_arg_type(x, float)
        assert_arg_type(y, float)
        self._x = x
        self._y = y
    
    def dbEncode(self):
        return str(self._x) + self.DB_SEP + str(self._y)
    
    @staticmethod
    def dbDecode(pointString):
        coord = map(lambda x : float(x), pointString.split(Point2D.DB_SEP))
#        assert_arg_list_type(coord, float)
        return Point2D(*coord)
    
    def __repr__(self):
        return "Point2D(" + str(self._x) + "," + str(self._y) + ")"

class Geolocation(object):
    
    def __init__(self, lat, long):
        assert_arg_type(lat, float)
        assert_arg_type(long, float)
        self.lat = lat
        self.long = long