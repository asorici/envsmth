from django.db.models import Q
from django.db.utils import DatabaseError
from core.models.utils import assert_arg_type
from core.db import db_connection

class CassandraQuery(object):
    SIMPLE_QUERY = "simple"
    COMPLEX_QUERY = "complex"
    
    OP_SELECT = "select"
    OP_INSERT = "insert"
    OP_UPDATE = "update"
    OP_DELETE = "delete"
    OP_FETCH = "fetch"

    MAX_FETCH_COUNT = 10000

    def __init__(self, domain, op_type="select", data=None):
        self.domain = domain
        self.op_type = op_type
        self.data = data
        
        self.where_node = None
        self.offset_id = 0
        self.limit = 100
    
    def add_filter_statements(self, **statements):
        for item in statements.items():
            self.add_filter_object(Q(item))
    
    def add_filter_object(self, qObj):
        assert_arg_type(qObj, Q)
        
        if not self.where_node:
            self.where_node = qObj
        else:
            self.where_node = self.where_node & qObj
    
    
    def set_fetch_limits(self, offset_id, limit):
        self.offset_id = offset_id
        self.limit = limit
    
    def execute_query(self, offset = 0, limit = 100):
        if self.op_type == CassandraQuery.OP_SELECT:
            pass
        elif self.op_type == CassandraQuery.OP_INSERT:
            pass
        elif self.op_type == CassandraQuery.OP_UPDATE:
            pass
        elif self.op_type == CassandraQuery.OP_DELETE:
            pass
        elif self.op_type == CassandraQuery.OP_FETCH:
            pass
        else:
            raise DatabaseError("Invalid query operation type: " + self.op_type)