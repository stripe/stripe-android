package com.stripe.android.paymentsheet.model

internal data class PromoBadgesState private constructor(
    private val promoBadges: Map<String, Boolean>,
) {

    operator fun get(key: String): Boolean {
        return promoBadges[key] ?: true
    }

    fun set(key: String, value: Boolean): PromoBadgesState {
        return copy(
            promoBadges = promoBadges + mapOf(key to value),
        )
    }

    companion object {
        fun empty() = PromoBadgesState(emptyMap())
    }
}
