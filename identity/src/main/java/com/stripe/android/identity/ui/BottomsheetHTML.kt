package com.stripe.android.identity.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.identity.networking.STRIPE_BOTTOM_SHEET
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.viewmodel.BottomSheetViewModel
import com.stripe.android.uicore.text.HtmlWithCustomOnClick

/**
 * Draw Html with the ability to open a web link or bottomsheet
 */
@Composable
internal fun BottomSheetHTML(
    html: String,
    modifier: Modifier = Modifier,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?,
    color: Color = Color.Unspecified,
    style: TextStyle,
    urlSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    onError: (Throwable) -> Unit = {}
) {
    val context = LocalContext.current
    val bottomSheetViewModel = viewModel<BottomSheetViewModel>()
    HtmlWithCustomOnClick(
        html = html,
        modifier = modifier,
        color = color,
        style = style,
        urlSpanStyle = urlSpanStyle
    ) { annotatedStringRanges ->
        annotatedStringRanges.firstOrNull()?.item?.let { urlString ->
            when {

                urlString.startsWith(STRIPE_BOTTOM_SHEET) -> {
                    val bottomSheetId = urlString.substringAfterLast('/')
                    bottomSheets?.get(bottomSheetId)?.let { bottomSheetContent ->
                        bottomSheetViewModel.showBottomSheet(bottomSheetContent)
                    } ?: run {
                        val error = IllegalStateException(
                            "Fail to present bottomsheet with id $bottomSheetId"
                        )
                        Log.e(BottomSheetHTMLTAG, error.message, error)
                        onError(error)
                    }
                }

                else -> {
                    runCatching {
                        val openURL = Intent(Intent.ACTION_VIEW)
                        openURL.data = Uri.parse(urlString)
                        context.startActivity(openURL)
                    }.onFailure {
                        val error = IllegalStateException("Failed to open url: $urlString", it)
                        Log.e(BottomSheetHTMLTAG, error.message, error)
                        onError(error)
                    }
                }
            }
        }
    }
}

internal const val BottomSheetHTMLTAG = "BottomSheetHTML"
