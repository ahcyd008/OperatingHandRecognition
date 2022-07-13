package com.clientai.recog.ohr

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager

object PermissionUtils {
    fun checkAndRequestPermission(activity: Activity): Boolean {
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            return false
        }
        if (activity.checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(arrayOf(Manifest.permission.INTERNET), 100)
            return false
        }
        return true
    }
}