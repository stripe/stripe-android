package com.stripe.link.core.data.storage

/**
 * In-memory store for local feature flag values.
 * Values start at each flag's [LocalFeatureFlag.defaultValue] and can be overridden at runtime
 * (e.g. from a developer settings screen or in tests).
 */
object LocalFeatureFlagsStore {
    private val overrides = mutableMapOf<String, Boolean>()

    /** Returns the current value for [flag], honouring any override set via [setValue]. */
    fun getValue(flag: LocalFeatureFlag): Boolean =
        overrides.getOrElse(flag.key) { flag.defaultValue }

    /** Overrides the value for [flag] until [reset] is called. */
    fun setValue(flag: LocalFeatureFlag, value: Boolean) {
        overrides[flag.key] = value
    }

    /** Removes all overrides, restoring each flag to its [LocalFeatureFlag.defaultValue]. */
    fun reset() {
        overrides.clear()
    }
}
