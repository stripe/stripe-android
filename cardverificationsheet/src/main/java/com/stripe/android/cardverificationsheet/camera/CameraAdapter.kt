package com.stripe.android.cardverificationsheet.camera

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.util.Log
import android.view.Surface
import androidx.annotation.CheckResult
import androidx.annotation.IntDef
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.stripe.android.cardverificationsheet.framework.Config
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
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

abstract class CameraAdapter<CameraOutput> : LifecycleObserver {

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
                if (!it) Log.e(Config.logTag, "System feature 'FEATURE_CAMERA_ANY' is unavailable")
            }

        /**
         * Calculate degrees from a [RotationValue].
         */
        @CheckResult
        fun Int.rotationToDegrees(): Int = this * 90
    }

    protected fun sendImageToStream(image: CameraOutput) = try {
        imageChannel.trySend(image).onClosed {
            Log.w(Config.logTag, "Attempted to send image to closed channel", it)
        }.onFailure {
            Log.w(Config.logTag, "Failure when sending image to channel", it)
        }
    } catch (e: ClosedSendChannelException) {
        Log.w(Config.logTag, "Attempted to send image to closed channel")
    } catch (t: Throwable) {
        Log.e(Config.logTag, "Unable to send image to channel", t)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroyed() {
        runBlocking { imageChannel.close() }
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
            Log.e(Config.logTag, "Bound lifecycle count $lifecyclesBound is below 0")
            lifecyclesBound = 0
        }

        this.onPause()
    }

    /**
     * Determine if the adapter is currently bound.
     */
    open fun isBoundToLifecycle() = lifecyclesBound > 0

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    open fun onPause() {
        // support OnPause events.
    }

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

interface CameraErrorListener {

    @MainThread
    fun onCameraOpenError(cause: Throwable?)

    @MainThread
    fun onCameraAccessError(cause: Throwable?)

    @MainThread
    fun onCameraUnsupportedError(cause: Throwable?)
}
