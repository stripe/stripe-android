package com.stripe.android.financialconnections.ui

import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * TextResource is a domain specific model to represent text.
 */
internal sealed interface TextResource {
    data class Text(val value: CharSequence) : TextResource
    data class StringId(
        @StringRes val value: Int,
        val args: List<String> = emptyList()
    ) : TextResource

    data class PluralId(
        @PluralsRes val value: Int,
        val count: Int,
        val args: List<String> = emptyList()
    ) : TextResource

    /**
     * Return the string value associated with a particular resource ID.
     * The returned object will be a String if this is a plain string;
     * it will be some other type of CharSequence if it is styled.
     */
    @Composable
    fun toText(): CharSequence {
        return when (this) {
            is Text -> value
            /**
             * [android.content.res.Resources.getText] does not support format args, and
             * [android.content.res.Resources.getString] does not keep annotations.
             * This function uses getText and manually handles formats.
             */
            is StringId -> args.foldIndexed(
                LocalContext.current.resources.getText(value)
            ) { index, current, arg ->
                TextUtils.replace(current, arrayOf("%${index + 1}\$s"), arrayOf(arg))
            }

            /**
             * [android.content.res.Resources.getQuantityText] does not support format args, and
             * [android.content.res.Resources.getQuantityString] does not keep annotations.
             * This function uses getText and manually handles formats.
             */
            is PluralId -> args.foldIndexed(
                LocalContext.current.resources.getQuantityText(value, count),
            ) { index, current, arg ->
                TextUtils.replace(current, arrayOf("%${index + 1}\$s"), arrayOf(arg))
            }
        }
    }
}

/**
 * TextResource is a domain specific model to represent images.
 */
internal sealed interface ImageResource {
    data class Network(val url: String) : ImageResource
    data class Local(@DrawableRes val resId: Int) : ImageResource
}
