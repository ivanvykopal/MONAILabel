from apps.pathology.model.pathology_structure_segmentation_deeplabv3plus.models.model import DeepLabV3Plus
from apps.pathology.model.pathology_structure_segmentation_nestedunet.models.model import NestedUnet
from apps.pathology.model.pathology_srel_segmentation.models.model import UNet


class InfereConfig:
    def __init__(self):
        self.models = {
            "deeplabv3+": DeepLabV3Plus,
            "u-net++": NestedUnet,
            "u-net": UNet,
        }

    def get_model(self, model_name):
        model_name = model_name.lower()
        if model_name in self.models:
            return self.models[model_name]
        return None
