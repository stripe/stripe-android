@file:Suppress("deprecation")
/*
 * camera1 is deprecated, but still our best option for android 5.0
 *
 * Camera2 is broken in android API 21. The YUV image it returns is in an incorrect format, which
 * affects a very limited number of older devices (visible in manual testing on a Samsung Galaxy
 * Note 3).
 *
 * CameraX (which uses camera2 under the hood) still has alpha dependencies, which merchants have
 * expressed hesitancy about integrating. Once the alpha dependencies are resolved and CameraX has
 * been tested on API 21 devices, we may choose to swap to that.
 *
 * For an implementation of CameraX, see the legacy bouncer code:
 * https://github.com/getbouncer/cardscan-android/blob/master/scan-camerax/src/main/java/com/getbouncer/scan/camera/extension/CameraAdapterImpl.kt
 */
package com.stripe.android.stripecardscan.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PointF
import android.graphics.Rect
import android.hardware.Camera
import android.hardware.Camera.AutoFocusCallback
import android.hardware.Camera.PreviewCallback
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.stripe.android.stripecardscan.framework.Config
import com.stripe.android.stripecardscan.framework.image.NV21Image
import com.stripe.android.stripecardscan.framework.image.getRenderScript
import com.stripe.android.stripecardscan.framework.image.rotate
import com.stripe.android.stripecardscan.framework.util.retrySync
import java.lang.ref.WeakReference
import java.util.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ASPECT_TOLERANCE = 0.2

private val MAXIMUM_RESOLUTION = Size(1920, 1080)

data class CameraPreviewImage<ImageType>(
    val image: ImageType,
    val viewBounds: Rect,
)

/**
 * A [CameraAdapter] that uses android's Camera 1 APIs to show previews and process images.
 */
