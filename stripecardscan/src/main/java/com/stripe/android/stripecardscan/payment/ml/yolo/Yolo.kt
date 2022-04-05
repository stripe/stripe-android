package com.stripe.android.stripecardscan.payment.ml.yolo

import android.graphics.RectF
import android.util.Size
import com.stripe.android.stripecardscan.framework.ml.ssd.softMax
import com.stripe.android.stripecardscan.framework.util.indexOfMax
import com.stripe.android.stripecardscan.payment.ml.ssd.DetectionBox
import kotlin.math.exp

internal fun sigmoid(x: Float): Float = (1.0f / (1.0f + exp((-x))))

/**
 * Takes a layer from the raw YOLO model output and performs post-processing on it,
 * returning a List<DetectionBox>
 */
internal fun processYoloLayer(
    layer: Array<Array<FloatArray>>,
    anchors: Array<Pair<Int, Int>>,
    imageSize: Size,
    numClasses: Int,
    confidenceThreshold: Float
): List<DetectionBox> {
    val results = mutableListOf<DetectionBox>()
    for (i in layer.indices) for (j in layer[i].indices) for (k in 0..2) {
        val offset = (numClasses + 5) * k
        var confidence = sigmoid(layer[i][j][offset + 4])
        val confidenceClasses = (0 until numClasses).map { layer[i][j][offset + 5 + it] }
            .toFloatArray()
        confidenceClasses.softMax()

        val objectId = confidenceClasses.indexOfMax() ?: continue
        val maxClass = confidenceClasses[objectId]
        confidence *= maxClass

        if (confidence > confidenceThreshold) {
            val x = (j + sigmoid(layer[i][j][offset])) / layer.size
            val y = (i + sigmoid(layer[i][j][offset + 1])) / layer.size
            val w =
                exp(layer[i][j][offset + 2]) * anchors[k].first / imageSize.width
            val h =
                exp(layer[i][j][offset + 3]) * anchors[k].second / imageSize.height
            val r = RectF(
                x - w / 2,
                y - h / 2,
                x + w / 2,
                y + h / 2
            )
            results.add(
                DetectionBox(
                    rect = r,
                    confidence = confidence,
                    label = objectId
                )
            )
        }
    }
    return results
}
