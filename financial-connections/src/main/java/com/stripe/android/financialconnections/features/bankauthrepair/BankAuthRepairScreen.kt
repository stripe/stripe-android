@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.bankauthrepair

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.airbnb.mvrx.compose.mavericksViewModel

@Composable
internal fun BankAuthRepairScreen() {
    // step view model
    val viewModel: BankAuthRepairViewModel = mavericksViewModel()
    Text(text = "Bank Auth Repair")
}
