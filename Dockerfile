#FROM tensorflow/tensorflow:2.8.0-gpu-jupyter
FROM nvidia/cuda:11.2.2-cudnn8-runtime-ubuntu20.04

ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/Bratislava
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY . /src
WORKDIR /src
RUN apt-get update && apt-get install -y \
  openslide-tools \
  npm \
  dos2unix \
  nvidia-cuda-toolkit

RUN dos2unix /src/monailabel/scripts/monailabel

RUN apt install software-properties-common -y
RUN add-apt-repository ppa:deadsnakes/ppa -y && apt-get update
RUN apt-get install -y python3.10 \
  python3-pip

RUN pip3 --no-cache-dir install --upgrade pip
RUN pip3 --no-cache-dir install \
  setuptools \
  wheel \
  twine

RUN pip3 install torch==1.13.1+cu116 torchvision==0.14.1+cu116 torchaudio===0.13.1+cu116 -f https://download.pytorch.org/whl/cu116/torch_stable.html
RUN pip3 install --no-cache-dir -r requirements.txt
ENV PATH="${PATH}:/src/monailabel/scripts"

RUN export PATH="${PATH}:/usr/local/nvidia/bin:/usr/local/cuda/bin"

EXPOSE 8000

#CMD ["python3", "-m", "monailabel.main", "start_server", "--app", "apps/pathology", "--studies", "datasets/"]