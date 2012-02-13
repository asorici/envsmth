from django import forms
from django.contrib.auth.forms import AuthenticationForm
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
        exclude = ("timestamp", "areaType", "layout", "shape", "env")




class AnnotationForm(forms.ModelForm):
    def __init__(self, set_not_required = False, *args, **kwargs):
        super(AnnotationForm, self).__init__(*args, **kwargs)
        
        if set_not_required:
            for key in self.fields:
                self.fields[key].required = False
    
    def clean(self):
        cleaned_data = super(AnnotationForm, self).clean()
        env = cleaned_data.get("env")
        area = cleaned_data.get("area")
        
        if env is None and area is None:
            raise forms.ValidationError("Environment and Area data are both missing. At least one is required.")
        
        if env and area and area.env != env:
            raise forms.ValidationError("Environment and Area data is contradictory." + 
                                        "Environment and Area.Environment don't match.")
        
        return cleaned_data

    class Meta:
        model = Annotation
        exclude = ("timestamp", "user",)


class UpdateAnnotationForm(AnnotationForm):
    class Meta(AnnotationForm.Meta):
        exclude = ("timestamp", "env", "area", "user")



class AnnouncementForm(forms.ModelForm):
    def __init__(self, set_not_required = False, *args, **kwargs):
        super(AnnouncementForm, self).__init__(*args, **kwargs)
        
        if set_not_required:
            for key in self.fields:
                self.fields[key].required = False

    def clean(self):
        cleaned_data = super(AnnouncementForm, self).clean()
        env = cleaned_data.get("env")
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
        exclude = ("timestamp", "env", "area")


class CheckinForm(forms.Form):
    area = forms.IntegerField(required = False)
    env = forms.IntegerField(required = False)
    
    def clean(self):
        cleaned_data = super(CheckinForm, self).clean()
        env = cleaned_data.get("env")
        area = cleaned_data.get("area")
    
        if env is None and area is None:
            raise forms.ValidationError("Environment and Area data are both missing. At least one is required.")
        
        if env and area and area.env != env:
            raise forms.ValidationError("Environment and Area data is contradictory." + 
                                        "Environment and Area.Environment don't match.")
        
        return cleaned_data
    

class LoginForm(forms.Form):
    email = forms.EmailField(required = True)
    password = forms.
    
    def clean(self):
        cleaned_data = super(CheckinForm, self).clean()
        env = cleaned_data.get("env")
        area = cleaned_data.get("area")
    
        if env is None and area is None:
            raise forms.ValidationError("Environment and Area data are both missing. At least one is required.")
        
        if env and area and area.env != env:
            raise forms.ValidationError("Environment and Area data is contradictory." + 
                                        "Environment and Area.Environment don't match.")
        
        return cleaned_data

     