internal class Camera1Adapter(
    private val activity: Activity,
    private val previewView: ViewGroup,
    private val minimumResolution: Size,
    private val cameraErrorListener: CameraErrorListener,
) : CameraAdapter<CameraPreviewImage<Bitmap>>(), PreviewCallback {
    override val implementationName: String = "Camera1"

    private var mCamera: Camera? = null
    private var cameraPreview: CameraPreview? = null
    private var mRotation = 0
    private var onCameraAvailableListener: WeakReference<((Camera) -> Unit)?> = WeakReference(null)
    private var currentCameraId = 0

    private val mainThreadHandler = Handler(activity.mainLooper)
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    override fun withFlashSupport(task: (Boolean) -> Unit) {
        mCamera?.let {
            task(isFlashSupported(it))
        } ?: run {
            onCameraAvailableListener = WeakReference { cam ->
                task(isFlashSupported(cam))
            }
        }
    }

    private fun isFlashSupported(camera: Camera) = camera
        .parameters
        ?.supportedFlashModes
        ?.contains(Camera.Parameters.FLASH_MODE_TORCH) == true

    override fun setTorchState(on: Boolean) {
        mCamera?.apply {
            val parameters = parameters
            if (on) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            } else {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
            }
            setCameraParameters(this, parameters)
            startCameraPreview()
        }
    }

    override fun isTorchOn(): Boolean =
        mCamera?.parameters?.flashMode == Camera.Parameters.FLASH_MODE_TORCH

    override fun setFocus(point: PointF) {
        mCamera?.apply {
            val params = parameters
            if (params.maxNumFocusAreas > 0) {
                val focusRect = Rect(
                    point.x.toInt() - 150,
                    point.y.toInt() - 150,
                    point.x.toInt() + 150,
                    point.y.toInt() + 150
                )
                val cameraFocusAreas: MutableList<Camera.Area> = ArrayList()
                cameraFocusAreas.add(Camera.Area(focusRect, 1000))
                params.focusAreas = cameraFocusAreas
                setCameraParameters(this, params)
            }
        }
    }

    override fun onPreviewFrame(bytes: ByteArray?, camera: Camera) {
        // this method may be called after the camera has closed if there was still an image in
        // flight. In this case, swallow the error. Ideally, we would be able to tell whether the
        // exception was due to the camera already having been closed or from an error with camera
        // hardware.
        val imageWidth =
            try { camera.parameters.previewSize.width } catch (t: Throwable) { return }
        val imageHeight =
            try { camera.parameters.previewSize.height } catch (t: Throwable) { return }

        if (bytes != null) {
            try {
                sendImageToStream(
                    CameraPreviewImage(
                        image = NV21Image(imageWidth, imageHeight, bytes)
                            .toBitmap(getRenderScript(activity))
                            .rotate(mRotation.toFloat()),
                        viewBounds = Rect(0, 0, previewView.width, previewView.height),
                    ),
                )
            } catch (t: Throwable) {
                // ignore errors transforming the image (OOM, etc)
                Log.e(Config.logTag, "Exception caught during camera transform", t)
            } finally {
                camera.addCallbackBuffer(bytes)
            }
        } else {
            camera.addCallbackBuffer(ByteArray((imageWidth * imageHeight * 1.5).roundToInt()))
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    override fun onPause() {
        super.onPause()

        mCamera?.stopPreview()
        mCamera?.setPreviewCallbackWithBuffer(null)
        mCamera?.release()
        mCamera = null

        cameraPreview?.apply { holder.removeCallback(this) }
        cameraPreview = null

        stopCameraThread()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        startCameraThread()

        mainThreadHandler.post {
            try {
                var camera: Camera? = null
                try {
                    camera = Camera.open(currentCameraId)
                } catch (t: Throwable) {
                    cameraErrorListener.onCameraOpenError(t)
                }
                onCameraOpen(camera)
            } catch (t: Throwable) {
                cameraErrorListener.onCameraOpenError(t)
            }
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startCameraThread() {
        val thread = HandlerThread("CameraBackground").also { it.start() }
        cameraThread = thread
        cameraHandler = Handler(thread.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        try {
            cameraThread?.join()
            cameraThread = null
            cameraHandler = null
        } catch (e: InterruptedException) {
            mainThreadHandler.post { cameraErrorListener.onCameraOpenError(e) }
        }
    }

    private fun setCameraParameters(
        camera: Camera,
        parameters: Camera.Parameters
    ) {
        try {
            camera.parameters = parameters
        } catch (t: Throwable) {
            Log.w(Config.logTag, "Error setting camera parameters", t)
            // ignore failure to set camera parameters
        }
    }

    private fun startCameraPreview() {
        cameraHandler?.post {
            try {
                retrySync(times = 5) {
                    mCamera?.startPreview()
                }
            } catch (t: Throwable) {
                mainThreadHandler.post {
                    cameraErrorListener.onCameraOpenError(t)
                }
            }
        }
    }

    private fun onCameraOpen(camera: Camera?) {
        if (camera == null) {
            mainThreadHandler.post {
                cameraPreview?.apply { holder.removeCallback(this) }
                cameraErrorListener.onCameraOpenError(null)
            }
        } else {
            mCamera = camera
            setCameraDisplayOrientation(activity)
            setCameraPreviewFrame()

            // Create our Preview view and set it as the content of our activity.
            cameraPreview = CameraPreview(activity, this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }.also { cameraPreview ->
                mainThreadHandler.post {
                    onCameraAvailableListener.get()?.let {
                        it(camera)
                    }
                    onCameraAvailableListener.clear()

                    previewView.removeAllViews()
                    previewView.addView(cameraPreview)
                }
            }
        }
    }

    private fun setCameraPreviewFrame() {
        mCamera?.apply {
            val format = ImageFormat.NV21
            val parameters = parameters
            parameters.previewFormat = format

            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            val displayWidth = max(displayMetrics.heightPixels, displayMetrics.widthPixels)
            val displayHeight = min(displayMetrics.heightPixels, displayMetrics.widthPixels)

            val height: Int = minimumResolution.height
            val width = displayWidth * height / displayHeight

            getOptimalPreviewSize(parameters.supportedPreviewSizes, width, height)?.apply {
                parameters.setPreviewSize(this.width, this.height)
            }

            setCameraParameters(this, parameters)
        }
    }

    private fun getOptimalPreviewSize(
        sizes: List<Camera.Size>?,
        w: Int,
        h: Int
    ): Camera.Size? {
        val targetRatio = w.toDouble() / h
        if (sizes == null) {
            return null
        }
        var optimalSize: Camera.Size? = null

        // Find the smallest size that fits our tolerance and is at least as big as our target
        // height
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (abs(ratio - targetRatio) <= ASPECT_TOLERANCE) {
                if (size.height >= h) {
                    optimalSize = size
                }
            }
        }

        // Find the closest ratio that is still taller than our target height
        if (optimalSize == null) {
            var minDiffRatio = Double.MAX_VALUE
            for (size in sizes) {
                val ratio = size.width.toDouble() / size.height
                val ratioDiff = abs(ratio - targetRatio)
                if (
                    size.height >= h && ratioDiff <= minDiffRatio &&
                    size.height <= MAXIMUM_RESOLUTION.height &&
                    size.width <= MAXIMUM_RESOLUTION.width
                ) {
                    optimalSize = size
                    minDiffRatio = ratioDiff
                }
            }
        }
        if (optimalSize == null) {
            // Find the smallest size that is at least as big as our target height
            for (size in sizes) {
                if (size.height >= h) {
                    optimalSize = size
                }
            }
        }

        return optimalSize
    }

    private fun setCameraDisplayOrientation(activity: Activity) {
        val camera = mCamera ?: return
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info)

        val rotation = activity.windowManager.defaultDisplay.rotation
        val degrees = rotation.rotationToDegrees()

        val result = if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (360 - (info.orientation + degrees) % 360) % 360 // compensate for the mirror
        } else { // back-facing
            (info.orientation - degrees + 360) % 360
        }

        try {
            camera.stopPreview()
        } catch (e: java.lang.Exception) {
            // preview was already stopped
        }

        try {
            camera.setDisplayOrientation(result)
        } catch (t: Throwable) {
//            cameraErrorListener.onCameraUnsupportedError(t)
        }

        startCameraPreview()

        mRotation = result
    }

    /** A basic Camera preview class  */
    @SuppressLint("ViewConstructor")
    private inner class CameraPreview(
        context: Context,
        private val mPreviewCallback: PreviewCallback
    ) : SurfaceView(context), AutoFocusCallback, SurfaceHolder.Callback {

        init {
            holder.addCallback(this)
            mCamera?.apply {
                val params = parameters
                val focusModes = params.supportedFocusModes
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                }

                params.setRecordingHint(true)
                setCameraParameters(this, params)
            }
        }

        override fun onAutoFocus(success: Boolean, camera: Camera) {}

        /**
         * The Surface has been created, now tell the camera where to draw the preview.
         */
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                mCamera?.setPreviewDisplay(this.holder)
                mCamera?.setPreviewCallbackWithBuffer(mPreviewCallback)
                startCameraPreview()
            } catch (t: Throwable) {
                mainThreadHandler.post {
                    cameraErrorListener.onCameraOpenError(t)
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            w: Int,
            h: Int
        ) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (this.holder.surface == null) {
                // preview surface does not exist
                return
            }

            // stop preview before making changes
            try {
                mCamera?.stopPreview()
            } catch (t: Throwable) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera?.setPreviewDisplay(this.holder)
                val bufSize = w * h * ImageFormat.getBitsPerPixel(format) / 8
                for (i in 0..2) {
                    mCamera?.addCallbackBuffer(ByteArray(bufSize))
                }
                mCamera?.setPreviewCallbackWithBuffer(mPreviewCallback)
                startCameraPreview()
            } catch (t: Throwable) {
                mainThreadHandler.post {
                    cameraErrorListener.onCameraOpenError(t)
                }
            }
        }
    }

    override fun withSupportsMultipleCameras(task: (Boolean) -> Unit) {
        task(Camera.getNumberOfCameras() > 1)
    }

    override fun changeCamera() {
        currentCameraId++
        if (currentCameraId >= Camera.getNumberOfCameras()) {
            currentCameraId = 0
        }
        onPause()
        onResume()
    }

    override fun getCurrentCamera(): Int = currentCameraId
}
