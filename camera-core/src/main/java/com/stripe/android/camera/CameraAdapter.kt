package com.stripe.android.camera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.util.Log
import android.view.Surface
import androidx.annotation.CheckResult
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking

/**
 * Valid integer rotation values.
 */
@IntDef(
    Surface.ROTATION_0,
    Surface.ROTATION_90,
    Surface.ROTATION_180,
    Surface.ROTATION_270
)
@Retention(AnnotationRetention.SOURCE)
internal annotation class RotationValue

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class CameraAdapter<CameraOutput> : LifecycleEventObserver {

    // TODO: change this to be a channelFlow once it's no longer experimental, add some capacity and use a backpressure drop strategy
    private val imageChannel = Channel<CameraOutput>(capacity = Channel.RENDEZVOUS)
    private var lifecyclesBound = 0

    abstract val implementationName: String

    companion object {

        /**
         * Determine if the device supports the camera features used by this SDK.
         */
        @JvmStatic
        fun isCameraSupported(context: Context): Boolean =
            (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)).also {
                if (!it) Log.e(logTag, "System feature 'FEATURE_CAMERA_ANY' is unavailable")
            }

        /**
         * Calculate degrees from a [RotationValue].
         */
        @CheckResult
        fun Int.rotationToDegrees(): Int = this * 90

        val logTag: String = CameraAdapter::class.java.simpleName
    }

    protected fun sendImageToStream(image: CameraOutput) = try {
        imageChannel.trySend(image).onClosed {
            Log.w(logTag, "Attempted to send image to closed channel", it)
        }.onFailure {
            if (it != null) {
                Log.w(logTag, "Failure when sending image to channel", it)
            } else {
                Log.v(logTag, "No analyzers available to process image")
            }
        }.onSuccess {
            Log.v(logTag, "Successfully sent image to be processed")
        }
    } catch (e: ClosedSendChannelException) {
        Log.w(logTag, "Attempted to send image to closed channel")
    } catch (t: Throwable) {
        Log.e(logTag, "Unable to send image to channel", t)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> onDestroyed()
            Lifecycle.Event.ON_PAUSE -> onPause()
            Lifecycle.Event.ON_CREATE -> onCreate()
            Lifecycle.Event.ON_START -> onStart()
            Lifecycle.Event.ON_RESUME -> onResume()
            Lifecycle.Event.ON_STOP -> onStop()
            Lifecycle.Event.ON_ANY -> onAny()
        }
    }

    protected open fun onDestroyed() {
        runBlocking { imageChannel.close() }
    }

    protected open fun onPause() {
        // support onPause events.
    }

    protected open fun onCreate() {
        // support onCreate events.
    }

    protected open fun onStart() {
        // support onStart events.
    }

    protected open fun onResume() {
        // support onResume events.
    }

    protected open fun onStop() {
        // support onStop events.
    }

    protected open fun onAny() {
        // support onAny events.
    }

    /**
     * Bind this camera manager to a lifecycle.
     */
    open fun bindToLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
        lifecyclesBound++
    }

    /**
     * Unbind this camera from a lifecycle. This will pause the camera.
     */
    open fun unbindFromLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.removeObserver(this)

        lifecyclesBound--
        if (lifecyclesBound < 0) {
            Log.e(logTag, "Bound lifecycle count $lifecyclesBound is below 0")
            lifecyclesBound = 0
        }

        this.onPause()
    }

    /**
     * Determine if the adapter is currently bound.
     */
    open fun isBoundToLifecycle() = lifecyclesBound > 0

    /**
     * Execute a task with flash support.
     */
    abstract fun withFlashSupport(task: (Boolean) -> Unit)

    /**
     * Turn the camera torch on or off.
     */
    abstract fun setTorchState(on: Boolean)

    /**
     * Determine if the torch is currently on.
     */
    abstract fun isTorchOn(): Boolean

    /**
     * Determine if the device has multiple cameras.
     */
    abstract fun withSupportsMultipleCameras(task: (Boolean) -> Unit)

    /**
     * Change to a new camera.
     */
    abstract fun changeCamera()

    /**
     * Determine which camera is currently in use.
     */
    abstract fun getCurrentCamera(): Int

    /**
     * Set the focus on a particular point on the screen.
     */
    abstract fun setFocus(point: PointF)

    /**
     * Get the stream of images from the camera. This is a hot [Flow] of images with a back pressure strategy DROP.
     * Images that are not read from the flow are dropped. This flow is backed by a [Channel].
     */
    fun getImageStream(): Flow<CameraOutput> = imageChannel.receiveAsFlow()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CameraErrorListener {

    @MainThread
    fun onCameraOpenError(cause: Throwable?)

    @MainThread
    fun onCameraAccessError(cause: Throwable?)

    @MainThread
    fun onCameraUnsupportedError(cause: Throwable?)
}
