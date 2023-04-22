#FROM tensorflow/tensorflow:2.8.0-gpu-jupyter
FROM nvidia/cuda:11.6.1-cudnn8-runtime-ubuntu20.04

ENV LANG C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive
#ENV TZ=Europe/Bratislava
#RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY . /src
WORKDIR /src
RUN apt-get update && apt-get install -y \
  openslide-tools \
  npm \
  dos2unix

RUN dos2unix /src/monailabel/scripts/monailabel

RUN apt-get install -y python3.10 \
  python3-pip

RUN pip3 --no-cache-dir install --upgrade pip
RUN pip3 --no-cache-dir install \
  setuptools \
  wheel \
  twine

RUN pip3 install --no-cache-dir -r requirements.txt
RUN pip3 install torch==1.13.1+cu116 torchvision==0.14.1+cu116 torchaudio===0.13.1+cu116 -f https://download.pytorch.org/whl/cu116/torch_stable.html
ENV PATH="${PATH}:/src/monailabel/scripts"

#EXPOSE 5555

#CMD ["monailabel", "start_server", "--app", "apps/pathology", "--studies", "datasets/"]