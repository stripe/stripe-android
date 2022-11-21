package com.stripe.android.camera

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.renderscript.RenderScript
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.RestrictTo
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.camera.framework.exception.ImageTypeNotSupportedException
import com.stripe.android.camera.framework.image.NV21Image
import com.stripe.android.camera.framework.image.getRenderScript
import com.stripe.android.camera.framework.util.mapArray
import com.stripe.android.camera.framework.util.mapToIntArray
import com.stripe.android.camera.framework.util.size
import com.stripe.android.camera.framework.util.toByteArray
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.experimental.inv
import kotlin.math.max
import kotlin.math.min

/**
 * Convert a resolution to a size on the screen based only on the display size.
 */
private fun Size.resolutionToSize(displaySize: Size) = when {
    displaySize.width >= displaySize.height -> Size(
        /* width */
        max(width, height),
        /* height */
        min(width, height),
    )
    else -> Size(
        /* width */
        min(width, height),
        /* height */
        max(width, height),
    )
}

/**
 * Utility function for converting YUV planes into an NV21 byte array
 *
 * https://stackoverflow.com/questions/52726002/camera2-captured-picture-conversion-from-yuv-420-888-to-nv21/52740776#52740776
 *
 * On Revvl2, average performance is ~5ms
 */
private fun yuvPlanesToNV21Fast(
    width: Int,
    height: Int,
    planeBuffers: Array<ByteBuffer>,
    rowStrides: IntArray,
    pixelStrides: IntArray,
): ByteArray {
    val ySize = width * height
    val uvSize = width * height / 4
    val nv21 = ByteArray(ySize + uvSize * 2)
    val yBuffer = planeBuffers[0] // Y
    val uBuffer = planeBuffers[1] // U
    val vBuffer = planeBuffers[2] // V
    var rowStride = rowStrides[0]
    check(pixelStrides[0] == 1)
    var pos = 0
    if (rowStride == width) { // likely
        yBuffer[nv21, 0, ySize]
        pos += ySize
    } else {
        var yBufferPos = -rowStride.toLong() // not an actual position
        while (pos < ySize) {
            yBufferPos += rowStride.toLong()
            yBuffer.position(yBufferPos.toInt())
            yBuffer[nv21, pos, width]
            pos += width
        }
    }
    rowStride = rowStrides[2]
    val pixelStride = pixelStrides[2]
    check(rowStride == rowStrides[1])
    check(pixelStride == pixelStrides[1])
    if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
        // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
        val savePixel = vBuffer[1]
        try {
            vBuffer.put(1, savePixel.inv())
            if (uBuffer[0] == savePixel.inv()) {
                vBuffer.put(1, savePixel)
                vBuffer.position(0)
                uBuffer.position(0)
                vBuffer[nv21, ySize, 1]
                uBuffer[nv21, ySize + 1, uBuffer.remaining()]
                return nv21 // shortcut
            }
        } catch (ex: ReadOnlyBufferException) {
            // unfortunately, we cannot check if vBuffer and uBuffer overlap
        }

        // unfortunately, the check failed. We must save U and V pixel by pixel
        vBuffer.put(1, savePixel)
    }

    // other optimizations could check if (pixelStride == 1) or (pixelStride == 2),
    // but performance gain would be less significant
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val vuPos = col * pixelStride + row * rowStride
            nv21[pos++] = vBuffer[vuPos]
            nv21[pos++] = uBuffer[vuPos]
        }
    }

    return nv21
}

/**
 * Convert an ImageProxy to a bitmap.
 */
@CheckResult
private fun ImageProxy.toBitmap(renderScript: RenderScript) = when (format) {
    ImageFormat.NV21 -> NV21Image(width, height, planes[0].buffer.toByteArray()).toBitmap(
        renderScript
    )
    ImageFormat.YUV_420_888 -> NV21Image(
        width,
        height,
        yuvPlanesToNV21Fast(
            width,
            height,
            planes.mapArray { it.buffer },
            planes.mapToIntArray { it.rowStride },
            planes.mapToIntArray { it.pixelStride },
        ),
    ).toBitmap(renderScript)
    else -> throw ImageTypeNotSupportedException(format)
}

