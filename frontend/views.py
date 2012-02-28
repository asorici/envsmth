from django_facebook.utils import next_redirect
from django.contrib.auth.decorators import login_required
from coresql.models import Environment

@login_required
def dashboard(request):
    """ 
    Gives the view of dashboard. Upper part with creation button and descriptive text, 
    as well as the list of own environments
    """
    
    ## get the user's environments
    user_profile = request.user.get_profile()
    user_environments = Environment.objects.filter(owner=user_profile)
    
    
    pass


def recent_news(request):
    """
    lists the newsfeed of recently created environments and areas
    """
    pass


