package com.clientai.recog.ohr.tflite

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.json.JSONArray
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OperatingHandClassifier(private val context: Context) {
    private var interpreter: Interpreter? = null
    private var modelInputSize = 0
    var isInitialized = false
        private set

    /** Executor to run inference task in the background */
    private val executorService: ExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var hasInit = false
    fun checkAndInit() {
        if (hasInit) {
            return
        }
        hasInit = true
        val task = TaskCompletionSource<Void?>()
        executorService.execute {
            try {
                initializeInterpreter()
                task.setResult(null)
            } catch (e: IOException) {
                task.setException(e)
            }
        }
        task.task.addOnFailureListener { e -> Log.e(TAG, "Error to setting up digit classifier.", e) }
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {
        // Load the TF Lite model
        val assetManager = context.assets
        val model = loadModelFile(assetManager)

        // Initialize TF Lite Interpreter with NNAPI enabled
        val options = Interpreter.Options()
        options.useNNAPI = true
        val interpreter = Interpreter(model, options)

        // Read input shape from model file
        val inputShape = interpreter.getInputTensor(0).shape()
        val simpleCount = inputShape[1]
        val tensorSize = inputShape[2]
        modelInputSize = FLOAT_TYPE_SIZE * simpleCount * tensorSize * PIXEL_SIZE
        val outputShape = interpreter.getOutputTensor(0).shape()

        // Finish interpreter initialization
        this.interpreter = interpreter
        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter. inputShape:${Arrays.toString(inputShape)}, outputShape:${Arrays.toString(outputShape)}")
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager): ByteBuffer {
        val fileDescriptor = assetManager.openFd(MODEL_FILE) // 使用全连接网络模型
        // val fileDescriptor = assetManager.openFd(MODEL_CNN_FILE) // 使用卷积神经网络模型
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun classify(pointList: JSONArray): ClassifierLabelResult {
        if (!isInitialized) {
            throw IllegalStateException("TF Lite Interpreter is not initialized yet.")
        }
        try {
            // Preprocessing: resize the input
            var startTime: Long = System.nanoTime()
            val byteBuffer = convertFloatArrayToByteBuffer(pointList)
            var elapsedTime = (System.nanoTime() - startTime) / 1000000
            Log.d(TAG, "Preprocessing time = " + elapsedTime + "ms")

            startTime = System.nanoTime()
            val result = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }
            interpreter?.run(byteBuffer, result)
            elapsedTime = (System.nanoTime() - startTime) / 1000000
            Log.d(TAG, "Inference time = " + elapsedTime + "ms result=" + result[0].contentToString())

            // return top 4
            val output = result[0][0]
            return if (output > 0.5f) {
                ClassifierLabelResult(output, "right")
            } else {
                ClassifierLabelResult(1.0f-output, "left")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Inference error", e)
        }
        return ClassifierLabelResult(-1f, "unknown")
    }

    fun classifyAsync(pointList: JSONArray): Task<ClassifierLabelResult> {
        val task = TaskCompletionSource<ClassifierLabelResult>()
        executorService.execute {
            val result = classify(pointList)
            task.setResult(result)
        }
        return task.task
    }

    fun close() {
        executorService.execute {
            interpreter?.close()
            Log.d(TAG, "Closed TFLite interpreter.")
        }
    }

    private fun convertFloatArrayToByteBuffer(pointList: JSONArray): ByteBuffer {
        Log.d(TAG, "convertFloatArrayToByteBuffer pointList=$pointList")
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        val step = pointList.length().toFloat() / sampleCount
        for (i in 0 until sampleCount) {
            val e = pointList[(i * step).toInt()] as JSONArray
            for (j in 0 until tensorSize) {
                val value = (e[j] as Number).toFloat() // x y w h density dtime
                byteBuffer.putFloat(value)
            }
        }
        return byteBuffer
    }

    companion object {
        private const val TAG = "ClientAI#Classifier"
        private const val MODEL_FILE = "mymodel.tflite"
        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1
        private const val OUTPUT_CLASSES_COUNT = 1

        const val sampleCount = 9
        const val tensorSize = 6
    }
}

class ClassifierLabelResult(var score: Float, var label: String) {
    override fun toString(): String {
        val format = DecimalFormat("#.##")
        return "$label score:${format.format(score)}"
    }
}