/**
 * CameraAdaptor implementation with CameraX, should be used in favor or [Camera1Adapter].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CameraXAdapter(
    private val activity: Activity,
    private val previewView: ViewGroup,
    private val minimumResolution: Size,
    private val cameraErrorListener: CameraErrorListener,
    private val startWithBackCamera: Boolean = true
) : CameraAdapter<CameraPreviewImage<Bitmap>>() {

    override val implementationName: String = "CameraX"

    private var lensFacing: Int = LENS_UNINITIALIZED

    private val mainThreadHandler = Handler(activity.mainLooper)

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
    private lateinit var lifecycleOwner: LifecycleOwner

    private val cameraListeners = mutableListOf<(Camera) -> Unit>()

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    private val display by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            null
        }
            ?: @Suppress("Deprecation")
            activity.windowManager.defaultDisplay
    }

    private val displayRotation by lazy { display.rotation }
    private val displayMetrics by lazy { DisplayMetrics().also { display.getRealMetrics(it) } }
    private val displaySize by lazy {
        Size(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels
        )
    }

    private val previewTextureView by lazy { PreviewView(activity) }

    override fun withFlashSupport(task: (Boolean) -> Unit) {
        withCamera { task(it.cameraInfo.hasFlashUnit()) }
    }

    override fun setTorchState(on: Boolean) {
        camera?.cameraControl?.enableTorch(on)
    }

    override fun isTorchOn(): Boolean =
        camera?.cameraInfo?.torchState?.value == TorchState.ON

    override fun withSupportsMultipleCameras(task: (Boolean) -> Unit) {
        withCameraProvider {
            task(hasBackCamera(it) && hasFrontCamera(it))
        }
    }

    override fun changeCamera() {
        withCameraProvider {
            lensFacing = when {
                lensFacing == CameraSelector.LENS_FACING_BACK && hasFrontCamera(it) -> CameraSelector.LENS_FACING_FRONT
                lensFacing == CameraSelector.LENS_FACING_FRONT && hasBackCamera(it) -> CameraSelector.LENS_FACING_BACK
                hasBackCamera(it) -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera(it) -> CameraSelector.LENS_FACING_FRONT
                else -> CameraSelector.LENS_FACING_BACK
            }

            bindCameraUseCases(it)
        }
    }

    override fun getCurrentCamera(): Int = lensFacing

    override fun setFocus(point: PointF) {
        camera?.let { cam ->
            val meteringPointFactory = DisplayOrientedMeteringPointFactory(
                display,
                cam.cameraInfo,
                displaySize.width.toFloat(),
                displaySize.height.toFloat(),
            )
            val action =
                FocusMeteringAction.Builder(meteringPointFactory.createPoint(point.x, point.y))
                    .build()
            cam.cameraControl.startFocusAndMetering(action)
        }
    }

    override fun onCreate() {
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        previewView.post {
            previewView.removeAllViews()
            previewView.addView(previewTextureView)

            previewTextureView.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }

            previewTextureView.requestLayout()

            setUpCamera()
        }
    }

    override fun onDestroyed() {
        withCameraProvider {
            it.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    override fun unbindFromLifecycle(lifecycleOwner: LifecycleOwner) {
        super.unbindFromLifecycle(lifecycleOwner)
        withCameraProvider { cameraProvider ->
            preview?.let { preview ->
                cameraProvider.unbind(preview)
            }
        }
    }

    private fun setUpCamera() {
        withCameraProvider {
            lensFacing = if (startWithBackCamera && hasBackCamera(it)) {
                CameraSelector.LENS_FACING_BACK
            } else if (!startWithBackCamera && hasFrontCamera(it)) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                mainThreadHandler.post {
                    cameraErrorListener.onCameraUnsupportedError(IllegalStateException("No camera is available"))
                }
                CameraSelector.LENS_FACING_BACK
            }
            bindCameraUseCases(it)
        }
    }

    @Synchronized
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetRotation(displayRotation)
            .setTargetResolution(previewView.size())
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetRotation(displayRotation)
            .setTargetResolution(minimumResolution.resolutionToSize(displaySize))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(1)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(
                    cameraExecutor
                ) { image ->
                    val bitmap = image.toBitmap(getRenderScript(activity))
                        .rotate(image.imageInfo.rotationDegrees.toFloat())
                    image.close()
                    sendImageToStream(
                        CameraPreviewImage(
                            bitmap,
                            Rect(
                                previewView.left,
                                previewView.top,
                                previewView.width,
                                previewView.height
                            )
                        )
                    )
                }
            }

        cameraProvider.unbindAll()

        try {
            val newCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            notifyCameraListeners(newCamera)
            camera = newCamera

            preview?.setSurfaceProvider(previewTextureView.surfaceProvider)
        } catch (t: Throwable) {
            Log.e(logTag, "Use case camera binding failed", t)
            mainThreadHandler.post { cameraErrorListener.onCameraOpenError(t) }
        }
    }

    private fun notifyCameraListeners(camera: Camera) {
        val listenerIterator = cameraListeners.iterator()
        while (listenerIterator.hasNext()) {
            listenerIterator.next()(camera)
            listenerIterator.remove()
        }
    }

    @Synchronized
    private fun <T> withCamera(task: (Camera) -> T) {
        val camera = this.camera
        if (camera != null) {
            task(camera)
        } else {
            cameraListeners.add { task(it) }
        }
    }

    override fun bindToLifecycle(lifecycleOwner: LifecycleOwner) {
        super.bindToLifecycle(lifecycleOwner)
        this.lifecycleOwner = lifecycleOwner
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(cameraProvider: ProcessCameraProvider): Boolean =
        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(cameraProvider: ProcessCameraProvider): Boolean =
        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

    /**
     * Run a task with the camera provider.
     */
    private fun withCameraProvider(
        executor: Executor = ContextCompat.getMainExecutor(activity),
        task: (ProcessCameraProvider) -> Unit,
    ) {
        cameraProviderFuture.addListener({ task(cameraProviderFuture.get()) }, executor)
    }

    private companion object {
        val logTag: String = CameraXAdapter::class.java.simpleName
        val LENS_UNINITIALIZED = -1
    }
}
