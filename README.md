# 端智能手写数字识别学习Demo

## 前言
- 为学习端智能入门课程设计的Android Demo，包括：
    - Android端：手写数字数据采集、识别demo展示
    - Python Notebook：手写数字识别模型设计、模型训练、模型转换
    - 数据集：训练集、测试集
- demo样例
![preview](readme/preview.png)
- 可以用Android Studio 加载和编译此工程
- 建议自己编译安装，如遇到困难可以使用已编译好的demo [readme/app-debug.apk](readme/app-debug.apk) (可直接下载安装)

- 模型训练和数据位于子目录 notebook中

## 工程介绍
### 数据采集
- 使用数据收集程序，按提示完成数据收集后，样本被保存在sdcard，可使用 adb 命令拉取到本地
```
adb pull /storage/emulated/0/Android/data/com.clientai.recog.digital/files/Track/ ./
```
- 同时，数据也会被上次到一个云服务端，可以直接通过web服务访问下载，方面数据收集。云服务端参考 https://github.com/ahcyd008/DataTrackCacheServer ，目前为方便学习新建了临时云服务 http://129.204.41.76:8000/

- 已采集的样本位于[notebook/dataset.zip](notebook/dataset.zip) 和 [notebook/dataset-test.zip](notebook/dataset-test.zip)，解压后使用

### 训练模型

- 代码位于[notebook/digital_recognition.ipynb](notebook/digital_recognition.ipynb)
- 是一个python notebook程序，可以在vscode中运行，也可以导入到在线的notebook平台（如kaggle, 本示例链接 https://www.kaggle.com/code/chenyidong1032/client-ai-digital-recog)

#### 安装环境
- 配置 notebook环境
> 参考 https://code.visualstudio.com/docs/datascience/jupyter-notebooks 配置vscode支持notebook开发运行 

- 配置依赖
``` bash
pip install matplotlib numpy Pillow tensorflow torch torchvision
```

#### 模型训练
- 运行[notebook/digital_recognition.ipynb](notebook/digital_recognition.ipynb) notebook代码，内部支持两种机器学习模型，全连接网络模型 & 卷积神经网络模型。

- 训练好模型，会转成tensorflow lite模型，用于在Android Demo中使用，包括 mymodel.tflite & mymodel-cnn.tflite，位于notebook目录下。

#### Android Demo应用模型
- 将自己训练好的 mymodel.tflite & mymodel-cnn.tflite 模型放到Android工程的assets目录下
- 加载模型代码位于 DigitalClassifier#loadModelFile 方法中，其中有两种模型加载代码，选择一种加载编译安装运行即可。
```
val fileDescriptor = assetManager.openFd(MODEL_FILE) // 使用全连接网络模型
// val fileDescriptor = assetManager.openFd(MODEL_CNN_FILE) // 使用卷积神经网络模型
```
