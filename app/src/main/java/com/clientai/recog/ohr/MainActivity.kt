package com.clientai.recog.ohr

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.clientai.recog.ohr.databinding.ActivityMainBinding
import com.clientai.recog.ohr.tflite.OperatingHandClassifier
import com.clientai.recog.ohr.tracker.MotionEventTracker
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), MotionEventTracker.ITrackDataReadyListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    companion object {
        var activityRef: WeakReference<MainActivity> ? = null
    }
    var classifier: OperatingHandClassifier? = null
    var tracker: MotionEventTracker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityRef = WeakReference(this)
        UIUtils.density = resources.displayMetrics.density

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        tracker = MotionEventTracker(this)
        tracker?.checkAndInit(findViewById(R.id.debug_container), this)
        classifier = OperatingHandClassifier(this)
        classifier?.checkAndInit()
    }

    override fun onPostResume() {
        super.onPostResume()
        PermissionUtils.checkAndRequestPermission(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        ev?.let {
            tracker?.recordMotionEvent(it)
        }
        return super.dispatchTouchEvent(ev)
    }

    fun toast(tip: String) {
        binding.drawerLayout.post {
            Toast.makeText(this, tip, Toast.LENGTH_LONG).show()
        }
    }

    override fun onTrackDataReady(pointList: JSONArray) {
        classifier?.let {
            it.classifyAsync(pointList).addOnSuccessListener { result ->
                val tip = findViewById<TextView>(R.id.recognizedResult)
                tip.post {
                    tip.text = "AI识别结果：$result"
                }
                Log.d(MotionEventTracker.TAG, "classifying success. $result")
            }
            .addOnFailureListener { e ->
                Log.e(MotionEventTracker.TAG, "Error classifying.", e)
            }
        }
    }
}