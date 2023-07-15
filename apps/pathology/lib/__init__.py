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

import ctypes.util
import platform
from ctypes import cdll
import os
os.environ['PATH'] = "C:\\Windows\\System32" + ";" + \
    os.environ['PATH']  # libvips-bin-path is where you save the libvips files

# For windows (preload openslide dll using file_library) https://github.com/openslide/openslide-python/pull/151
if platform.system() == "Windows":
    cdll.LoadLibrary(str(ctypes.util.find_library("libopenslide-0.dll")))
