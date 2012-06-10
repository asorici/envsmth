from tastypie.validation import Validation

class AnnotationValidation(Validation):
    def is_valid(self, bundle, request=None):
        from client.api import EnvironmentResource, AreaResource
        from coresql.models import CATEGORY_CHOICES
        
        if not bundle.data:
            return {'__all__': 'No data submitted.'}
        
        errors = {}
        
        if bundle.request.method.upper() == "POST":
            env_obj = None
            area_obj = None
            
            if 'environment' in bundle.data:
                try:
                    env_obj = EnvironmentResource().get_via_uri(bundle.data['environment']) 
                except:
                    env_obj = None
                    
            if 'area' in bundle.data:
                try:
                    area_obj = AreaResource().get_via_uri(bundle.data['area'])
                except:
                    area_obj = None
            
            if env_obj is None and area_obj is None:
                errors['environment'] = ['No or wrong environment uri']
                errors['area'] = ['No or wrong area uri']
                
            if not env_obj is None and not area_obj is None and area_obj.env != env_obj:
                errors['environment'] = ["Environment resource mismatches parent environment of area resource."]
            
        ## TODO - some additional validation of the data field might also be possible
        if not 'data' in bundle.data or not bundle.data['data']:
            errors['data'] = ["No or empty data field."]
        
        
        if not 'category' in bundle.data or not (bundle.data['category'], bundle.data['category']) in CATEGORY_CHOICES:
            errors['category'] = ["No category specified or wrong category."]
        
        return errors