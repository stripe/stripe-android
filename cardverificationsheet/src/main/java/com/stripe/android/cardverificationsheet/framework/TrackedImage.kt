package com.stripe.android.cardverificationsheet.framework

/**
 * An image with a stat tracker.
 */
internal data class TrackedImage<ImageType>(
    val image: ImageType,
    val tracker: StatTracker,
)
