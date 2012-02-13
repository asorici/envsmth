from tastypie.validation import Validation

class AnnotationValidation(Validation):
    def is_valid(self, bundle, request=None):
        from client.api import EnvironmentResource, AreaResource
        
        if not bundle.data:
            return {'__all__': 'No data submitted.'}
        
        errors = {}
        
        if bundle.request.method.upper() == "POST":
            env_obj = None
            area_obj = None
            
            if 'env' in bundle.data:
                try:
                    env_obj = EnvironmentResource().get_via_uri(bundle.data['env']) 
                except:
                    env_obj = None
                    
            if 'area' in bundle.data:
                try:
                    area_obj = AreaResource().get_via_uri(bundle.data['area'])
                except:
                    area_obj = None
        
            
            if env_obj is None and area_obj is None:
                errors['env'] = ['No or wrong environment uri']
                errors['area'] = ['No or wrong area uri']
                
            if not env_obj is None and not area_obj is None and area_obj.env != env_obj:
                errors['env'] = ["Environment resource mismatches parent environment of area resource."]
            
        ## TODO - some additional validation of the data field might also be possible
        if not 'data' in bundle.data or not bundle.data['data']:
            errors['data'] = ["No or empty data field."]
        
        return errors