import numpy as np
import os
import shutil
import json
import torch


def custom_infere(data, configs, paths, xml_path):
    directory = './tmp'
    if os.path.exists(directory):
        shutil.rmtree(directory)
    os.mkdir(directory)
    json.dump(configs, open(f'{directory}/configs.json', 'w'))
    json.dump(paths, open(f'{directory}/paths.json', 'w'))
    np.save(f'{directory}/data.npy', data)

    del data
    torch.cuda.empty_cache()

    result = os.system(
        f'python3 -m apps.pathology.lib.infers.infere --directory "{directory}" --xml_path "./datasets/labels/final/{xml_path}"')

    patch_name = xml_path.split('/')[-1].split('.')[0]
    if os.path.exists(f'./datasets/{patch_name}.png'):
        os.remove(f'./datasets/{patch_name}.png')

    try:
        mask = np.load(f'{directory}/mask.npy')
        shutil.rmtree(directory)
    except Exception as e:
        print(e)
        shutil.rmtree(directory)
        raise Exception('Error with inference.')

    return mask
