from django import forms
from coresql.models import Environment, Area, Annotation, Announcement


class EnvironmentForm(forms.ModelForm):
    """
    Use model form to link fields to Environment model fields but make all fields not required 
    """
    def __init__(self, set_not_required = False, *args, **kwargs):
        super(EnvironmentForm, self).__init__(*args, **kwargs)
        
        if set_not_required:
            for key in self.fields:
                self.fields[key].required = False
    
    class Meta:
        model = Environment
        exclude = ("timestamp",)
        
    
    def clean_data(self):
        """
        just for future prospects of additional data field cleaning
        for now do nothing 
        """
        return self.cleaned_data['data']



class UpdateEnvironmentForm(EnvironmentForm):
    class Meta(EnvironmentForm.Meta):
        exclude = ("timestamp", "owner", "parentID", "width", "height")
        
        


class AreaForm(forms.ModelForm):
    """
    Use model form to link fields to Area model fields but make all fields not required 
    """
    def __init__(self, set_not_required = False, *args, **kwargs):
        super(AreaForm, self).__init__(*args, **kwargs)
        
        if set_not_required:
            for key in self.fields:
                self.fields[key].required = False
    
    class Meta:
        model = Area
        exclude = ("timestamp", )
        
    
    def clean_data(self):
        """
        just for future prospects of additional data field cleaning
        for now do nothing 
        """
        return self.cleaned_data['data']


class UpdateAreaForm(AreaForm):
    class Meta(AreaForm.Meta):
        exclude = ("timestamp", "areaType", "layout", "shape", "environment")




class AnnotationForm(forms.ModelForm):
    def __init__(self, set_not_required = False, *args, **kwargs):
        super(AnnotationForm, self).__init__(*args, **kwargs)
        
        if set_not_required:
            for key in self.fields:
                self.fields[key].required = False
    
    def clean(self):
        cleaned_data = super(AnnotationForm, self).clean()
        env = cleaned_data.get("environment")
        area = cleaned_data.get("area")
        
        if env is None and area is None:
            raise forms.ValidationError("Environment and Area data are both missing. At least one is required.")
        
        if env and area and area.environment != env:
            raise forms.ValidationError("Environment and Area data is contradictory." + 
                                        "Environment and Area.Environment don't match.")
        
        return cleaned_data

    class Meta:
        model = Annotation
        exclude = ("timestamp", "user",)


class UpdateAnnotationForm(AnnotationForm):
    class Meta(AnnotationForm.Meta):
        exclude = ("timestamp", "environment", "area", "user")



class AnnouncementForm(forms.ModelForm):
    def __init__(self, set_not_required = False, *args, **kwargs):
        super(AnnouncementForm, self).__init__(*args, **kwargs)
        
        if set_not_required:
            for key in self.fields:
                self.fields[key].required = False

    def clean(self):
        cleaned_data = super(AnnouncementForm, self).clean()
        env = cleaned_data.get("environment")
        area = cleaned_data.get("area")
        
        if env is None and area is None:
            raise forms.ValidationError("Environment and Area data are both missing. At least one is required.")
        
        if env and area and area.env != env:
            raise forms.ValidationError("Environment and Area data is contradictory." + 
                                        "Environment and Area.Environment don't match.")
        
        return cleaned_data

    class Meta:
        model = Announcement
        exclude = ("timestamp",)


class UpdateAnnouncementForm(AnnouncementForm):
    class Meta(AnnouncementForm.Meta):
        exclude = ("timestamp", "environment", "area")


class CheckinForm(forms.Form):
    area = forms.IntegerField(required = False)
    environment = forms.IntegerField(required = False)
    
    def clean(self):
        cleaned_data = super(CheckinForm, self).clean()
        environment = cleaned_data.get("environment")
        area = cleaned_data.get("area")
    
        if environment is None and area is None:
            raise forms.ValidationError("Environment and Area data are both missing. At least one is required.")
        
        if environment and area and area.environment != environment:
            raise forms.ValidationError("Environment and Area data is contradictory." + 
                                        "Environment and Area.Environment don't match.")
        
        return cleaned_data
    

class LoginForm(forms.Form):
    email = forms.EmailField(required = True)
    password = forms.CharField(required = True)
    
    def __init__(self, *args, **kwargs):
        self.user_cache = None
        super(LoginForm, self).__init__(*args, **kwargs)
    
    def clean(self):
        from django.contrib.auth import authenticate
        from django.utils.translation import ugettext_lazy as _
        
        cleaned_data = super(LoginForm, self).clean()
        
        email = cleaned_data.get('email')
        password = cleaned_data.get('password')

        if email and password:
            self.user_cache = authenticate(username=email, password=password)
            if self.user_cache is None:
                raise forms.ValidationError(_("Please enter a correct username and password. Note that both fields are case-sensitive."))
            elif not self.user_cache.is_active:
                raise forms.ValidationError(_("This account is inactive."))
            
        return cleaned_data
    
    
    def get_user_id(self):
        if self.user_cache:
            return self.user_cache.id
        return None

    
    def get_user(self):
        return self.user_cache