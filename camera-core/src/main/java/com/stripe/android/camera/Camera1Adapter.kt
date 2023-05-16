@file:Suppress("deprecation")

package com.stripe.android.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
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
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import com.stripe.android.camera.framework.image.NV21Image
import com.stripe.android.camera.framework.image.getRenderScript
import com.stripe.android.camera.framework.util.retrySync
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ASPECT_TOLERANCE = 0.2

private val MAXIMUM_RESOLUTION = Size(1920, 1080)

/**
 * Rotate a [Bitmap] by the given [rotationDegrees].
 */
@CheckResult
internal fun Bitmap.rotate(rotationDegrees: Float): Bitmap = if (rotationDegrees != 0F) {
    val matrix = Matrix()
    matrix.postRotate(rotationDegrees)
    Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
} else {
    this
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class CameraPreviewImage<ImageType>(
    val image: ImageType,
    val viewBounds: Rect
)

/**
 * A [CameraAdapter] that uses android's Camera 1 APIs to show previews and process images.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated("Use CameraXAdaptor instead")
class Camera1Adapter(
    private val activity: Activity,
    private val previewView: ViewGroup,
    private val minimumResolution: Size,
    private val cameraErrorListener: CameraErrorListener,
    startWithBackCamera: Boolean = true
) : CameraAdapter<CameraPreviewImage<Bitmap>>(), PreviewCallback {
    override val implementationName: String = "Camera1"

    private var mCamera: Camera? = null
    private var cameraPreview: CameraPreview? = null
    private var mRotation = 0
    private var onCameraAvailableListener: WeakReference<((Camera) -> Unit)?> = WeakReference(null)
    private var currentCameraId =
        if (startWithBackCamera) Camera.CameraInfo.CAMERA_FACING_BACK else Camera.CameraInfo.CAMERA_FACING_FRONT

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
            if (isFlashSupported(this)) {
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

    @Deprecated("Deprecated in Java")
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
                            .rotate(
                                if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                    mRotation.toFloat()
                                } else {
                                    -mRotation.toFloat()
                                }
                            ),
                        viewBounds = Rect(
                            previewView.left,
                            previewView.top,
                            previewView.width,
                            previewView.height
                        )
                    )
                )
            } catch (t: Throwable) {
                // ignore errors transforming the image (OOM, etc)
                Log.e(logTag, "Exception caught during camera transform", t)
            } finally {
                camera.addCallbackBuffer(bytes)
            }
        } else {
            camera.addCallbackBuffer(ByteArray((imageWidth * imageHeight * 1.5).roundToInt()))
        }
    }

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

    override fun onResume() {
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
            Log.w(logTag, "Error setting camera parameters", t)
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

    /**
     * Create a LayoutParams by maintaining the target aspect ratio so that both dimensions
     * (width and height) of the Layout will be equal to or larger than the corresponding dimension
     * of the parent. This is similar to ImageView's CENTER_CROP scale type.
     */
    private fun calculateNewParamOverScreen(
        parentWidth: Int,
        parentHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): ViewGroup.LayoutParams {
        // Target dimension
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        // Parent dimension
        val parentRatio = parentWidth.toFloat() / parentHeight.toFloat()

        val finalWidth: Int
        val finalHeight: Int
        if (targetRatio > parentRatio) { // too wide, prospect height, let width go over
            finalWidth = (targetRatio * parentHeight.toFloat()).toInt()
            finalHeight = parentHeight
        } else { // too high, prospect width, let height go over
            finalWidth = parentWidth
            finalHeight = (parentWidth.toFloat() / targetRatio).toInt()
        }
        // PreviewView has to be a FrameLayout so that we can center it
        // TODO(ccen) change the type of previewView to [FrameLayout]
        return FrameLayout.LayoutParams(finalWidth, finalHeight)
            .also { it.gravity = Gravity.CENTER }
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
                layoutParams = calculateNewParamOverScreen(
                    previewView.width,
                    previewView.height,
                    // previewSize is always landscape, need to flip height and width
                    camera.parameters.previewSize.height,
                    camera.parameters.previewSize.width
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

            val previewWidth = max(previewView.height, previewView.width)
            val previewHeight = min(previewView.height, previewView.width)

            val height: Int = minimumResolution.height
            val width = previewWidth * height / previewHeight

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
        Camera.getCameraInfo(currentCameraId, info)

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

        @Deprecated("Deprecated in Java")
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

    private companion object {
        val logTag: String = Camera1Adapter::class.java.simpleName
    }
}
