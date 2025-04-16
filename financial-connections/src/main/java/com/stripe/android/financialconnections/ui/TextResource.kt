package com.stripe.android.financialconnections.ui

import android.os.Parcelable
import android.text.TextUtils
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.parcelize.Parcelize

/**
 * TextResource is a domain specific model to represent text.
 */
internal sealed interface TextResource : Parcelable {

    @Parcelize
    data class Text(val value: CharSequence) : TextResource

    @Parcelize
    data class StringId(
        @StringRes val value: Int,
        val args: List<String> = emptyList()
    ) : TextResource

    @Parcelize
    data class PluralId(
        @StringRes val singular: Int,
        @StringRes val plural: Int,
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

            is StringId -> value.buildText(args)

            is PluralId -> when (count) {
                1 -> singular.buildText(args)
                else -> plural.buildText(args)
            }
        }
    }

    /**
     * [android.content.res.Resources.getText] does not support format args, and
     * [android.content.res.Resources.getString] does not keep annotations.
     * This function uses getText and manually handles formats.
     */
    @Composable
    private fun Int.buildText(args: List<String>): CharSequence = args.foldIndexed(
        LocalContext.current.resources.getText(this)
    ) { index, current, arg ->
        TextUtils.replace(current, arrayOf("%${index + 1}\$s"), arrayOf(arg))
    }
}

/**
 * TextResource is a domain specific model to represent images.
 */
internal sealed interface ImageResource {
    data class Network(val url: String) : ImageResource
    data class Local(@DrawableRes val resId: Int) : ImageResource
}
