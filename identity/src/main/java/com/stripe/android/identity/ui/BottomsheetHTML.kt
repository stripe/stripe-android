package com.stripe.android.identity.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.compose.material.ExperimentalMaterialApi
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

/** Renders HTML with links that open a URL or an SDK bottom sheet. */
@Composable
@ExperimentalMaterialApi
internal fun BottomSheetHTML(
    html: String,
    modifier: Modifier = Modifier,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?,
    color: Color = Color.Unspecified,
    style: TextStyle,
    urlSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
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
            val uri = Uri.parse(urlString)
            when {
                URLUtil.isNetworkUrl(urlString) -> {
                    val openUrlIntent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(openUrlIntent)
                }
                uri.scheme.equals(MAILTO_SCHEME, ignoreCase = true) -> {
                    val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
                    context.startActivity(emailIntent)
                }
                urlString.startsWith(STRIPE_BOTTOM_SHEET) -> {
                    val bottomSheetId = urlString.substringAfterLast('/')
                    bottomSheets?.get(bottomSheetId)?.let { bottomSheetContent ->
                        bottomSheetViewModel.showBottomSheet(bottomSheetContent)
                    } ?: run {
                        Log.e(
                            BOTTOM_SHEET_HTML_TAG,
                            "Failed to present bottom sheet with id $bottomSheetId"
                        )
                    }
                }

                else -> {
                    Log.e(BOTTOM_SHEET_HTML_TAG, "Unknown URL string: $urlString")
                }
            }
        }
    }
}

private const val BOTTOM_SHEET_HTML_TAG = "BottomSheetHTML"
private const val MAILTO_SCHEME = "mailto"
