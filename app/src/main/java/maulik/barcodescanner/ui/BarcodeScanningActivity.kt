package maulik.barcodescanner.ui

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import maulik.barcodescanner.R
import maulik.barcodescanner.analyzer.MLKitBarcodeAnalyzer
import maulik.barcodescanner.analyzer.ScanningResultListener
import maulik.barcodescanner.analyzer.ZXingBarcodeAnalyzer
import maulik.barcodescanner.databinding.ActivityBarcodeScanningBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val ARG_SCANNING_SDK = "scanning_SDK"

class BarcodeScanningActivity : AppCompatActivity() {
    private val TAG = "BarcodeScanningActivity"

    companion object {
        @JvmStatic
        fun start(context: Context, scannerSDK: ScannerSDK) {
            val starter = Intent(context, BarcodeScanningActivity::class.java).apply {
                putExtra(ARG_SCANNING_SDK, scannerSDK)
            }
            context.startActivity(starter)
        }
    }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var binding: ActivityBarcodeScanningBinding

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService
    private var flashEnabled = false
    private var scannerSDK: ScannerSDK = ScannerSDK.ZXING //default is MLKit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarcodeScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scannerSDK = intent?.getSerializableExtra(ARG_SCANNING_SDK) as ScannerSDK

        when (scannerSDK) {
            ScannerSDK.MLKIT -> binding.ivScannerLogo.setImageResource(R.drawable.mlkit_icon)
            ScannerSDK.ZXING -> binding.ivScannerLogo.setImageResource(R.drawable.zxing)
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
            enterPipMode()
        }, ContextCompat.getMainExecutor(this))

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
    }

//    override fun onUserLeaveHint() {
//        enterPipMode()
//    }


    private fun enterPipMode() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(250,200))
                .build();



            enterPictureInPictureMode(params)
        }
    }


//    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean,newConfig: Configuration) {
//        super.onPictureInPictureModeChanged(isInPictureInPictureMode , newConfig)
//        if (!isInPictureInPictureMode) {
//
//            var intent = Intent(Intent(this, javaClass));
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//            application.startActivity(
//                intent
//            )
//        }
//    }

    private fun launchEvent(data: String) {
        val eventName = "scanner_data"
        val eventIntent = Intent(eventName)
        eventIntent.putExtra("data", data);
        this.sendBroadcast(eventIntent)
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {

        if (isDestroyed || isFinishing) {
            //This check is to avoid an exception when trying to re-bind use cases but user closes the activity.
            //java.lang.IllegalArgumentException: Trying to create use case mediator with destroyed lifecycle.
            return
        }

        cameraProvider?.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(binding.cameraPreview.width, binding.cameraPreview.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val orientationEventListener = object : OrientationEventListener(this as Context) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                val rotation: Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalysis.targetRotation = rotation
            }
        }
        orientationEventListener.enable()

        //switch the analyzers here, i.e. MLKitBarcodeAnalyzer, ZXingBarcodeAnalyzer
        class ScanningListener : ScanningResultListener {
            override fun onScanned(result: String) {
                runOnUiThread {
                    imageAnalysis.clearAnalyzer()
                    cameraProvider?.unbindAll()
                    print("Result" + result);
                    Log.d(TAG, "onScanned: " + result);
//                    Toast.makeText(applicationContext, "" + result, Toast.LENGTH_LONG).show()
                    launchEvent(result);
                    Log.d(TAG, "onScanned: ")

                    bindPreview(cameraProvider)

//                    ScannerResultDialog.newInstance(
//                        result,
//                        object : ScannerResultDialog.DialogDismissListener {
//                            override fun onDismiss() {
//                                bindPreview(cameraProvider)
//                            }
//                        })
//                        .show(supportFragmentManager, ScannerResultDialog::class.java.simpleName)
                }
            }
        }

        var analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer(ScanningListener())

        if (scannerSDK == ScannerSDK.ZXING) {
            analyzer = ZXingBarcodeAnalyzer(ScanningListener())
        }

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        val camera =
            cameraProvider?.bindToLifecycle(this, cameraSelector, imageAnalysis, preview)

        if (camera?.cameraInfo?.hasFlashUnit() == true) {
            binding.ivFlashControl.visibility = View.VISIBLE

            binding.ivFlashControl.setOnClickListener {
                camera.cameraControl.enableTorch(!flashEnabled)
            }

            camera.cameraInfo.torchState.observe(this) {
                it?.let { torchState ->
                    if (torchState == TorchState.ON) {
                        flashEnabled = true
                        binding.ivFlashControl.setImageResource(R.drawable.ic_round_flash_on)
                    } else {
                        flashEnabled = false
                        binding.ivFlashControl.setImageResource(R.drawable.ic_round_flash_off)
                    }
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down our background executor
        cameraExecutor.shutdown()
    }

    enum class ScannerSDK {
        MLKIT,
        ZXING
    }
}