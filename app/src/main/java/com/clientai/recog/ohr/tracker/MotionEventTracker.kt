package com.clientai.recog.ohr.tracker

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.clientai.recog.ohr.JsonUtils
import com.clientai.recog.ohr.R
import com.clientai.recog.ohr.tflite.OperatingHandClassifier
import org.json.JSONArray
import java.lang.Integer.max
import java.lang.Integer.min

object MotionEventTracker {
    private const val TAG = "ClientAI#tracker"
    var records = JSONArray()
    var label = ""
    private var width = 0
    private var height = 0
    private var density = 1f
    private var context: Context? = null

    private fun startTrack(label: String) {
        val context = this.context ?: return
        this.label = label
        val metric = context.resources.displayMetrics
        width = min(metric.widthPixels, metric.heightPixels)
        height = max(metric.widthPixels, metric.heightPixels)
        density = metric.density
    }

    private fun saveData() {
        val context = this.context ?: return
        if (label != "" && records.length() > 0) {
            JsonUtils.saveJson(records, label, context)
        }
        clean()
    }

    private fun clean() {
        records = JSONArray()
        label = ""
    }

    private var currentEvents: JSONArray? = null
    private var currentEventCount = 0
    private var currentDownTime = 0L
    fun recordMotionEvent(ev: MotionEvent) {
        if (label == "") {
            return
        }
        if (ev.pointerCount > 1) {
            currentEvents = null
            return
        }
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentEvents = JSONArray()
            currentEventCount = 0
            currentDownTime = ev.eventTime
        }

        if (currentEvents != null) {
            if (ev.historySize > 0) {
                for (i in 0 until ev.historySize) {
                    val x = ev.getHistoricalX(i)
                    val y = ev.getHistoricalY(i)
                    val timestamp = ev.getHistoricalEventTime(i)
                    val point = JSONArray()
                    point.put(x)
                    point.put(y)
                    point.put(width)
                    point.put(height)
                    point.put(density)
                    point.put(timestamp - currentDownTime)
                    currentEventCount++
                    currentEvents?.put(point)
                }
            }
            val x = ev.x
            val y = ev.y
            val timestamp = ev.eventTime
            val point = JSONArray()
            point.put(x)
            point.put(y)
            point.put(width)
            point.put(height)
            point.put(density)
            point.put(timestamp - currentDownTime)
            currentEventCount++
            currentEvents?.put(point)
        }

        if (ev.action == MotionEvent.ACTION_UP && currentEvents != null) {
            if (currentEventCount >= 6) {
                currentEvents?.let {
                    records.put(it)
                    checkAndRecognition(it)
                }
                Log.i(TAG, "cache events, eventCount=$currentEventCount, data=$currentEvents")
            } else {
                // 过滤点击和误触轨迹
                Log.i(TAG, "skipped short events, eventCount=$currentEventCount, data=$currentEvents")
            }
            currentEvents = null
        }
    }

    fun setupTracker(ohrContainer: View) {
        context = ohrContainer.context.applicationContext
        classifier = OperatingHandClassifier(context!!)
        classifier?.checkAndInit()
        Log.i(TAG, "setupOHR ohrContainer: $ohrContainer")
        val leftHandBtn = ohrContainer.findViewById<Button>(R.id.left)
        val rightHandBtn = ohrContainer.findViewById<Button>(R.id.right)
        val cleanBtn = ohrContainer.findViewById<Button>(R.id.clean)
        leftHandBtn.setOnClickListener { v: View ->
            val tv = v as TextView
            if (v.getTag() == null && rightHandBtn.tag == null) { // current closed
                startTrack("left")
                tv.text = "左手录制中"
                v.setTag(1)
                v.isEnabled = false
                rightHandBtn.tag = null
                rightHandBtn.text = "停止录制并上报"
            } else {
                saveData()
                tv.text = "开启左手录制"
                v.setTag(null)
                rightHandBtn.isEnabled = true
                rightHandBtn.tag = null
                rightHandBtn.text = "开启右手录制"
            }
        }
        rightHandBtn.setOnClickListener { v: View ->
            val tv = v as TextView
            if (v.getTag() == null && leftHandBtn.tag == null) { // current closed
                startTrack("right")
                tv.text = "右手录制中"
                v.setTag(1)
                v.isEnabled = false
                leftHandBtn.tag = null
                leftHandBtn.text = "停止录制并上报"
            } else {
                saveData()
                tv.text = "开启右手录制"
                v.setTag(null)
                leftHandBtn.isEnabled = true
                leftHandBtn.tag = null
                leftHandBtn.text = "开启左手录制"
            }
        }
        cleanBtn.setOnClickListener { v: View? ->  // clean
            leftHandBtn.tag = null
            leftHandBtn.text = "开启左手录制"
            leftHandBtn.isEnabled = true
            rightHandBtn.tag = null
            rightHandBtn.text = "开启右手录制"
            rightHandBtn.isEnabled = true
            clean()
        }
    }

    const val sampleCount = 9
    const val tensorSize = 6
    var classifier: OperatingHandClassifier? = null
    private fun checkAndRecognition(dataList: JSONArray) {
        if (dataList.length() < sampleCount) {
            Log.d(TAG, "sample not enough, dataList.size=${dataList.length()}")
            return
        }
        val step = dataList.length().toFloat() / sampleCount
        val inputBuffer = inputBuffer
        for (i in 0 until sampleCount) {
            val e = dataList[(i * step).toInt()] as JSONArray
            inputBuffer[i * tensorSize] = (e[0] as Number).toFloat() // x
            inputBuffer[i * tensorSize + 1] = (e[1] as Number).toFloat() // y
            inputBuffer[i * tensorSize + 2] = (e[2] as Number).toFloat() // w
            inputBuffer[i * tensorSize + 3] = (e[3] as Number).toFloat() // h
            inputBuffer[i * tensorSize + 4] = (e[4] as Number).toFloat() // density
            inputBuffer[i * tensorSize + 5] = (e[5] as Number).toFloat() // dtime
        }
        classifier?.let {
            it.classifyAsync(inputBuffer).addOnSuccessListener { result ->
                Log.d(TAG, "classifying success. $result")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error classifying.", e)
            }
        }
    }

    private var mInputBuffer: FloatArray? = null
    private val inputBuffer: FloatArray get() {
        val buffer = mInputBuffer
        val size = sampleCount * tensorSize
        if (buffer != null && buffer.size == size) {
            return buffer
        }
        val tmp = FloatArray(size)
        mInputBuffer = tmp
        return tmp
    }
}