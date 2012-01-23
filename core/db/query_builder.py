from django.db.models import Q
from django.db.utils import DatabaseError
from core.models.utils import assert_arg_type
from core.db import db_connection
from pycassa.columnfamily import ColumnFamily


"""
===================== GENERAL QUERY MANAGEMENT =================== 
"""
class CassandraQuery(object):
    SIMPLE_QUERY = "simple"
    COMPLEX_QUERY = "complex"
    
    OP_SELECT = "select"
    OP_INSERT = "insert"
    OP_BATCH_INSERT = "batch_insert"
    OP_UPDATE = "update"
    OP_DELETE = "delete"
    OP_BATCH_DELETE = "batch_delete"
    OP_FETCH = "fetch"

    MAX_FETCH_COUNT = 10000

    def __init__(self, domain, op_type="select", data=None, **additional_args):
        self.domain = domain
        self.op_type = op_type
        self.data = data
        self.additional_args = additional_args
        
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
    
    
    def execute_query(self):
        if self.op_type == CassandraQuery.OP_SELECT:
            pass
        
        elif self.op_type == CassandraQuery.OP_FETCH:
            pass
        
        elif self.op_type == CassandraQuery.OP_INSERT:
            ins_mgr = InsertManager(self.data, CassandraQuery.OP_INSERT)
            success, fault = ins_mgr.execute()
            
            if not success:
                raise DatabaseError("INSERT failed for object: " + str(self.data) + ". " + str(fault))
        
        elif self.op_type == CassandraQuery.OP_BATCH_INSERT:
            ins_mgr = InsertManager(self.data, CassandraQuery.OP_BATCH_INSERT)
            success, fault = ins_mgr.execute()
            
            if not success:
                raise DatabaseError("BATCH INSERT failed for collection: " + str(self.data) + ". " + str(fault))
            
            
        elif self.op_type == CassandraQuery.OP_UPDATE:
            """
            for now tread update just like an insert -- include a column fitler afterwards
            """
            ins_mgr = InsertManager(self.data, CassandraQuery.OP_INSERT)
            success, fault = ins_mgr.execute()
            
            if not success:
                raise DatabaseError("UPDATE failed for object: " + str(self.data) + ". " + str(fault))
            
        elif self.op_type == CassandraQuery.OP_DELETE:
            cascade = True
            if self.additional_args.has_key("cascade"):
                cascade = self.additional_args["cascade"]
            
            del_mgr = DeleteManager(self.data, CassandraQuery.OP_DELETE, cascade)
            success, fault = del_mgr.execute()
            
            if not success:
                raise DatabaseError("DELETE failed for object: " + str(self.data) + ". " + str(fault))
            
        elif self.op_type == CassandraQuery.OP_BATCH_DELETE:
            cascade = True
            if self.additional_args.has_key("cascade"):
                cascade = self.additional_args["cascade"]
            
            del_mgr = DeleteManager(self.data, CassandraQuery.OP_BATCH_DELETE, cascade)
            success, fault = del_mgr.execute()
            
            if not success:
                raise DatabaseError("BATCH DELETE failed for collection: " + str(self.data) + ". " + str(fault))
        
        else:
            raise DatabaseError("Invalid query operation type: " + self.op_type)



"""
===================== DELETE MANAGEMENT =================== 
"""
class DeleteManager(object):
    def __init__(self, data, op_type, cascade):
        self.data = data
        self.op_type = op_type
        self.cascade = cascade
        
    def _validate_data(self):
        ## check for data object
        if not self.data:
            return (False, DatabaseError("Invalid DELETE query: no data object supplied"))
        
        if self.op_type == CassandraQuery.OP_DELETE: 
            row_key = self.data.get_pk()
            if not row_key:
                return (False, DatabaseError("DELETE Error: no primary key found for object " + str(self.data) + 
                                    ". Does the field `id' exist? Was get_pk() overridden?"))
        
        elif self.op_type == CassandraQuery.OP_BATCH_DELETE:
            if not reduce(lambda a, b: a and b, map(lambda obj: obj.get_pk(), self.data.objSet)):
                return (False, DatabaseError("BATCH DELETE Error: object in collection [" + str(self.data) + "] misses a primary key. "
                                    + " Does the field `id' exist? Was get_pk() overridden?"))
                
        return (True, None)


    def execute(self):
        ## first validate data
        data_ok, fault = self._validate_data()
        
        if not data_ok:
            return (False, fault)
        
        ## if data ok, construct InsertCommands
        if self.op_type == CassandraQuery.OP_DELETE:
            try:
                domain = self.data.domain
                row_key = self.data.get_pk()
                
                client = db_connection.get_client()
                cf = ColumnFamily(client, domain)
                
                ## if cascading is enabled, first delete all DBObject and collections comprised in this DBObject
                if self.cascade:
                    pass
                
                ## lastly remove data for current element
                cf.remove(row_key)
                
                return (True, None)
            except Exception, ex:
                return (False, ex)
                
        
        elif self.op_type == CassandraQuery.OP_BATCH_DELETE:
            pass




