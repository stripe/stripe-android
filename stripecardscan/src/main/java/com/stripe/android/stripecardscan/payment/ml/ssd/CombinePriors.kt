package com.stripe.android.stripecardscan.payment.ml.ssd

import android.util.Size
import com.stripe.android.stripecardscan.framework.ml.ssd.SizeAndCenter
import com.stripe.android.stripecardscan.framework.ml.ssd.clampAll
import com.stripe.android.stripecardscan.framework.ml.ssd.sizeAndCenter
import kotlin.math.sqrt

private const val NUMBER_OF_PRIORS = 3

internal fun combinePriors(trainedImageSize: Size): Array<SizeAndCenter> {
    val priorsOne: Array<SizeAndCenter> =
        generatePriors(
            trainedImageSize = trainedImageSize,
            featureMapSize = Size(38, 24),
            shrinkage = Size(16, 16),
            boxSizeMin = 14F,
            boxSizeMax = 30F,
            aspectRatio = 3F
        )

    val priorsTwo: Array<SizeAndCenter> =
        generatePriors(
            trainedImageSize = trainedImageSize,
            featureMapSize = Size(19, 12),
            shrinkage = Size(31, 31),
            boxSizeMin = 30F,
            boxSizeMax = 45F,
            aspectRatio = 3F
        )

    return (priorsOne + priorsTwo).apply { forEach { it.clampAll(0F, 1F) } }
}

private fun generatePriors(
    trainedImageSize: Size,
    featureMapSize: Size,
    shrinkage: Size,
    boxSizeMin: Float,
    boxSizeMax: Float,
    aspectRatio: Float
): Array<SizeAndCenter> {
    val scaleWidth = trainedImageSize.width.toFloat() / shrinkage.width
    val scaleHeight = trainedImageSize.height.toFloat() / shrinkage.height
    val ratio = sqrt(aspectRatio)

    fun generatePrior(column: Int, row: Int, sizeFactor: Float, ratio: Float) =
        sizeAndCenter(
            centerX = (column + 0.5F) / scaleWidth,
            centerY = (row + 0.5F) / scaleHeight,
            width = sizeFactor / trainedImageSize.width,
            height = sizeFactor / trainedImageSize.height * ratio
        )

    return Array(featureMapSize.width * featureMapSize.height * NUMBER_OF_PRIORS) { index ->
        val row = index / NUMBER_OF_PRIORS / featureMapSize.width
        val column = (index / NUMBER_OF_PRIORS) % featureMapSize.width
        when (index % NUMBER_OF_PRIORS) {
            0 -> generatePrior(column, row, boxSizeMin, 1F)
            1 -> generatePrior(column, row, sqrt(boxSizeMax * boxSizeMin), ratio)
            else -> generatePrior(column, row, boxSizeMin, ratio)
        }
    }
}
