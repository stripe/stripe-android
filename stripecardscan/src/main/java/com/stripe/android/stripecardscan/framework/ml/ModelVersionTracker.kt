package com.stripe.android.stripecardscan.framework.ml

private val MODEL_MAP = mutableMapOf<String, MutableSet<Triple<String, Int, Boolean>>>()

/**
 * Details about a model loaded into memory.
 */
internal data class ModelLoadDetails(
    val modelClass: String,
    val modelVersion: String,
    val modelFrameworkVersion: Int,
    val success: Boolean
)

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

/**
 * Get the full list of models that were loaded into memory during this session.
 */
internal fun getLoadedModelVersions(): List<ModelLoadDetails> = MODEL_MAP.flatMap { entry ->
    entry.value.map {
        ModelLoadDetails(
            modelClass = entry.key,
            modelVersion = it.first,
            modelFrameworkVersion = it.second,
            success = it.third
        )
    }
}
