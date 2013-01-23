import sys, os
from coresql.db.objects import DateTimeList, AreaShape
from coresql.utils.geo import Point2D

#from pycassa.system_manager import *
#from pycassa.columnfamily import ColumnFamily

## must setup environment before core.* related imports
sys.path.extend(['./', '../'])
#os.environ["DJANGO_SETTINGS_MODULE"] = "envived.settings"
os.environ["DJANGO_SETTINGS_MODULE"] = "envsocial.settings"


from coresql.models import *
from django.contrib.auth.models import User
from django.utils import simplejson



    
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
    import re
    
    create_users = True
    create_env = False
    create_env_features = False
    create_layout = False
    create_areas = False
    create_area_features = False
    create_announcements = False
    create_annotations = False
    
    username_pattern = re.compile('\W+')
    
    ## 1) first lets create some users
    if create_users:
        user_list = []
        for i in range(10):
            email = 'user_' + str(i + 1) + '@email.com'
            password = 'pass_' + str(i + 1)
            username = username_pattern.sub('_', email)
            user_list.append((username, password, email))
            
        for username, password, email in user_list:
            user_obj = User.objects.create_user(username, email, password)
            user_obj.save()
        
    ## 2) create a dummy environment
    if create_env:
        owner = User.objects.get(email = 'user_1@email.com').get_profile()
        print owner.user.username
        print owner.user.id
        print owner.id  
        env_data = {'owner': owner, 'name': 'Environment1', 
                    'tags': u'tag1;tag2;tag3', 'width': 500, 'height': 500
                    }
        environment = Environment(**env_data)
        environment.save()

    ## 3) create env features
    if create_env_features:
        environment = Environment.objects.get(name='Environment1')
        feature_data = {'environment': environment, 'category': 'default', 
                    'data': "test data for environment1"}
        feature = Feature(**feature_data)
        feature.save()
        
        feature_data = {'environment': environment, 'category': 'order', 
                    'data':  {"order_menu":
                                    [{"category": "Andrei\'s Beer", 
                                        "items": 
                                            [{"name": "RedBeer", "description": "Coolest beer in town!", "price": "free"}, 
                                            {"name": "YellowBeer", "description": "Coolest beer in town!", "price": "free"}, 
                                            {"name": "BlueBeer", "description": "Coolest beer in town!", "price": "free"}
                                            ]}, 
                                    {"category": "Andrei\'s Chips", 
                                        "items": 
                                            [{"name": "RedChips", "description": "Coolest chips in town!", "price": "free"}, 
                                            {"name": "YellowChips", "description": "Coolest chips in town!", "price": "free"}, 
                                            {"name": "BlueChips", "description": "Coolest chips in town!", "price": "free"}
                                            ]}
                                ]}
                        }
        feature = Feature(**feature_data)
        feature.save()
        
    ## 4) create a layout
    if create_layout:
        environment = Environment.objects.get(name='Environment1')
        layout_data = {'environment': environment, 'mapURL': 'http://acme.aquasoft.com/some-dummy-layout-url'}
        environment_layout = Layout(**layout_data)
        environment_layout.save()
        
    ## 5) create some test areas for the environment
    if create_areas:
        # create 4 areas
        environment_layout = Layout.objects.get(environment__name = 'Environment1')
        environment = environment_layout.environment
        area_shape = AreaShape(AreaShape.TYPE_POLYGON, Point2D(10.0,10.0), Point2D(100.0,400.0))
         
        for i in range(4):
            area_data = {'name': 'area_' + str(i + 1), 'areaType': 'interest',
                         'tags': u'tag1;tag2;tag3', 
                         'layout': environment_layout, 'environment': environment, 'shape': area_shape
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
            
            feature_data = {'area': area, 'category': 'order', 
                    'data':  {"order_menu":
                                    [{"category": "Andrei\'s Beer", 
                                        "items": 
                                            [{"name": "RedBeer", "description": "Coolest beer in town!", "price": "free"}, 
                                            {"name": "YellowBeer", "description": "Coolest beer in town!", "price": "free"}, 
                                            {"name": "BlueBeer", "description": "Coolest beer in town!", "price": "free"}
                                            ]}, 
                                    {"category": "Andrei\'s Chips", 
                                        "items": 
                                            [{"name": "RedChips", "description": "Coolest chips in town!", "price": "free"}, 
                                            {"name": "YellowChips", "description": "Coolest chips in town!", "price": "free"}, 
                                            {"name": "BlueChips", "description": "Coolest chips in town!", "price": "free"}
                                            ]}
                                ]}
                        }
            
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
            
            ann1 = Announcement(environment = area.environment, area=area, triggers = trigger_list1, repeatEvery = "day", 
                                 data = u'some announcement data 1')
            
            triggers2 = [now + timedelta(days=1, hours=-10), now + timedelta(days = 2, hours=-8)]
            trigger_list2 = DateTimeList(triggers = triggers2)
            ann2 = Announcement(environment = area.environment, area=area, triggers = trigger_list2, repeatEvery = "week", 
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


def generate_qrcodes():
    from coresql.utils.qrcodegen import QRCodeManager
    
    ## generate qrcodes for all environments
    environment_list = Environment.objects.all()
    for environment in environment_list:
        environment_url = QRCodeManager.generate_qr_code(environment=environment)
        print environment_url
        
    ## generate qrcodes for all areas
    area_list = Area.objects.all()
    for area in area_list:
        area_url = QRCodeManager.generate_qr_code(area=area)
        print area_url


def urllib_header_test():
    import urllib2, time
    import email.utils as eut
    from datetime import datetime
    
    f = urllib2.urlopen("http://127.0.0.1:8000/envsocial/client/v1/resources/environment/1/?format=xml")
    print f.code
    header_info = f.info()
    h_date = header_info.get('Date')
    print h_date
    print eut.parsedate_tz(h_date)
    
    t = time.mktime(eut.parsedate_tz(h_date)[:9])
    now = time.mktime(time.gmtime())
    

def parse_program():
    from xml.dom.minidom import parse
    import datetime
    
    # open file
    prg_file = open("program-v1-custom.xml", "r") 
    document = parse(prg_file)
    prg_file.close()

    sessions = []
    ses_ct = 1

    entries = []
    entry_ct = 1

    session_elems = document.getElementsByTagName("session")
    for session_elem in session_elems:
        # parse data for a session
        title = session_elem.attributes["title"].value
        tag = session_elem.attributes["tag"].value
        date = datetime.datetime.strptime(session_elem.attributes["date"].value, "%d-%m-%Y")

        ses = {"id" : str(ses_ct), "title" : title, "tag" : tag, "location" : "/envsocial/client/v1/resources/environment/1/"}
        ses_ct += 1

        sessions.append(ses)

        # parse data for all entries of this session
        entry_elems = session_elem.getElementsByTagName("entry")
        for entry_elem in entry_elems:
            period_text = _getText(entry_elem.getElementsByTagName("period")[0].childNodes)
            title = _getText(entry_elem.getElementsByTagName("title")[0].childNodes)
            speakers = _getText(entry_elem.getElementsByTagName("speakers")[0].childNodes)
            
            periods_text = period_text.split("-")
            start_time = datetime.datetime.strptime(periods_text[0].strip(), "%H:%M")
            end_time = datetime.datetime.strptime(periods_text[1].strip(), "%H:%M")
            
            start_time = start_time.replace(year = date.year, month = date.month, day=date.day)
            end_time = end_time.replace(year = date.year, month = date.month, day=date.day)
            
            entry = {"id" : str(entry_ct), 
                     "sessionId" : ses["id"], 
                     "title" : title, 
                     "speakers" : speakers, 
                     "startTime" : start_time.strftime("%Y-%m-%dT%H:%M:00"),
                     "endTime" : end_time.strftime("%Y-%m-%dT%H:%M:00")
                    }
            entry_ct += 1
            
            entries.append(entry)
            
    #program = {"program" : {"sessions" : sessions, "entries" : entries}}
    #programJSON = simplejson.dumps(program)
    
    #f = open("program-v1-custom.json", "w")
    #print >>f, programJSON
    #f.close()
    return sessions, entries

def _getText(nodelist):
    rc = []
    for node in nodelist:
        if node.nodeType == node.TEXT_NODE:
            rc.append(node.data)
    return ''.join(rc)


def build_wims_simulation(argv=None):
    from coresql.models import DescriptionFeature, ProgramFeature, Session, Entry, PeopleFeature
    from django.utils.encoding import smart_unicode
    import datetime
    
    # 1) create the wims environment
    owner = User.objects.get(email = 'user_1@email.com').get_profile()
    env_data = {'owner': owner, 'name': 'WIMS 2012 Conference',
                'tags': u'conference;web intelligence;mining and semantics', 'width': 500, 'height': 500
               }
    environment = Environment(**env_data)
    environment.save()
    
    print ">> Done creating environment!"
    
    # 2) create the features for the environment
    ## description feature
    desc_feature_data = {'environment': environment, 'category': 'default', 
                    'description': "The 2nd International Conference on Web Intelligence, Mining and Semantics (WIMS'12) is organised under the auspices of University of Craiova. This is the second in a new series of conferences concerned with intelligent approaches to transform the World Wide Web into a global reasoning and semantics-driven computing machine. "}
    desc_feature = DescriptionFeature(**desc_feature_data)
    desc_feature.save()
    
    print ">> Done adding desc feature for environment"
    
    ## program feature
    program_feature_data = {'environment': environment, 'category': 'program', 
                    'description' : "Presentation schedule for the WIMS'12 conference"}
    program_feature = ProgramFeature(**program_feature_data)
    program_feature.save()
    
    ## create the sessions and the entries for the program
    sessions, entries = parse_program()
    for ses in sessions:
        db_ses = Session(title = ses['title'], tag = ses['tag'], program=program_feature)
        db_ses.save()
        
        session_entries = filter(lambda e: e['sessionId'] == ses['id'], entries)
        for entry in session_entries:
            db_entry_data = {'session' : db_ses, 'title' : entry['title'], 
                             'speakers' : smart_unicode(entry['speakers']),
                             'startTime' : datetime.datetime.strptime(entry['startTime'], "%Y-%m-%dT%H:%M:%S"),
                             'endTime' : datetime.datetime.strptime(entry['endTime'], "%Y-%m-%dT%H:%M:%S")}
            db_entry = Entry(**db_entry_data)
            db_entry.save()
    
    print ">> Done adding program feature for environment"
    
    ## create 3 areas and add the default and people features for them
    ## the program feature only needs to be inputed at the environment level
    layout_data = {'environment': environment}
    environment_layout = Layout(**layout_data)
    environment_layout.save()
    
    ## area 1 - session 1, 2 rooms
    area1_data = {'name': 'Blue Hall', 'areaType': 'interest',
                         'layout': environment_layout, 'environment': environment
                 }
    area1 = Area(**area1_data)
    area1.save()
    desc_feature_data = {'area': area1, 'category': 'default', 
                    'description':"The Blue Hall will host the talks by the invited speakers and Tutorial 1."}
    desc_feature = DescriptionFeature(**desc_feature_data)
    desc_feature.save()
    
    people_feature = PeopleFeature(description = "people feature for Blue Hall", area = area1, category = "people")
    people_feature.save()
    
    print ">> Done adding area 1"
    
    area2_data = {'name': 'Room 443D', 'areaType': 'interest',
                         'layout': environment_layout, 'environment': environment
                 }
    area2 = Area(**area2_data)
    area2.save()
    desc_feature_data = {'area': area2, 'category': 'default', 
                    'description':"Room 443D will host Sessions 1, 3, 5, 7, 9 and Tutorials 2 and 4."}
    desc_feature = DescriptionFeature(**desc_feature_data)
    desc_feature.save()
    
    people_feature = PeopleFeature(description = "people feature for Room 443D", area = area2, category = "people")
    people_feature.save()
    
    print ">> Done adding area 2"
    
    area3_data = {'name': 'Room 443C', 'areaType': 'interest',
                         'layout': environment_layout, 'environment': environment
                 }
    area3 = Area(**area3_data)
    area3.save()
    desc_feature_data = {'area': area3, 'category': 'default', 
                    'description':"Room 443C will host Sessions 2, 4, 6, 8, 10 and Tutorials 3 and 5."}
    desc_feature = DescriptionFeature(**desc_feature_data)
    desc_feature.save()
    
    people_feature = PeopleFeature(description = "people feature for Room 443C", area = area3, category = "people")
    people_feature.save()
    
    print ">> Done adding area 3"
    
    area4_data = {'name': 'University Hall', 'areaType': 'interest',
                         'layout': environment_layout, 'environment': environment
                 }
    area4 = Area(**area4_data)
    area4.save()
    desc_feature_data = {'area': area4, 'category': 'default', 
                    'description':"University Hall will host Poster Sessions and Coffee Breaks."}
    desc_feature = DescriptionFeature(**desc_feature_data)
    desc_feature.save()
    
    people_feature = PeopleFeature(description = "people feature for University Hall", area = area4, category = "people")
    people_feature.save()
    
    print ">> Done adding area 4"
    

if __name__ == "__main__":
    #main()
    #test_Q()
    #dummy_sql_insert()
    generate_qrcodes()
    #urllib_header_test()
    #parse_program()
    #build_wims_simulation()
    
    
