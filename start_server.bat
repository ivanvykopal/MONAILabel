@echo off

rem Change the name of the conda environment to activate
set "conda_env_name=monailabel"

rem Check if the conda environment exists
call conda info --envs | findstr /i /c:"%conda_env_name%" >nul
if errorlevel 1 (
    echo Conda environment %conda_env_name% does not exist.
    
    rem Activate Python virtual environment
    call .venv/monailabel/Scripts/activate
    echo Python virtual environment activated.
) else (
    rem Activate the conda environment
    call conda activate %conda_env_name%
    echo Conda environment %conda_env_name% activated.
)

rem Start the server
call monailabel/scripts/monailabel start_server --app apps/pathology --studies datasets/
