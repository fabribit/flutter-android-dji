package com.dji.sdk.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.secneo.sdk.Helper
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.common.mission.waypoint.WaypointMissionDownloadEvent
import dji.common.mission.waypoint.WaypointMissionExecutionEvent
import dji.common.mission.waypoint.WaypointMissionUploadEvent
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.java.name

        private val REQUIRED_PERMISSION_LIST = arrayOf(
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE)

        private const val REQUEST_PERMISSION_CODE = 12345
    }

    private val missingPermission: MutableList<String> = ArrayList()
    private val isRegistrationInProgress = AtomicBoolean(false)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Initialize DJI SDK Manager
        checkAndRequestPermissions()
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private fun checkAndRequestPermissions() {
        Log.i(TAG, "Checking and requesting permissions")
        // Check for permissions
        for (eachPermission in REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission $eachPermission not granted")
                missingPermission.add(eachPermission)
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            initialize()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            showToast("Need to grant the permissions!")
            if (missingPermission.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, missingPermission.toTypedArray(), REQUEST_PERMISSION_CODE)
            }
        } else {
            showToast("Need to grant the permissions!")
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (i in grantResults.indices.reversed()) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i])
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            initialize()
        } else {
            showToast("Missing permissions!!!")
        }
    }

    private fun initialize() {
        showToast("Initializing SDK")
        Helper.install(application)
        if (isRegistrationInProgress.compareAndSet(false, true) && DJISDKManager.getInstance() != null) {
            AsyncTask.execute {
                showToast("Registering...")
                DJISDKManager.getInstance().registerApp(this@MainActivity.applicationContext, object : SDKManagerCallback {
                    override fun onRegister(djiError: DJIError) {
                        if (djiError === DJISDKError.REGISTRATION_SUCCESS) {
                            showToast("Register Success")
                            val listener: WaypointMissionOperatorListener = object : WaypointMissionOperatorListener {
                                override fun onDownloadUpdate(waypointMissionDownloadEvent: WaypointMissionDownloadEvent) {}
                                override fun onUploadUpdate(waypointMissionUploadEvent: WaypointMissionUploadEvent) {}
                                override fun onExecutionUpdate(waypointMissionExecutionEvent: WaypointMissionExecutionEvent) {}
                                override fun onExecutionStart() {}
                                override fun onExecutionFinish(djiError: DJIError) {}
                            }
                            DJISDKManager.getInstance().missionControl.waypointMissionOperator.addListener(listener)
                            DJISDKManager.getInstance().startConnectionToProduct()
                        } else {
                            showToast("Register sdk fails, please check the bundle id and network connection!")
                            Log.v(TAG, djiError.description)
                        }
                    }

                    override fun onProductDisconnect() {}
                    override fun onProductConnect(baseProduct: BaseProduct) {}
                    override fun onProductChanged(baseProduct: BaseProduct) {}
                    override fun onComponentChange(componentKey: ComponentKey, oldComponent: BaseComponent, newComponent: BaseComponent) {}
                    override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
                    override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
                })
            }
        } else {
            Log.i(TAG, "Registration could not be started")
        }
    }

    private fun showToast(toastMsg: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Log.i(TAG, toastMsg)
            Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show()
        }
    }

}