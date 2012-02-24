from functools import wraps
from django.contrib.auth import login, authenticate, SESSION_KEY
from django.conf import settings
from django.http import HttpResponseRedirect
import uuid

USER_AGENT_BLACKLIST = []


def allow_anonymous_profile(func):
    """
    adapted from lazysignup: https://github.com/danfairs/django-lazysignup.git
    """
    
    def wrapped(request, *args, **kwargs):
        # If the user agent is one we ignore, bail early
        ignore = False
        request_user_agent = request.META.get('HTTP_USER_AGENT', '')
        for user_agent in USER_AGENT_BLACKLIST:
            if user_agent.search(request_user_agent):
                ignore = True
                break

        # If there's already a valid user (returned either by session or by auth-key middleware), then
        # we don't need to do anything. If the user isn't valid, then
        # request.user will return an anonymous user
        if request.user.is_anonymous() and not ignore:
            # If not, then we have to create a temporary user
            user, username = create_anonymous_user_profile()
            request.user = None
            user = authenticate(username=username)
            
            # Set the user id in the session here to prevent the login
            # call cycling the session key.
            request.session[SESSION_KEY] = user.id
            login(request, user)
            
        return func(request, *args, **kwargs)

    return wraps(func)(wrapped)


def secure_required(view_func):
    """Decorator makes sure URL is accessed over https."""
    def _wrapped_view_func(request, *args, **kwargs):
        if not request.is_secure():
            if getattr(settings, 'HTTPS_SUPPORT', True):
                request_url = request.build_absolute_uri(request.get_full_path())
                secure_url = request_url.replace('http://', 'https://')
                return HttpResponseRedirect(secure_url)
        return view_func(request, *args, **kwargs)
    return _wrapped_view_func


def create_anonymous_user_profile():
    from django.contrib.auth.models import User
    
    ## generate a new username
    ##max_length = User._meta.get_field('username').max_length
    max_length = 30
    username = uuid.uuid4().hex[:max_length]

    ## create a dummy user with the above username and no password
    user = User.objects.create_user(username, '')
    ## a profile will have also been created => mark it's anonymous attribute as true
    user_profile = user.get_profile()
    user_profile.is_anonymous = True
    user_profile.save() 
    
    return (user, username)