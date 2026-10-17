package com.stripe.android.link

/**
 * Marks the LinkController API as being in private preview. The API may change without notice
 * and is not yet covered by semantic-versioning guarantees.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "LinkController is in private preview and may change without notice."
)
@Retention(AnnotationRetention.BINARY)
annotation class LinkControllerPreview
