package com.stripe.android.financialconnections.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

@Immutable
data class FinancialConnectionsTypography(
    val subtitle: TextStyle,
    val subtitleEmphasized: TextStyle,
    val heading: TextStyle,
    val subheading: TextStyle,
    val kicker: TextStyle,
    val body: TextStyle,
    val bodyEmphasized: TextStyle,
    val detail: TextStyle,
    val detailEmphasized: TextStyle,
    val caption: TextStyle,
    val captionEmphasized: TextStyle,
    val captionTight: TextStyle,
    val captionTightEmphasized: TextStyle,
)
