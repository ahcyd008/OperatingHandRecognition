# 端智能左右手识别学习Demo

## 前言
- 为学习端智能入门课程设计的Android Demo，包括：
    - Android端：左右手识别数据采集、识别demo展示
    - Python Notebook：左右手识别模型设计、模型训练、模型转换
    - 数据集：训练集、测试集
- 本案例与 https://github.com/ahcyd008/DigitalRecognition 同属于一个系列。

- 可以用Android Studio 加载和编译此工程
- 已经编译好的app位于 [readme/app-debug.apk](readme/app-debug.apk)

- 模型训练和数据位于子目录 notebook中

## 工程介绍
### 数据采集
- 使用数据收集程序，按提示完成数据收集后，样本被保存在sdcard，可使用 adb 命令拉取到本地
```
adb pull /storage/emulated/0/Android/data/com.clientai.recog.ohr/files/Track/ ./
```
- 同时，数据也会被上次到一个云服务端，可以直接通过web服务访问下载，方面数据收集。云服务端参考 https://github.com/ahcyd008/DataTrackCacheServer ，目前为方便学习新建了临时云服务 http://129.204.41.76:8000/

- 已采集的样本位于[notebook/dataset.zip](notebook/dataset.zip) 和 [notebook/dataset-test.zip](notebook/dataset-test.zip)，解压后使用

### 训练模型

- 代码位于[notebook/ohr_recognition.ipynb](notebook/ohr_recognition.ipynb)
- 是一个python notebook程序，可以在vscode中运行

#### 安装环境
- 配置 notebook环境
> 参考 https://code.visualstudio.com/docs/datascience/jupyter-notebooks 配置vscode支持notebook开发运行 

- 配置依赖
``` bash
pip install numpy tensorflow
```

#### 模型训练
- 运行[notebook/ohr_recognition.ipynb](notebook/ohr_recognition.ipynb) notebook代码

- 训练好模型，会转成tensorflow lite模型，用于在Android Demo中使用，包括 mymodel.tflite，位于notebook目录下。

#### Android Demo应用模型
- 将自己训练好的 mymodel.tflite 模型放到Android工程的assets目录下
- 加载模型代码位于 OperatingHandClassifier#loadModelFile 方法中。
```
val fileDescriptor = assetManager.openFd(MODEL_FILE) // 使用网络模型
```
