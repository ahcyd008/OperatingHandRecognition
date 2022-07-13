package com.clientai.recog.ohr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat


object JsonUtils {
    private const val TAG = "ClientAI#json"

    fun saveJson(json: JSONArray, label: String, context: Context): Boolean {
        val saveDir = context.getExternalFilesDir("Track/${label}") ?: return false
        if (!saveDir.exists()) {
            val result = saveDir.mkdirs()
            Log.d(TAG, "saveJson mkdirs result=$result")
        }
        val now = System.currentTimeMillis()
        val timeString = SimpleDateFormat("yyyy_MM_dd_hh_mm_ss").format(now)
        val name = "${timeString}_${now%1000}.json"
        val saveFile = File(saveDir.absolutePath, name)
        try {
            val out = FileOutputStream(saveFile)
            val writer = OutputStreamWriter(out)
            writer.write(json.toString())
            writer.close()
            Log.d(TAG, "saveJson success, path=${saveFile.absolutePath}")
            UploadUtils.uploadFile(saveFile.absolutePath, "$label/$name") // 上传到服务端
            return true
        } catch (ex: IOException) {
            Log.e(TAG, "saveJson error!", ex)
        }
        return false
    }
}