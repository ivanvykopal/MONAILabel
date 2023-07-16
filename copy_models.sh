#!/bin/bash

echo "Creating"

# Check if the image name and version arguments are provided
if [ -z "$1" ] || [ -z "$2" ]; then
  echo "Usage: $0 <image_name> <version>"
  exit 1
fi

IMAGE_NAME="$1"
VERSION="$2"
SOURCE_DIR="./apps/pathology/model"

# Define an array with the source and destination paths for each model
MODELS=("pathology_srel_segmentation/models" "pathology_structure_segmentation_deeplabv3plus/models" "pathology_structure_segmentation_nestedunet/models")

# Create and start a Docker container
CONTAINER_ID=$(docker run --gpus all -ti -p 8000:8000 -d "$IMAGE_NAME:$VERSION")

# Check if the container was created successfully
if [ -z "$CONTAINER_ID" ]; then
  echo "Failed to create or start the Docker container."
  exit 1
fi

# Function to clean up and exit the script gracefully
cleanup_and_exit() {
  # Stop and remove the Docker container
  docker stop "$CONTAINER_ID" &> /dev/null
  docker rm "$CONTAINER_ID" &> /dev/null

  echo "Script execution completed. Docker container stopped and removed."
  exit 0
}

# Set up the trap to call the cleanup_and_exit function when the script exits
trap cleanup_and_exit EXIT

# Loop through the MODELS array and copy files to the Docker container
for MODEL_PATH in "${MODELS[@]}"; do
  SRC_PATH="$SOURCE_DIR/$MODEL_PATH/."
  DEST_PATH="/src/apps/pathology/model/$MODEL_PATH"

  docker cp "$SRC_PATH" "$CONTAINER_ID:$DEST_PATH"
done

# Commit the changes made in the container to a new Docker image with the specified version tag
docker commit "$CONTAINER_ID" "$IMAGE_NAME:$VERSION"
docker commit "$CONTAINER_ID" "$IMAGE_NAME:latest"

# Push the new image with the specified version tag to the Docker registry (you might need to log in first)
docker push "$IMAGE_NAME:$VERSION"
docker push "$IMAGE_NAME:latest"

# Call the cleanup_and_exit function to stop and remove the Docker container
cleanup_and_exit

# Remove both the specific version tag and the 'latest' tag from the local machine after they have been successfully pushed
docker rmi "$IMAGE_NAME:$VERSION" "$IMAGE_NAME:latest"
