import sys, os
from coresql.db.objects import DateTimeList, AreaShape
from coresql.utils.geo import Point2D
#from pycassa.system_manager import *
#from pycassa.columnfamily import ColumnFamily

## must setup environment before core.* related imports
sys.path.extend(['./', '../'])
os.environ["DJANGO_SETTINGS_MODULE"] = "envsocial.settings"

from coresql.models import *
from django.contrib.auth.models import User

#from core.db import db_connection

def main(argv=None):
    """
    sys_mg = SystemManager("localhost:9160")
     
    print sys_mg.list_keyspaces()

    sys_mg.create_keyspace("envsocial", SIMPLE_STRATEGY, {'replication_factor': '1'})
    sys_mg.create_column_family("envsocial", "Environment")
    sys_mg.create_column_family("envsocial", "Area")
    sys_mg.create_column_family("envsocial", "Annotation")
    sys_mg.create_column_family("envsocial", "Event")
    sys_mg.create_column_family("envsocial", "Announcement")
    sys_mg.create_column_family("envsocial", "History")
    sys_mg.create_column_family("envsocial", "Privacy")
    sys_mg.create_column_family("envsocial", "Layout")
    sys_mg.create_column_family("envsocial", "Config")
    
    sys_mg.create_index("envsocial", "Environment", "name", UTF8_TYPE, index_name="environment_name_index")
    sys_mg.create_index("envsocial", "Area", "name", UTF8_TYPE, index_name="area_name_index")
    
    sys_mg.alter_column('envsocial', 'Config', 'env_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'area_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'event_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'announce_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'annotation_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'user_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'history_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'layout_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'privacy_id_count', LONG_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'env_cache_time', INT_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'area_cache_time', INT_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'event_cache_time', INT_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'announce_poll_time', INT_TYPE)

    sys_mg.alter_column('envsocial', 'Config', 'max_levels', INT_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'max_tags', INT_TYPE)
    sys_mg.alter_column('envsocial', 'Config', 'max_children', INT_TYPE)

    client = db_connection.get_client()
    config_cf = ColumnFamily(client, "Config")
    config_cf.insert('conf', {'env_id_count' : 0, 'area_id_count': 0, 'announce_id_count':0, 
                              'annotation_id_count':0, 'event_id_count':0, 'user_id_count':0, 
                              'history_id_count':0, 'layout_id_count':0, 'privacy_id_count':0,
                              'env_cache_time':1000, 'area_cache_time':1000, 'event_cache_time':1000, 'announce_poll_time':10, 'max_levels':100, 'max_tags':100, 'max_children':100})    
    """
    pass

    
def test_Q():
    from django.db.models import Q

    a = Q(name__eq="alex")
    b = Q(len__gt=12)
    c = Q(page__eq=4)

    d = a & b & ~c

    print d
    print d.children
    print d.children[2].children


def dummy_sql_insert():
    create_users = True
    create_env = True
    create_env_features = True
    create_layout = True
    create_areas = True
    create_area_features = True
    create_announcements = True
    create_annotations = True
    
    ## 1) first lets create some users
    if create_users:
        user_list = []
        for i in range(10):
            username = 'user_' + str(i + 1)
            password = 'pass_' + str(i + 1)
            email = 'user_' + str(i + 1) + '@email.com' 
            user_list.append((username, password, email))
            
        for username, password, email in user_list:
            user_obj = User.objects.create_user(username, email, password)
            user_obj.save()
        
    ## 2) create a dummy environment
    if create_env:
        owner = User.objects.get(username = 'user_1').get_profile()
        print owner.user.username
        print owner.user.id
        print owner.id  
        env_data = {'owner': owner, 'name': 'Environment1', 
                    'tags': u'tag1;tag2;tag3', 'width': 500, 'height': 500
                    }
        env = Environment(**env_data)
        env.save()

    ## 3) create env features
    if create_env_features:
        env = Environment.objects.get(name='Environment1')
        feature_data = {'env': env, 'category': 'default', 
                    'data': u'test data for environment1'}
        feature = Feature(**feature_data)
        feature.save()
        
    ## 4) create a layout
    if create_layout:
        env = Environment.objects.get(name='Environment1')
        layout_data = {'env': env, 'mapURL': 'http://acme.aquasoft.com/some-dummy-layout-url'}
        env_layout = Layout(**layout_data)
        env_layout.save()
        
    ## 5) create some test areas for the environment
    if create_areas:
        # create 4 areas
        env_layout = Layout.objects.get(env__name = 'Environment1')
        env = env_layout.env
        area_shape = AreaShape(AreaShape.TYPE_POLYGON, Point2D(10.0,10.0), Point2D(100.0,400.0))
         
        for i in range(4):
            area_data = {'name': 'area_' + str(i + 1), 'areaType': 'interest',
                         'tags': u'tag1;tag2;tag3', 
                         'layout': env_layout, 'env': env, 'shape': area_shape
                         }
            area = Area(**area_data)
            area.save()
        
    
    ## 6) create features for the areas
    if create_area_features:
        for area in Area.objects.all():
            feature_data = {'area': area, 'category': 'default', 
                        'data': u'test data for ' + area.name}
            feature = Feature(**feature_data)
            feature.save()    
        
    ## 7) create a set of announcements
    if create_announcements:
        from datetime import datetime, timedelta
        ## create announcements for area_1 and area_2
        for i in range(2):
            area_name = 'area_' + str(i+1)
            area = Area.objects.get(name=area_name)
            
            now = datetime.now()
            triggers1 = [now + timedelta(hours=-10), now + timedelta(hours=-8)]
            trigger_list1 = DateTimeList(triggers = triggers1) 
            
            ann1 = Announcement(env = area.env, area=area, triggers = trigger_list1, repeatEvery = "day", 
                                 data = u'some announcement data 1')
            
            triggers2 = [now + timedelta(days=1, hours=-10), now + timedelta(days = 2, hours=-8)]
            trigger_list2 = DateTimeList(triggers = triggers2)
            ann2 = Announcement(env = area.env, area=area, triggers = trigger_list2, repeatEvery = "week", 
                                 data = u'some announcement data 2')
            
            ann1.save()
            ann2.save()
            
    ## 8) create some annotations for each area
    if create_annotations:
        import random, time
        
        areas = Area.objects.all()
        users = list(UserProfile.objects.all())
        
        for area in areas:
            for i in range(3):
                user = users[random.randint(0, len(users) - 1)]
                annotation = Annotation(user=user, area=area, 
                                        data = u'dummy annotation ' + str(i+1) + ' for area ' + area.name)
                time.sleep(2)
                annotation.save()


if __name__ == "__main__":
    #main()
    #test_Q()
    dummy_sql_insert()
    
    
