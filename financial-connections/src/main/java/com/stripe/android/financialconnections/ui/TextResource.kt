package com.stripe.android.financialconnections.ui

import android.text.TextUtils
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * TextResource is a domain specific model to represent text.
 */
internal sealed interface TextResource {
    data class Text(val value: String) : TextResource
    data class StringId(
        @StringRes val value: Int,
        val args: List<String> = emptyList()
    ) : TextResource

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
                LocalContext.current.resources.getText(value),
            ) { index, current, arg ->
                TextUtils.replace(current, arrayOf("%${index + 1}\$s"), arrayOf(arg))
            }
        }
    }
}
