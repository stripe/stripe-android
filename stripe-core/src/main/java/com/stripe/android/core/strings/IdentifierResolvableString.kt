package com.stripe.android.core.strings

import android.content.Context
import androidx.annotation.StringRes

internal class IdentifierResolvableString(
    @StringRes private val id: Int,
    private val args: Array<out Any?>
) : ResolvableString {
    @Suppress("SpreadOperator")
    override fun resolve(context: Context): String {
        return context.getString(id, *resolveArgs(context, args))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (javaClass != other?.javaClass) {
            return false
        }

        return if (other is IdentifierResolvableString) {
            id == other.id && args.contentEquals(other.args)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return 31 * id + args.contentHashCode()
    }

    override fun toString(): String {
        return "resolvableString(id = $id, args = ${args.contentToString()})"
    }
}
