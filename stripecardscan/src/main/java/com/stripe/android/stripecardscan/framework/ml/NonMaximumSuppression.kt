package com.stripe.android.stripecardscan.framework.ml

import com.stripe.android.stripecardscan.framework.ml.ssd.RectForm
import com.stripe.android.stripecardscan.framework.ml.ssd.areaClamped
import com.stripe.android.stripecardscan.framework.ml.ssd.overlapWith

/**
 * In this project we implement HARD NMS and NOT Soft NMS. I highly recommend checkout SOFT NMS
 * implementation of Facebook Detectron Framework.
 *
 * See https://towardsdatascience.com/non-maximum-suppression-nms-93ce178e177c
 *
 * @param boxes: Detected boxes
 * @param probabilities: Probabilities of the given boxes
 * @param iouThreshold: intersection over union threshold.
 * @param limit: keep this number of results. If limit <= 0, keep all the results.
 *
 * @return pickedIndices: a list of indexes of the kept boxes
 */
internal fun hardNonMaximumSuppression(
    boxes: Array<FloatArray>,
    probabilities: FloatArray,
    iouThreshold: Float,
    limit: Int?
): ArrayList<Int> {
    val indexArray = probabilities.indices.sortedByDescending { probabilities[it] }
        .take(200)
        .toMutableList()
    val pickedIndexes = ArrayList<Int>()

    while (indexArray.isNotEmpty()) {
        val current = indexArray.removeAt(0)
        pickedIndexes.add(current)

        if (pickedIndexes.size == limit) {
            return pickedIndexes
        }

        val iterator = indexArray.iterator()
        while (iterator.hasNext()) {
            if (intersectionOverUnionOf(boxes[current], boxes[iterator.next()]) >= iouThreshold) {
                iterator.remove()
            }
        }
    }

    return pickedIndexes
}

/**
 * Return intersection-over-union (Jaccard index) of boxes.
 *
 * Args:
 * boxes0 (N, 4): ground truth boxes.
 * boxes1 (N or 1, 4): predicted boxes.
 * eps: a small number to avoid 0 as denominator.
 * Returns: iou (N): IOU values
 */
private fun intersectionOverUnionOf(currentBox: RectForm, nextBox: RectForm): Float {
    val eps = 0.00001f
    val overlapArea = nextBox.overlapWith(currentBox).areaClamped()
    val nextArea = nextBox.areaClamped()
    val currentArea = currentBox.areaClamped()
    return overlapArea / (nextArea + currentArea - overlapArea + eps)
}
