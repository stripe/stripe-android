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

/**
 * Runs greedy NonMaxSuppression over the raw predictions. Greedy NMS looks for the local maximas
 * ("peaks") in the prediction confidences of the consecutive same predictions, keeps those,
 * and replaces the other values as the background class.
 *
 * Example: given the following [rawPredictions] and [confidence] pair
 *   [rawPredictions]: [LABEL0, LABEL0, LABEL0, LABEL1, LABEL1, LABEL1]
 *   [confidence]:     [0.1,    0.2,    0.4,    0.3,    0.5,   0.3]
 *   Output:           [BACKGROUND, BACKGROUND, LABEL0, BACKGROUND, LABEL, BACKGROUND]
 */
internal fun <Input> greedyNonMaxSuppression(
    rawPredictions: Array<Input>,
    confidence: FloatArray,
    backgroundClass: Input
): Array<Input> {
    val digits = rawPredictions.clone()

    // greedy non max suppression
    for (idx in 0 until digits.size - 1) {
        if (digits[idx] != backgroundClass && digits[idx + 1] != backgroundClass) {
            if (confidence[idx] < confidence[idx + 1]) {
                digits[idx] = backgroundClass
            } else {
                digits[idx + 1] = backgroundClass
            }
        }
    }
    return digits
}
