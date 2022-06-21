package com.stripe.android.stripecardscan.payment.ml.ssd

import com.stripe.android.stripecardscan.framework.ml.hardNonMaximumSuppression
import com.stripe.android.stripecardscan.framework.ml.ssd.ClassifierScores
import com.stripe.android.stripecardscan.framework.ml.ssd.RectForm
import com.stripe.android.stripecardscan.framework.ml.ssd.toRectF
import com.stripe.android.stripecardscan.framework.util.filterByIndexes
import com.stripe.android.stripecardscan.framework.util.filteredIndexes
import com.stripe.android.stripecardscan.framework.util.transpose
import com.stripe.android.stripecardscan.payment.card.QUICK_READ_GROUP_LENGTH
import com.stripe.android.stripecardscan.payment.card.QUICK_READ_LENGTH
import kotlin.math.abs

internal data class OcrFeatureMapSizes(
    val layerOneWidth: Int,
    val layerOneHeight: Int,
    val layerTwoWidth: Int,
    val layerTwoHeight: Int
)

/**
 * The model outputs a particular location or a particular class of each prior before moving on to
 * the next prior. For instance, the model will output probabilities for background class
 * corresponding to all priors before outputting the probability of next class for the first prior.
 * This method serves to rearrange the output if you are using outputs from multiple layers If you
 * use outputs from single layer use the method defined above
 *
 * TODO: simplify this
 */
internal fun rearrangeOCRArray(
    locations: Array<FloatArray>,
    featureMapSizes: OcrFeatureMapSizes,
    numberOfPriors: Int,
    locationsPerPrior: Int
): Array<FloatArray> {
    val totalLocationsForAllLayers =
        (
            featureMapSizes.layerOneWidth *
                featureMapSizes.layerOneHeight *
                numberOfPriors *
                locationsPerPrior
            ) +
            (
                featureMapSizes.layerTwoWidth *
                    featureMapSizes.layerTwoHeight *
                    numberOfPriors *
                    locationsPerPrior
                )
    val rearranged = Array(1) { FloatArray(totalLocationsForAllLayers) }
    val featureMapHeights = arrayOf(
        featureMapSizes.layerOneHeight,
        featureMapSizes.layerTwoHeight
    )
    val featureMapWidths = arrayOf(
        featureMapSizes.layerOneWidth,
        featureMapSizes.layerTwoWidth
    )
    val heightIterator = featureMapHeights.iterator()
    val widthIterator = featureMapWidths.iterator()
    var offset = 0

    while (heightIterator.hasNext() && widthIterator.hasNext()) {
        val height = heightIterator.next()
        val width = widthIterator.next()
        val totalNumberOfLocationsForThisLayer = height * width * numberOfPriors * locationsPerPrior
        val stepsForLoop = height - 1
        var j: Int
        var i = 0
        var step = 0
        while (i < totalNumberOfLocationsForThisLayer) {
            while (step < height) {
                j = step
                while (j < totalNumberOfLocationsForThisLayer - stepsForLoop + step) {
                    rearranged[0][offset + i] = locations[0][offset + j]
                    i++
                    j += height
                }
                step++
            }
            offset += totalNumberOfLocationsForThisLayer
        }
    }
    return rearranged
}

/**
 * Applies non-maximum suppression to each class. Picks out the remaining boxes, the class
 * probabilities for classes that are kept, and composes all the information.
 */
internal fun extractPredictions(
    scores: Array<ClassifierScores>,
    boxes: Array<RectForm>,
    probabilityThreshold: Float,
    intersectionOverUnionThreshold: Float,
    limit: Int?,
    classifierToLabel: (Int) -> Int = { it }
): List<DetectionBox> {
    val predictions = mutableListOf<DetectionBox>()

    val classifiersScores = scores.transpose()

    for (classifier in 1 until classifiersScores.size) { // skip background classifier (index = 0)
        val classifierScores = classifiersScores[classifier]
        val filteredIndexes = classifierScores.filteredIndexes { it >= probabilityThreshold }

        if (filteredIndexes.isNotEmpty()) {
            val filteredScores = classifierScores.filterByIndexes(filteredIndexes)
            val filteredBoxes = boxes.filterByIndexes(filteredIndexes)

            val indexes =
                hardNonMaximumSuppression(
                    boxes = filteredBoxes,
                    probabilities = filteredScores,
                    iouThreshold = intersectionOverUnionThreshold,
                    limit = limit
                )
            for (index in indexes) {
                predictions.add(
                    DetectionBox(
                        rect = filteredBoxes[index].toRectF(),
                        confidence = filteredScores[index],
                        label = classifierToLabel(classifier)
                    )
                )
            }
        }
    }

    return predictions
}

/**
 * Determine if the number is displayed horizontally or both horizontally and vertically. We do this
 * by finding the median vertical coordinate center. If the number is displayed horizontally, the
 * deviation of all the number boxes and the median center should be minimal since they are laid on
 * roughly the same horizontal line. In this case we just need to sort from left to right to order
 * the number boxes. Additionally, we also filter out boxes that are outside the same horizontal
 * line. This is done to exclude information such as phone numbers or expiry. On the other hand, if
 * the aggregate deviation of the number box centers from the median center is above a threshold,
 * i.e. the number has both vertical and horizontal components we need to sort from left to right
 * and top to bottom to order the boxes according to the card number.
 */
internal fun determineLayoutAndFilter(
    detectedBoxes: List<DetectionBox>,
    verticalOffset: Float
): List<DetectionBox> {
    if (detectedBoxes.isEmpty()) {
        return detectedBoxes
    }

    // calculate the median center and height of each digit in the image
    val centers = detectedBoxes.map { it.rect.centerY() }.sorted()
    val heights = detectedBoxes.map { it.rect.height() }.sorted()

    val medianCenter = centers.elementAt(centers.size / 2)
    val medianHeight = heights.elementAt(heights.size / 2)
    val aggregateDeviation = centers.map { abs(it - medianCenter) }.sum()

    if (
        aggregateDeviation > verticalOffset * medianHeight &&
        detectedBoxes.size == QUICK_READ_LENGTH
    ) {
        val quickReadGroups = detectedBoxes
            .sortedBy { it.rect.centerY() }
            .chunked(QUICK_READ_GROUP_LENGTH)
            .map { it.sortedBy { detectionBox -> detectionBox.rect.left } }

        // Quick read groups should be in vertical blocks. Make sure the blocks are not horizontally
        // laid out
        if (
            quickReadGroups[1].first().rect.centerX() < quickReadGroups[0].last().rect.centerX() &&
            quickReadGroups[1].last().rect.centerX() > quickReadGroups[0].first().rect.centerX()
        ) {
            return quickReadGroups.flatten()
        }
    }

    return detectedBoxes
        .sortedBy { it.rect.left }
        .filter { abs(it.rect.centerY() - medianCenter) <= medianHeight }
}