"""
===================== INSERT MANAGEMENT =================== 
"""
class InsertManager(object):
    
    def __init__(self, data, op_type):
        self.data = data
        self.op_type = op_type
        self.commands = []
        #self.success = True
    
        
    def _set_commands(self):
        if self.op_type == CassandraQuery.OP_INSERT:
            basic_type_items, object_items, collection_items = self._get_object_fields_by_type(self.data)
            
            row_key = self.data.get_pk()
            if basic_type_items:
                self.commands.append(InsertCommand((self.data.domain, row_key, dict(basic_type_items)), InsertCommand.INS_BASIC))
                
            for obj in object_items:
                self.commands.append(InsertCommand(obj, InsertCommand.INS_OBJECT))
            
            for col in collection_items:
                self.commands.append(InsertCommand(col, InsertCommand.INS_OBJECT))
                
        elif self.op_type == CassandraQuery.OP_BATCH_INSERT:
            collection_objects = self.data.objSet
            basic_type_item_dict = {}
            batch_object_list = []
            batch_collection_list = []
            
            for dbObj in collection_objects:
                basic_type_items, object_items, collection_items = self._get_object_fields_by_type(dbObj)
                row_key = dbObj.get_pk()
                
                if basic_type_items:
                    basic_type_item_dict[row_key] = dict(basic_type_items)
                    
                batch_object_list.extend(object_items)
                batch_collection_list.extend(collection_items)
                
            self.commands.append(InsertCommand((self.data.domain, basic_type_item_dict), InsertCommand.INS_BATCH))
            for obj in batch_object_list:
                self.commands.append(InsertCommand(obj, InsertCommand.INS_OBJECT))
            
            for col in batch_collection_list:
                self.commands.append(InsertCommand(col, InsertCommand.INS_OBJECT))
    
    
    def _get_object_fields_by_type(self, dbObject):
        from core.models.dbobject import DBObject
        from core.models.dbcollection import DBCollection
        
        var_items = vars(dbObject).items()
        
        basic_type_items = filter(lambda (name, val): is_db_field_name(name) and not isinstance(val, (DBObject, DBCollection)), var_items)
        object_items = filter(lambda (name, val): is_db_field_name(name) and isinstance(val, DBObject), var_items)
        collection_items = filter(lambda (name, val): is_db_field_name(name) and isinstance(val, DBCollection), var_items)
            
        return (basic_type_items, object_items, collection_items)
    
        
    def _validate_data(self):
        ## check for data object
        if not self.data:
            return (False, DatabaseError("Invalid INSERT query: no data object supplied"))
        
        if self.op_type == CassandraQuery.OP_INSERT: 
            row_key = self.data.get_pk()
            if not row_key:
                return (False, DatabaseError("INSERT Error: no primary key found for object " + str(self.data) + 
                                    ". Does the field `id' exist? Was get_pk() overridden?"))
        
        elif self.op_type == CassandraQuery.OP_BATCH_INSERT:
            if not reduce(lambda a, b: a and b, map(lambda obj: obj.get_pk(), self.data.objSet)):
                return (False, DatabaseError("INSERT Error: object in collection [" + str(self.data) + "] misses a primary key. "
                                    + " Does the field `id' exist? Was get_pk() overridden?"))
        
        return (True, None)
        
    def execute(self):
        ## first validate data
        data_ok, fault = self._validate_data()
        
        if not data_ok:
            return (False, fault)
            
        ## if data ok, construct InsertCommands
        self._set_commands()
        
        for command_index in range(len(self.commands)):
            try:
                command = self.commands[command_index]
                command.do()
            except Exception, ex:
                self._redo(command_index)
                #self.success = False
                return (False, ex)
               
        ## if all succeeds return true for success
        return (True, None)
        
        
    def _redo(self, command_index):
        undo_commands = reversed(self.commands[0:command_index])
        for command in undo_commands:
            try:
                command.undo()
            except Exception, ex:
                print "INTERNAL ERROR! Undo unsuccessful for insertion command: " + str(command) + ". " + str(ex)
        
        


class InsertCommand(object):
    INS_BASIC = 0
    INS_OBJECT = 1
    INS_BATCH = 2
    
    def __init__(self, data, type):
        self.data = data
        self.type = type
        
    def __repr__(self):
        return "(INSERT COMMAND with data: " + str(self.data) + ")"
    
    def do(self):
        if type == InsertCommand.INS_BASIC:
            ## I know that data for a basic insert is of this tuple type
            domain, row_key, basic_type_dict = self.data
            
            client = db_connection.get_client()
            cf = ColumnFamily(client, domain)
            cf.insert(row_key, basic_type_dict)
                
        elif type == InsertCommand.INS_OBJECT:
            ## call the save operation for the object
            if self.data:
                self.data.save()
        
        elif type == InsertCommand.INS_BATCH:
            ## Again, I know data for a batch insert will be of the following tuple type
            domain, basic_type_item_dict = self.data
            client = db_connection.get_client()
            cf = ColumnFamily(client, domain)
            b = cf.batch()
            
            for row_key in basic_type_item_dict.keys():
                b.insert(row_key, basic_type_item_dict[row_key])
            b.send()
    
    def undo(self):
        if type == InsertCommand.INS_BASIC:
            ## I know that data for a basic insert is of this tuple type
            domain, row_key, basic_type_dict = self.data
            
            client = db_connection.get_client()
            cf = ColumnFamily(client, domain)
            cf.remove(row_key)
                
        elif type == InsertCommand.INS_OBJECT:
            ## call the save operation for the object
            if self.data:
                self.data.delete(cascade=False)
        
        elif type == InsertCommand.INS_BATCH:
            domain, basic_type_item_dict = self.data
            client = db_connection.get_client()
            cf = ColumnFamily(client, domain)
            
            b = cf.batch()
            for row_key in basic_type_item_dict.keys():
                b.remove(row_key)
            b.send()



"""
===================== AUXILIARY =================== 
"""
def is_db_field_name(field_name):
    if field_name[0] == '_':
        return False
    return True
          