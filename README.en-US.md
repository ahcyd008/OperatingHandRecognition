

# Edge AI Left/Right Hand Recognition Learning Demo

## Preface
- An Android Demo designed for learning introductory Edge AI courses, including:
    - Android side: Data collection for left/right hand recognition, recognition demo display
    - Python Notebook: Model design, model training, and model conversion for left/right hand recognition
    - Dataset: Training set, test set
- This case belongs to the same series as https://github.com/ahcyd008/DigitalRecognition.

- This project can be loaded and compiled with Android Studio.
- The pre-compiled app is located at [readme/app-debug.apk](readme/app-debug.apk)

- Model training and data are located in the `notebook` subdirectory.

## Project Overview
### Data Collection
- Using the data collection program, after completing data collection as prompted, samples are saved to the SD card. You can pull them to your local machine using the `adb` command:
```
adb pull /storage/emulated/0/Android/data/com.clientai.recog.ohr/files/Track/ ./
```
- Meanwhile, the data will also be uploaded to a cloud service. You can directly access and download it via the web service to facilitate data collection. For the cloud service reference, see https://github.com/ahcyd008/DataTrackCacheServer. Currently, a temporary cloud service has been set up for learning purposes: http://129.204.41.76:8000/

- The collected samples are located at [notebook/dataset.zip](notebook/dataset.zip) and [notebook/dataset-test.zip](notebook/dataset-test.zip). Extract them for use.

### Training the Model

- The code is located at [notebook/ohr_recognition.ipynb](notebook/ohr_recognition.ipynb)
- It is a Python Notebook program that can be run in VS Code.

#### Environment Setup
- Configure the Notebook environment
> Refer to https://code.visualstudio.com/docs/datascience/jupyter-notebooks to configure VS Code for Notebook development and execution.

- Install dependencies
``` bash
pip install numpy tensorflow
```

#### Model Training
- Run the Notebook code at [notebook/ohr_recognition.ipynb](notebook/ohr_recognition.ipynb)

- After training, the model will be converted to a TensorFlow Lite model for use in the Android Demo. It includes `mymodel.tflite`, located in the `notebook` directory.

#### Applying the Model in the Android Demo
- Place your trained `mymodel.tflite` model into the `assets` directory of the Android project.
- The model loading code is located in the `OperatingHandClassifier#loadModelFile` method.
```
val fileDescriptor = assetManager.openFd(MODEL_FILE) // Use remote model
```
