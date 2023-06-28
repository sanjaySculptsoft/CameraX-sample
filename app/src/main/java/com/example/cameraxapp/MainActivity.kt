package com.example.cameraxapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxapp.activities.BaseActivity
import com.example.cameraxapp.databinding.ActivityMainBinding
import com.example.cameraxapp.extension.playCaptureSound
import com.example.cameraxapp.util.BitmapUtils
import com.example.cameraxapp.util.afterMeasured
import com.example.cameraxapp.util.getOutputDirectory
import com.example.cameraxapp.extension.saveImageToGallery
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//add permission in manifest
//<uses-feature android:name="android.hardware.camera.any" /> // // it makes sure that device has camera
//<uses-permission android:name="android.permission.CAMERA" />
class MainActivity : BaseActivity() {
    var binding: ActivityMainBinding? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    var camera: Camera? = null
    private var flash: Boolean? = null

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun initArguments() {
        // status bar transparent
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

//        implement the outputDirectory and cameraExecutor
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun initViews() {
    }

    override fun setupListener() {

        binding?.apply {

        }

        /*binding?.flash?.setOnClickListener {
            // Get a cameraControl instance
            val cameraControl = camera?.cameraControl

            if(binding?.flash?.isSelected == true){
                flash = false
                binding?.flash?.isSelected = false
            }else{
                flash = true
                binding?.flash?.isSelected = true
            }
            // Call enableTorch(), you can listen to the result to check whether it was successful
//            cameraControl?.enableTorch(true) // enable torch
//            cameraControl?.enableTorch(false) // disbale torch
        } */
        // to take photo
        binding?.cameraCaptureButton?.setOnClickListener {
            takePhoto()
        }
//        binding?.cameraSwitch?.setOnClickListener {
//            flipCamera()
//        }
    }

    override fun loadData() {
        startFocusAndMetering()
        // Request Camera Permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun startFocusAndMetering() {
        binding?.viewFinderPreview?.afterMeasured {
            binding?.viewFinderPreview?.setOnTouchListener { _, event ->
                return@setOnTouchListener when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        val width = binding?.viewFinderPreview?.width?.toFloat()
                        val height = binding?.viewFinderPreview?.height?.toFloat()
                        var factory: MeteringPointFactory? = null
                        if (width != null && height != null) {
                            factory = SurfaceOrientedMeteringPointFactory(width, height)
                        }

                        // to create metering point from cordinates
                        val autoFocusPoint = factory?.createPoint(event.x, event.y)
                        try {
                            // create metering Action from metering point
                            autoFocusPoint?.let {
                                FocusMeteringAction.Builder(it, FocusMeteringAction.FLAG_AE)
                                    .apply {
                                        //focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                            }?.let {
                                camera?.cameraControl?.startFocusAndMetering(
                                    it
                                )
                            }
                        } catch (e: CameraInfoUnavailableException) {
                            Log.d("ERROR", "cannot access camera", e)
                        }
                        true
                    }

                    else -> false // Unhandled event.
                }
            }
        }

    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        init()

    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private fun flipCamera() {
        if (lensFacing === CameraSelector.DEFAULT_FRONT_CAMERA)
            lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
        else if (lensFacing === CameraSelector.DEFAULT_BACK_CAMERA)
            lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA
        startCamera()
    }

    //Implement Preview use case
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            //a singleton Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            //viewfinder is used to let the user preview the photo they will be taking. can  viewfinder using the CameraX Preview class.
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding?.viewFinderPreview?.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build() // use cases
//            videoCapture = VideoCapture.Builder().build()


            // Select back camera as a default
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, lensFacing, preview, imageCapture);

//                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                // auto focus functionality in every x seconds
                binding?.viewFinderPreview?.afterMeasured {
                    val autoFocusPoint = SurfaceOrientedMeteringPointFactory(1f, 1f)
                        .createPoint(.5f, .5f)
                    try {
                        val autoFocusAction = FocusMeteringAction.Builder(
                            autoFocusPoint,
                            FocusMeteringAction.FLAG_AF
                        ).apply {
                            //start auto-focusing after 2 seconds
                            setAutoCancelDuration(2, java.util.concurrent.TimeUnit.SECONDS)
                        }.build()
                        camera?.cameraControl?.startFocusAndMetering(autoFocusAction)
                    } catch (e: CameraInfoUnavailableException) {
                        Log.d("ERROR", "cannot access camera", e)
                    }
                }


            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    //    First, you define a configuration object that is used to instantiate the actual use case object
    //    Implement ImageCapture use case
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        // if the use case is null exit to the function This will be null If you tap the photo button before image capture is set up. Without the return statement, the app would crash if it was null.
        val imageCapture = imageCapture ?: return
        val photoFile = File(outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

        // create outputOption object which contains file + metadata
        //  OutputFileOptions object where u can specify things about how your output would be
        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // flash on when image click
        flash?.let { camera?.cameraControl?.enableTorch(it) } // enable torch
        // set up image capture listener , which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri =  Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, "onImageSaved: ${Uri.fromFile(photoFile)}")
                    Log.d(TAG, "onImageSaved: ${outputFileResults.savedUri}")
                    saveImageToGallery(BitmapUtils.uriToBitmap(this@MainActivity, savedUri)) {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.d(TAG, "Photo capture failed: ${exc.message}", exc)
                }

            },
        )

        imageCapture.takePicture( ContextCompat.getMainExecutor(this) ,object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                playCaptureSound(R.raw.camera_capture_sound)
                Toast.makeText(this@MainActivity,"capture success",Toast.LENGTH_SHORT).show()
                image.close();
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@MainActivity,"capture failed",Toast.LENGTH_SHORT).show()
            }
        })
    }

    // tap to focus implementation
    //    CameraX supports autofocus, but want to make the ability to manually control the focus target.


    //    fun onTouch(x:Float, y:Float) {
//        val meteringPoint =  DisplayOrientedMeteringPointFactory(mSurfaceView.getDisplay(), cameraSelector, mSurfaceView.getWidth(), mSurfaceView.getHeight()).createPoint(x.y);
//
//        // Prepare focus action to be triggered.
//        val action = FocusMeteringAction.Builder(meteringPoint).build();
//
//        // Execute focus action
//        cameraControl.startFocusAndMetering(action);
//    }


}


