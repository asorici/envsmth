from core.models.dbobject import ENV_DOMAIN, AREA_DOMAIN, ANNOTATION_DOMAIN, ANNOUNCEMENT_DOMAIN
from core.models.dbobject import EVENT_DOMAIN, USER_DOMAIN, HISTORY_DOMAIN, LAYOUT_DOMAIN, PRIVACY_DOMAIN
from core.db import db_connection
from pycassa.columnfamily import ColumnFamily
from pycassa import ConsistencyLevel
from threading import Lock

CONFIG_DOMAIN = "Config"
CONFIG_ROW = "conf"

MAX_PADDING_RANGE = 12

## create locks for each counter access
env_counter_lock = Lock()
area_counter_lock = Lock()
annotation_counter_lock = Lock()
announce_counter_lock = Lock()
event_counter_lock = Lock()
user_counter_lock = Lock()
layout_counter_lock = Lock()
history_counter_lock = Lock()
privacy_counter_lock = Lock()


domain_counter_map = {
    ENV_DOMAIN: ("env_id_count", env_counter_lock),
    AREA_DOMAIN: ("area_id_count", area_counter_lock),
    ANNOTATION_DOMAIN: ("annotation_id_count", annotation_counter_lock),
    ANNOUNCEMENT_DOMAIN: ("announce_id_count", announce_counter_lock),
    EVENT_DOMAIN: ("event_id_count", event_counter_lock),
    USER_DOMAIN: ("user_id_count", user_counter_lock),
    LAYOUT_DOMAIN: ("layout_id_count", layout_counter_lock),
    HISTORY_DOMAIN: ("history_id_count", history_counter_lock),
    PRIVACY_DOMAIN: ("privacy_id_count", privacy_counter_lock),
}

def get_row_key_id(domain):
    counter_column, counter_lock = domain_counter_map[domain]
    
    ## acquire lock before getting value of 
    counter_lock.acquire()
    try:
        client = db_connection.get_client()
        cf = ColumnFamily(client, CONFIG_DOMAIN)
        
        ## get new key id
        id_key = cf.get(CONFIG_ROW, counter_column)[counter_column]
        
        ## increment value if not None
        if id_key:
            new_id_key = id_key + 1
            cf.insert(CONFIG_ROW, {counter_column: new_id_key}, write_consistency_level=ConsistencyLevel.ALL)
        
        return id_key
        
        """
        if id_key:
            str_id_key = str(id_key)
            str_id_key.zfill(MAX_PADDING_RANGE)
            return str_id_key
        else:
            return None
        """
        
    finally:
        ## release lock before returning from this function
        counter_lock.release()
        
    