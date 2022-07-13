package com.clientai.recog.ohr.tracker

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.clientai.recog.ohr.JsonUtils
import com.clientai.recog.ohr.R
import org.json.JSONArray
import java.lang.Integer.max
import java.lang.Integer.min

class MotionEventTracker(var context: Context) {
    companion object {
        const val TAG = "ClientAI#tracker"
    }

    interface ITrackDataReadyListener {
        fun onTrackDataReady(dataList: JSONArray)
    }

    var records = JSONArray()
    var label = ""
    private var width = 0
    private var height = 0
    private var density = 1f
    private var listener: ITrackDataReadyListener? = null

    fun checkAndInit(trackContainer: View, listener: ITrackDataReadyListener) {
        this.listener = listener
        val metric = context.resources.displayMetrics
        width = min(metric.widthPixels, metric.heightPixels)
        height = max(metric.widthPixels, metric.heightPixels)
        density = metric.density
        setupTracker(trackContainer)
    }

    private var currentEvents: JSONArray? = null
    private var currentDownTime = 0L

    fun recordMotionEvent(ev: MotionEvent) {
        if (ev.pointerCount > 1) {
            currentEvents = null
            return
        }
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentEvents = JSONArray()
            currentDownTime = ev.eventTime
        }
        if (currentEvents != null) {
            if (ev.historySize > 0) {
                for (i in 0 until ev.historySize) {
                    currentEvents?.put(buildPoint(ev.getHistoricalX(i), ev.getHistoricalY(i), ev.getHistoricalEventTime(i)))
                }
            }
            currentEvents?.put(buildPoint(ev.x, ev.y, ev.eventTime))
        }
        if (ev.action == MotionEvent.ACTION_UP) {
            currentEvents?.let {
                if (it.length() >= 6) {
                    if (label != "") { // 数据收集
                        records.put(it)
                    }
                    listener?.onTrackDataReady(it) // 触发预测
                    Log.i(TAG, "cache events, eventCount=${it.length()}, data=$it")
                } else {
                    // 过滤点击和误触轨迹
                    Log.i(TAG, "skipped short events, eventCount=${it.length()}, data=$it")
                }
            }
            currentEvents = null
        }
    }

    private fun buildPoint(x: Float, y: Float, timestamp: Long): JSONArray {
        val point = JSONArray()
        point.put(x)
        point.put(y)
        point.put(width)
        point.put(height)
        point.put(density)
        point.put(timestamp - currentDownTime)
        return point
    }

    private fun setupTracker(trackContainer: View) {
        context = trackContainer.context.applicationContext
        Log.i(TAG, "setupOHR trackContainer: $trackContainer")
        val leftHandBtn = trackContainer.findViewById<Button>(R.id.left)
        val rightHandBtn = trackContainer.findViewById<Button>(R.id.right)
        val cleanBtn = trackContainer.findViewById<Button>(R.id.clean)
        leftHandBtn.setOnClickListener { v: View ->
            val tv = v as TextView
            if (v.getTag() == null && rightHandBtn.tag == null) { // current closed
                startTrackLabel("left")
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
                startTrackLabel("right")
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

    private fun startTrackLabel(label: String) {
        this.label = label
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
}