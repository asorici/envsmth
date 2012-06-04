import datetime
from haystack import indexes
from coresql.models import Environment, Area


class EnvironmentIndex(indexes.SearchIndex, indexes.Indexable):
    text = indexes.CharField(document=True, use_template=True)
    uLocStr = indexes.CharField()
    locationId = indexes.IntegerField(model_attr='id')
    locationType = indexes.CharField()
    parent = indexes.IntegerField(model_attr='parent__id', null = True)
    name = indexes.CharField(model_attr='name', boost=2.0, faceted=True)
    tags = indexes.MultiValueField(boost=2.0, faceted=True)
    description = indexes.CharField()
    timestamp = indexes.DateTimeField(model_attr='timestamp')
    
    def get_model(self):
        return Environment
    
    def prepare_uLocStr(self, obj):
        return "environment" + str(obj.id)
    
    def prepare_locationType(self, obj):
        return "environment"
    
    def prepare_tags(self, obj):
        return obj.tags.getList()
    
    def prepare_description(self, obj):
        desc = u''
        
        for feat in obj.features.all():
            if feat.category == "default":
                desc += feat.data.to_serializable()
        
        return desc

    def index_queryset(self):
        ## Used when the entire index for model is updated.
        return self.get_model().objects.filter(timestamp__lte=datetime.datetime.now())
    

class AreaIndex(indexes.SearchIndex, indexes.Indexable):
    text = indexes.CharField(document=True, use_template=True)
    uLocStr = indexes.CharField()
    locationId = indexes.IntegerField(model_attr='id')
    locationType = indexes.CharField()
    parent = indexes.IntegerField(model_attr='environment__id')
    name = indexes.CharField(model_attr='name', boost=2.0, faceted=True)
    tags = indexes.MultiValueField(boost=2.0, faceted=True)
    description = indexes.CharField()
    timestamp = indexes.DateTimeField(model_attr='timestamp')
    
    def get_model(self):
        return Area
    
    def prepare_uLocStr(self, obj):
        return "area" + str(obj.id)
    
    def prepare_locationType(self, obj):
        return "area"
    
    def prepare_tags(self, obj):
        return obj.tags.getList()
    
    def prepare_description(self, obj):
        desc = u''
        for feat in obj.features.all():
            if feat.category == "default":
                desc += feat.data.to_serializable()
        
        return desc

    def index_queryset(self):
        ## Used when the entire index for model is updated.
        return self.get_model().objects.filter(timestamp__lte=datetime.datetime.now())
