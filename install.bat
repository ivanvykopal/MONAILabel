@echo off

rem Check if Conda is installed
conda -h >nul 2>&1
if %errorlevel% equ 0 (
    echo Conda is installed.
    
    rem Create a Conda environment with the specified name and Python version
    call conda create --name monailabel python=3.9.16
    echo Conda environment monailabel created.
    
    rem Activate the Conda environment
    call conda activate monailabel
    echo Conda environment monailabel activated.
) else (
    echo Conda is not installed.
    
    call python -m ensurepip
    call python -m pip install virtualenv
    
    rem Create a virtual environment with the specified name and Python version
    call virtualenv .venv/monailabel --python=python3.9.16
    echo Virtual environment monailabel created.
    
    rem Activate the virtual environment
    call .venv/monailabel/Scripts/activate
    echo Virtual environment monailabel activated.
)

rem Install packages from requirements.txt using pip
call pip install -r requirements.txt
echo Packages installed.

rem Start your application or script here
