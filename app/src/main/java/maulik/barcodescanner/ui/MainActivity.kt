package maulik.barcodescanner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import maulik.barcodescanner.databinding.ActivityMainBinding
import android.content.BroadcastReceiver
import android.content.Context
import android.os.PersistableBundle
import android.content.IntentFilter
import android.view.View


class MainActivity : AppCompatActivity() {

    private val cameraPermissionRequestCode = 1
    private var selectedScanningSDK = BarcodeScanningActivity.ScannerSDK.ZXING;
    private lateinit var binding: ActivityMainBinding
    val eventName = "scanner_data"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        registerEventReceiver();
        startScanning();
        binding.floatingScanNow.setOnClickListener(View.OnClickListener {
            startScanning();
        });

//        binding.cardMlKit.setOnClickListener {
//            selectedScanningSDK = BarcodeScanningActivity.ScannerSDK.MLKIT
//            startScanning()
//        }
//        binding.cardZxing.setOnClickListener {
//            selectedScanningSDK = BarcodeScanningActivity.ScannerSDK.ZXING
//            startScanning()
//        }

    }

    private fun startScanning() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCameraWithScanner()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraWithScanner()
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, cameraPermissionRequestCode)
            }
        }
    }

    private fun openCameraWithScanner() {
        BarcodeScanningActivity.start(this, selectedScanningSDK)
    }

    private val eventReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            //This code will be executed when the broadcast in activity B is launched
            if (intent != null) {
                binding.tvDeveloper.setText(intent.getStringExtra("data"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private fun registerEventReceiver() {
        val eventFilter = IntentFilter()
        eventFilter.addAction(eventName)
        registerReceiver(eventReceiver, eventFilter)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraPermissionRequestCode) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openCameraWithScanner()
            }
        }
    }
}