# Copyright (c) MONAI Consortium
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import logging
from typing import Any, Callable, Dict, Sequence
from apps.pathology.lib.infers.utils import BundleConstants, load_json
from apps.pathology.model.pathology_structure_segmentation_nestedunet.models.model import NestedUnet

import numpy as np
from apps.pathology.lib.transforms import LoadImagePatchd, PostFilterLabeld
from monai.transforms import FromMetaTensord, LoadImaged, SaveImaged, SqueezeDimd

from monailabel.interfaces.tasks.infer_v2 import InferType
from monailabel.tasks.infer.bundle import BundleInferTask
from monailabel.transform.post import FindContoursCustom, PostProcess, PostProcessAnnotations
from monailabel.transform.writer import PolygonWriter

logger = logging.getLogger(__name__)


class NestedUnetStructure(BundleInferTask):
    """
    This provides Inference Engine for pre-trained DeepLabV3+ model segmentation + Classification model.
    """
    # TODO: replace tensorflow flag by model

    def __init__(self, path: str, conf: Dict[str, str], **kwargs):
        const = BundleConstants(
            model_pytorch=[
                "1676994769.867802_U-Net++_IKEM.index",
            ],
            models=[
                NestedUnet,
            ],
            config_paths=[
                'apps/pathology/model/pathology_structure_segmentation_nestedunet/configs/config.json',
            ]

        )
        # const = BundleConstants(model_pytorch="1670105766.4486911_DeepLabV3+_IKEM.index")
        super().__init__(
            path,
            conf,
            type=InferType.SEGMENTATION,
            add_post_restore=False,
            pre_filter=[LoadImaged],
            post_filter=[FromMetaTensord, SaveImaged],
            load_strict=True,
            tensorflow=True,
            const=const,
            ** kwargs,
        )

        self._custom_config = load_json(const.config_paths()[0])

        # Override Labels
        self.labels = {
            "Blood vessels": 0,
            "Inflammation": 1,
            "Endocardium": 2,
        }
        self.label_colors = {
            "Blood vessels": (255, 0, 0),
            "Inflammation": (255, 255, 0),
            "Endocardium": (0, 0, 255),
        }
        self._config["label_colors"] = self.label_colors

    def pre_transforms(self, data=None) -> Sequence[Callable]:
        t = [LoadImagePatchd(keys="image", mode="RGB",
                             dtype=np.uint8, padding=False)]
        t.extend([x for x in super().pre_transforms(data)])
        return t

    def post_transforms(self, data=None, xml_path=None) -> Sequence[Callable]:
        t = [x for x in super().post_transforms(data)]
        t.extend(
            [
                PostFilterLabeld(keys="pred"),
                PostProcess(keys="pred"),
                PostProcessAnnotations(keys="pred", xml_path=xml_path, config=self._custom_config),
                FindContoursCustom(keys="pred", labels=self.labels),
            ]
        )
        return t

    def info(self) -> Dict[str, Any]:
        d = super().info()
        d["pathology"] = True
        return d

    def writer(self, data, extension=None, dtype=None):
        writer = PolygonWriter(label=self.output_label_key,
                               json=self.output_json_key)
        return writer(data)
