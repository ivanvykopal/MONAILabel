@echo off

REM Check if the image name and version arguments are provided
IF "%~1"=="" (
    echo Usage: %~nx0 ^<image_name^> ^<version^>
    exit /b 1
)

SET "IMAGE_NAME=%~1"
SET "VERSION=%~2"
SET "SOURCE_DIR=.\apps\pathology\model"

REM Define the MODELS array with the source and destination paths for each model
SET "MODELS=pathology_srel_segmentation\models pathology_structure_segmentation_deeplabv3plus\models pathology_structure_segmentation_nestedunet\models"

REM Create and start a Docker container
FOR /F %%i IN ('docker run --gpus all -ti -p 8000:8000 -d "%IMAGE_NAME%:%VERSION%"') DO SET "CONTAINER_ID=%%i"

REM Check if the container was created successfully
IF "%CONTAINER_ID%"=="" (
    echo Failed to create or start the Docker container.
    exit /b 1
)

REM Copy files to the Docker container
FOR %%m IN (%MODELS%) DO (
    echo Copying files from "%SOURCE_DIR%\%%m\." to "%CONTAINER_ID%:\src\apps\pathology\model\%%m"
    docker cp "%SOURCE_DIR%\%%m\." "%CONTAINER_ID%:\src\apps\pathology\model\%%m"
)
REM Commit the changes made in the container to a new Docker image with the specified version tag
docker commit "%CONTAINER_ID%" "%IMAGE_NAME%:%VERSION%"
docker commit "%CONTAINER_ID%" "%IMAGE_NAME%:latest"

REM Push the new image with the specified version tag to the Docker registry (you might need to log in first)
docker push "%IMAGE_NAME%:%VERSION%"
docker push "%IMAGE_NAME%:latest"

REM Remove both the specific version tag and the 'latest' tag from the local machine after they have been successfully pushed
docker rmi "%IMAGE_NAME%:%VERSION%" "%IMAGE_NAME%:latest"

REM Stop and remove the Docker container
docker stop "%CONTAINER_ID%" >nul
docker rm "%CONTAINER_ID%" >nul

echo Script execution completed. Docker container stopped and removed.
