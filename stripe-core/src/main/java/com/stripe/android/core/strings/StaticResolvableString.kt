package com.stripe.android.core.strings

import android.content.Context

internal class StaticResolvableString(
    private val value: String,
    private val args: Array<out Any?>,
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return value.format(*resolveArgs(context, args))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (javaClass != other?.javaClass) {
            return false
        }

        return if (other is StaticResolvableString) {
            value == other.value && args.contentEquals(other.args)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return 31 * value.hashCode() + args.contentHashCode()
    }

    override fun toString(): String {
        return "resolvableString(value = $value, args = ${args.contentToString()})"
    }
}
