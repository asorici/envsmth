from django.conf import settings

class QRCodeManager(object):
    #HOST = "192.168.100.102:8080"
    #HOST = "192.168.1.103:8080"
    #HOST = "172.16.2.145:8080"
    #HOST = "envived.com:8800/envived"
    
    @staticmethod
    def generate_qr_code(area = None, environment = None, virtual = False):
        from django.core.urlresolvers import reverse
        checkin_url_path = reverse("checkin")
        
        if area is None and environment is None:
            return None
        
        elif not area is None:
            ## we first check to see if there is an area object
            data_url = checkin_url_path + "?area=" + str(area.id) + "&virtual=" + str(virtual)
            img_seed_name = "a" + str(area.id)
            if virtual:
                img_seed_name += "_v"
            
            return QRCodeManager._gen_code_img(data_url, img_seed_name)
        
        elif not environment is None:
            ## then we check to see if there is an environment object
            data_url = checkin_url_path + "?environment=" + str(environment.id) + "&virtual=" + str(virtual)
            img_seed_name = "e" + str(environment.id)
            if virtual:
                img_seed_name += "_v"
            
            return QRCodeManager._gen_code_img(data_url, img_seed_name)
    
    @staticmethod
    def _gen_code_img(data_url, img_seed_name):
        from qrcode import QRCode, constants
        qr = QRCode(
            version=1,
            error_correction=constants.ERROR_CORRECT_L,
            box_size=10,
            border=4,
        )
        
        full_url = "http://" + QRCodeManager.HOST + data_url
        qr.add_data(full_url)
        qr.make(fit=True)
        img = qr.make_image()
        
        img_name = "qrcode_" + img_seed_name + ".png"
        img_path = settings.MEDIA_ROOT + "qrcodes/" + img_name
        img_url = settings.MEDIA_URL + "qrcodes/" + img_name
        
        img.save(img_path, 'PNG')
         
        return img_url
