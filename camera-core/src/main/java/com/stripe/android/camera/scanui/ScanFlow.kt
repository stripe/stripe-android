package com.stripe.android.camera.scanui

import android.content.Context
import android.graphics.Rect
import androidx.annotation.RestrictTo
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * A flow for scanning something. This manages the callbacks and lifecycle of the flow.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ScanFlow<Parameters, DataType> {

    /**
     * Start the image processing flow for scanning a card.
     *
     * @param context: The context used to download analyzers if needed
     * @param imageStream: The flow of images to process
     * @param viewFinder: The location of the view finder in the previewSize
     * @param lifecycleOwner: The activity that owns this flow. The flow will pause if the activity
     * is paused
     * @param coroutineScope: The coroutine scope used to run async tasks for this flow
     * @param errorHandler: A handler to report errors to
     */
    fun startFlow(
        context: Context,
        imageStream: Flow<DataType>,
        viewFinder: Rect,
        lifecycleOwner: LifecycleOwner,
        coroutineScope: CoroutineScope,
        parameters: Parameters,
        errorHandler: (e: Exception) -> Unit
    )

    /**
     * In the event that the scan cannot complete, halt the flow to halt analyzers and free up CPU and memory.
     */
    fun cancelFlow()
}
