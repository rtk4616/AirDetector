package com.microjet.airqi2.photoShare

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.NonNull
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.View
import android.widget.Toast
import com.microjet.airqi2.R
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.timerTask


class CameraActivity : AppCompatActivity() {

    private var isBackCamera: Boolean = true
    private var isFullScreen: Boolean = false
    private var isCountDownEnabled: Boolean = false
    private var countDownInSeconds: Int = 0
    private lateinit var cameraId: String
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSessions: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var imageDimension: Size
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread

    private var textureListener: TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            prepareToShowCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        this.isBackCamera = this.intent.getBooleanExtra(IS_BACK_CAMERA, true)
        this.isFullScreen = this.intent.getBooleanExtra(IS_FULLSCREEN, false)
        this.isCountDownEnabled = this.intent.getBooleanExtra(IS_COUNT_DOWN_ENABLED, false)
        this.countDownInSeconds = this.intent.getIntExtra(COUNT_DOWN_IN_SECONDS, 0)

        this.textureView.surfaceTextureListener = this.textureListener

        this.takePictureButton.setOnClickListener { takePicture() }

        this.btnChangeCamera.setOnClickListener { changeCamera() }
        this.setupFullScreen()
    }

    override fun onRequestPermissionsResult(requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray) {
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Camera permissions is requried.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                if (this.textureView.isAvailable) {
                    prepareToShowCamera()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        }

        if (this.textureView.isAvailable) {
            prepareToShowCamera()
        } else {
            this.textureView.surfaceTextureListener = this.textureListener
        }
    }

    override fun onPause() {
        stopBackgroundThread()
        super.onPause()
    }

    private fun prepareToShowCamera() {
        if (ContextCompat.checkSelfPermission(this@CameraActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this@CameraActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
            setupCountDownLabel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
        if (this.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun setupCountDownLabel() {

        if (this.isCountDownEnabled) {
            this.coundDownLabel.text = this.countDownInSeconds.toString()
            Timer("Count Down", false).schedule(timerTask {
                runOnUiThread {
                    if (this@CameraActivity.countDownInSeconds > 0) {
                        this@CameraActivity.countDownInSeconds -= 1
                        if (this@CameraActivity.countDownInSeconds == 0) {
                            this@CameraActivity.coundDownLabel.visibility = View.GONE
                        }
                        this@CameraActivity.coundDownLabel.text = this@CameraActivity.countDownInSeconds.toString()
                    } else {
                        this@timerTask.cancel()
                        this@CameraActivity.takePicture()
                    }
                }
            }, 1000, 1000)
        } else {
            this.coundDownLabel.visibility = View.GONE
        }
    }

    private fun setupFullScreen() {
        if (this.isFullScreen) {
            this.takePictureButton.visibility = View.GONE
        }
    }

    private fun startBackgroundThread() {
        this.backgroundThread = HandlerThread("Camera Background")
        this.backgroundThread.start()
        this.backgroundHandler = Handler(this.backgroundThread.looper)
    }

    private fun stopBackgroundThread() {
        this.backgroundThread.quitSafely()
        try {
            this.backgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun takePicture() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val characteristics = manager.getCameraCharacteristics(this.cameraDevice.id)
            var jpegSizes: Array<Size> = arrayOf()

            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG)
            }

            var width = 640
            var height = 480

            if (jpegSizes.isNotEmpty()) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }

            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)

            val outputSurfaces = ArrayList<Surface>(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(this.textureView.surfaceTexture))

            val captureBuilder = this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, this.getSensorOrientation(manager))

            val readerListener = ImageReader.OnImageAvailableListener { reader ->
                val image: Image = reader.acquireNextImage()

                try {
                    val buffer = image.planes[0].buffer
                    buffer.rewind()
                    val data = ByteArray(buffer.capacity())
                    buffer.get(data)
                    val storedBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

                    sendByteArray(storedBitmap)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    image.close()
                }
            }

            reader.setOnImageAvailableListener(readerListener, this.backgroundHandler)

            val captureListener = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    super.onCaptureCompleted(session, request, result)
                    createCameraPreview()
                }
            }

            this.cameraDevice.createCaptureSession(outputSurfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, this@CameraActivity.backgroundHandler)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                }
            }, this.backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun changeCamera() {
        closeCamera()
        this.isBackCamera = !this.isBackCamera
        openCamera()
    }

    @Throws(CameraAccessException::class)
    private fun getSensorOrientation(manager: CameraManager): Int {
        return manager.getCameraCharacteristics(this.cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION)
    }

    private fun createCameraPreview() {
        try {
            val texture = this.textureView.surfaceTexture
            texture.setDefaultBufferSize(this.imageDimension.width, this.imageDimension.height)

            val surface = Surface(texture)
            this.captureRequestBuilder = this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            this.captureRequestBuilder.addTarget(surface)

            this.cameraDevice.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(@NonNull cameraCaptureSession: CameraCaptureSession) {
                    cameraCaptureSessions = cameraCaptureSession
                    updatePreview()
                }

                override fun onConfigureFailed(@NonNull cameraCaptureSession: CameraCaptureSession) {
                    Toast.makeText(this@CameraActivity, "Configuration change.", Toast.LENGTH_SHORT).show()
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getFrontFacingCameraId(cameraManager: CameraManager): String {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
            if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) return cameraId
        }
        return cameraManager.cameraIdList[0]
    }

    private fun getBackCameraId(cameraManager: CameraManager): String {
        return cameraManager.cameraIdList[0]
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (this.isBackCamera) {
                this.cameraId = this.getBackCameraId(manager)
            } else {
                this.cameraId = this.getFrontFacingCameraId(manager)
            }
            val characteristics = manager.getCameraCharacteristics(this.cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            this.imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]

            manager.openCamera(this.cameraId, stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePreview() {
        this.captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            this.cameraCaptureSessions.setRepeatingRequest(this.captureRequestBuilder.build(), null, this.backgroundHandler)
            this.configureTransform(this.textureView.width.toFloat(), this.textureView.height.toFloat())
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun configureTransform(viewWidth: Float, viewHeight: Float) {
        val rotation = this.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0.toFloat(), 0.toFloat(), viewWidth, viewHeight)
        val bufferRect = RectF(0.toFloat(), 0.toFloat(), this.textureView.height.toFloat(), this.textureView.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                    viewHeight / this.textureView.height.toFloat(),
                    viewWidth / this.textureView.width.toFloat())
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90.toFloat() * (rotation - 2), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180.toFloat(), centerX, centerY)
        }
        this.textureView?.setTransform(matrix)
    }

    private fun closeCamera() {
        this.cameraDevice.close()
        //this.imageReader.close()
    }

    private fun sendByteArray(bitmap: Bitmap) {
        val output = Intent()
        output.putExtra(VSCAMERAACTIVITY_IMAGE_ID, VSBitmapStore.putBitmap(bitmap))
        this.setResult(1, output)
        this.finish()
    }

    companion object {
        val VSCAMERAACTIVITY_RESULT_CODE = 1
        val VSCAMERAACTIVITY_IMAGE_ID = "VSCameraActivityImageId"
        val IS_BACK_CAMERA = "is_back_camera"
        val IS_FULLSCREEN = "is_fullscreen"
        val IS_COUNT_DOWN_ENABLED = "is_count_down_enabled"
        val COUNT_DOWN_IN_SECONDS = "count_down_in_seconds"
        private val REQUEST_CAMERA_PERMISSION = 200

        fun newIntent(context: Context, isBackCamera: Boolean? = true, isFullScreen: Boolean? = false, isCountDownEnabled: Boolean? = false, countDownInSeconds: Int? = 0): Intent {
            val intent = Intent(context, CameraActivity::class.java)
            intent.putExtra(IS_BACK_CAMERA, isBackCamera)
            intent.putExtra(IS_FULLSCREEN, isFullScreen)
            intent.putExtra(IS_COUNT_DOWN_ENABLED, isCountDownEnabled)
            intent.putExtra(COUNT_DOWN_IN_SECONDS, countDownInSeconds)
            return intent
        }
    }
}

object VSBitmapStore {
    private val BITMAPS = HashMap<Int, Bitmap>()
    fun getBitmap(id: Int): Bitmap {
        return BITMAPS.remove(id)!!
    }

    fun putBitmap(b: Bitmap): Int {
        val id = b.hashCode()
        BITMAPS[id] = b
        return id
    }
}
