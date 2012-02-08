from django.contrib.auth.backends import ModelBackend
from django.contrib.auth.models import User

class AnonymousProfileBackend(ModelBackend):
    """
    adapted from lazysignup: https://github.com/danfairs/django-lazysignup.git
    
    creates a backend to authenticate anonymous users solely based on their "dummy" name
    """
    def authenticate(self, username=None):
        try:
            return User.objects.get(username=username)
        except User.DoesNotExist:
            return None

    def get_user(self, user_id):
        # Annotate the user with our backend so it's always available,
        # not just when authenticate() has been called.
        try:
            user = User.objects.get(pk=user_id)
        except User.DoesNotExist:
            user = None
        else:
            user.backend = 'client.backends.AnonymousProfileBackend'
        return user