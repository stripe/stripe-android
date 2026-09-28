package com.stripe.android.paymentelement.confirmation

import java.util.Objects

internal interface ConfirmationMetadata {
    interface Key<V>

    operator fun <V> get(key: Key<V>): V?
}

internal class MutableConfirmationMetadata : ConfirmationMetadata {
    private val mappedMetadata = mutableMapOf<ConfirmationMetadata.Key<*>, Any?>()

    override fun <V> get(key: ConfirmationMetadata.Key<V>): V? {
        @Suppress("UNCHECKED_CAST")
        return mappedMetadata[key] as V?
    }

    operator fun <V> set(key: ConfirmationMetadata.Key<V>, value: V) {
        mappedMetadata[key] = value
    }

    override fun equals(other: Any?): Boolean {
        return other is MutableConfirmationMetadata &&
            other.mappedMetadata == mappedMetadata
    }

    override fun hashCode(): Int {
        return Objects.hash(mappedMetadata)
    }
}
