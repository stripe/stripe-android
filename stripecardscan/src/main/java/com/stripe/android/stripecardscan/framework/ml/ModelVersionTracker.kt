package com.stripe.android.stripecardscan.framework.ml

private val MODEL_MAP = mutableMapOf<String, MutableSet<Triple<String, Int, Boolean>>>()

/**
 * When a ML model is loaded into memory, track the details of the model.
 */
internal fun trackModelLoaded(
    modelClass: String,
    modelVersion: String,
    modelFrameworkVersion: Int,
    success: Boolean
) {
    MODEL_MAP.getOrPut(modelClass) { mutableSetOf() }
        .add(Triple(modelVersion, modelFrameworkVersion, success))
}
