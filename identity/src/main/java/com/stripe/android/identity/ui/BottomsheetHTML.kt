package com.stripe.android.identity.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import com.stripe.android.identity.networking.STRIPE_BOTTOM_SHEET
import com.stripe.android.identity.networking.models.VerificationPageStaticContentBottomSheetContent
import com.stripe.android.identity.viewmodel.BottomSheetViewModel
import com.stripe.android.uicore.text.HtmlWithCustomOnClick

/**
 * Draw Html with the ability to open a web link or bottomsheet
 */
@Composable
@ExperimentalMaterialApi
internal fun BottomSheetHTML(
    html: String,
    modifier: Modifier = Modifier,
    bottomSheetViewModel: BottomSheetViewModel,
    bottomSheets: Map<String, VerificationPageStaticContentBottomSheetContent>?,
    color: Color = Color.Unspecified,
    style: TextStyle,
    urlSpanStyle: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
) {
    val context = LocalContext.current
    HtmlWithCustomOnClick(
        html = html,
        modifier = modifier,
        color = color,
        style = style,
        urlSpanStyle = urlSpanStyle
    ) { annotatedStringRanges ->
        annotatedStringRanges.firstOrNull()?.let {
            it.item.let { urlString ->
                if (urlString.startsWith("http")) {
                    val openURL = Intent(Intent.ACTION_VIEW)
                    openURL.data = Uri.parse(urlString)
                    context.startActivity(openURL)
                } else if (urlString.startsWith(STRIPE_BOTTOM_SHEET)) {
                    val bottomSheetId = it.item.substringAfterLast('/')
                    bottomSheets?.get(bottomSheetId)?.let { bottomSheetContent ->
                        bottomSheetViewModel.showBottomSheet(bottomSheetContent)
                    } ?: run {
                        Log.e(
                            BottomSheetHTMLTAG,
                            "Fail to present buttonsheet with id $bottomSheetId"
                        )
                    }
                } else {
                    Log.e(BottomSheetHTMLTAG, "unknown url string: $urlString")
                }
            }
        }
    }
}

internal const val BottomSheetHTMLTAG = "BottomSheetHTML"
