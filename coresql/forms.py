from django import forms
from django.utils.translation import ugettext_lazy as _
from coresql.models import Environment, Area, Annotation, Announcement
import re

username_pattern = re.compile('\W+')
attrs_dict = { 'class': 'required' }

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
    virtual = forms.BooleanField(required=False)
    
    def clean(self):
        cleaned_data = super(CheckinForm, self).clean()
        environment_id = cleaned_data.get("environment")
        area_id = cleaned_data.get("area")
        
        environment_obj = None
        try:
            environment_obj = Environment.objects.get(id=environment_id)
        except:
            pass
        
        area_obj = None
        try:
            area_obj = Area.objects.get(id=area_id)
        except:
            pass
        
        if environment_obj is None and area_obj is None:
            raise forms.ValidationError("Location data is missing.")
        
        if environment_obj and area_obj and area_obj.environment != environment_obj:
            raise forms.ValidationError("Location data is inconsistent. Please repeat checkin.")
        
        return cleaned_data
    

class LoginForm(forms.Form):
    email = forms.EmailField(required = True)
    password = forms.CharField(required = True)
    
    def __init__(self, *args, **kwargs):
        self.user_cache = None
        super(LoginForm, self).__init__(*args, **kwargs)
    
    def clean(self):
        from django.contrib.auth import authenticate
        
        cleaned_data = super(LoginForm, self).clean()
        
        email = cleaned_data.get('email')
        password = cleaned_data.get('password')

        if email and password:
            self.user_cache = authenticate(email=email, password=password)
            if self.user_cache is None:
                raise forms.ValidationError(_("Please enter a correct email and password. Note that both fields are case-sensitive."))
            elif not self.user_cache.is_active:
                raise forms.ValidationError(_("This account is inactive."))
            
        return cleaned_data
    
    
    def get_user_id(self):
        if self.user_cache:
            return self.user_cache.id
        return None

    
    def get_user(self):
        return self.user_cache
    

class RegistrationForm(forms.Form):
    """
    Form for registering a new user account.   
    Subclasses should feel free to add any additional validation they
    need, but should either preserve the base ``save()`` or implement
    a ``save()`` which accepts the ``profile_callback`` keyword
    argument and passes it through to
    ``RegistrationProfile.objects.create_inactive_user()``.
    
    """
    email = forms.EmailField(required = True)
    password1 = forms.CharField(required = True)
    password2 = forms.CharField(required = True)
    
    
    def clean_email(self):
        from django.contrib.auth.models import User
        """
        Validate that the supplied email address is unique for the
        site.
        
        """
        if User.objects.filter(email__iexact=self.cleaned_data['email']):
            raise forms.ValidationError(_(u'This email address is already in use. Please supply a different email address.'))
        return self.cleaned_data['email']
    
    
    def clean(self):
        """
        Verifiy that the values entered into the two password fields
        match. Note that an error here will end up in
        ``non_field_errors()`` because it doesn't apply to a single
        field.
        
        """
        if 'password1' in self.cleaned_data and 'password2' in self.cleaned_data:
            if self.cleaned_data['password1'] != self.cleaned_data['password2']:
                raise forms.ValidationError(_(u'You must type the same password each time'))
        return self.cleaned_data
    
    
    def save(self, profile_callback=None):
        from registration.models import RegistrationProfile
        """
        Create the new ``User`` and ``RegistrationProfile``, and
        returns the ``User``.
        
        This is essentially a light wrapper around
        ``RegistrationProfile.objects.create_inactive_user()``,
        feeding it the form data and a profile callback
        
        The ``username`` is a trimmed down version of the email with all 
        non-alfanumeric characters substituted by a ``_`` 
        """
        username = self.cleaned_data['email']
        username = username.split("@")[0]
        if len(username) > 30:
            username = username[:30]
        username = username_pattern.sub('_', username)
        
        new_user = RegistrationProfile.objects.create_inactive_user(username=username,
                                                                    password=self.cleaned_data['password1'],
                                                                    email=self.cleaned_data['email'],
                                                                    profile_callback=profile_callback)
        
        return new_user
    
    
class ClientRegistrationForm(RegistrationForm):
    first_name = forms.CharField(required = True)
    last_name = forms.CharField(required = True)
    
    def save(self, profile_callback=None):
        from registration.models import RegistrationProfile
        from django.contrib.auth.models import User
        
        username = self.cleaned_data['email']
        username = username.split("@")[0]
        if len(username) > 30:
            username = username[:30]
        username = username_pattern.sub('_', username)
        
        new_user = User.objects.create_user(username, self.cleaned_data['email'], password=self.cleaned_data['password1'])
        new_user.is_active = True
        new_user.first_name = self.cleaned_data['first_name']
        new_user.last_name = self.cleaned_data['last_name']
        new_user.save()
        
        RegistrationProfile.objects.create(user=new_user, activation_key="ALREADY_ACTIVATED")
        
        return new_user
        