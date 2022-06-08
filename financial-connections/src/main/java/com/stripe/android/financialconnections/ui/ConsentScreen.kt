package com.stripe.android.financialconnections.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.compose.mavericksActivityViewModel
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.presentation.ConsentState
import com.stripe.android.financialconnections.presentation.ConsentViewModel
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel

@Composable
internal fun ConsentScreen() {
    // get shared configuration from activity state
    val activityViewModel: FinancialConnectionsSheetNativeViewModel = mavericksActivityViewModel()
    val manifest = activityViewModel.collectAsState { it.manifest }

    // update step state when manifest changes
    val viewModel: ConsentViewModel = mavericksViewModel()
    LaunchedEffect(manifest.value) { viewModel.onManifestChanged(manifest.value) }

    val state = viewModel.collectAsState()
    ConsentContent(
        state = state.value,
        onContinueClick = viewModel::onContinueClick
    )
}

@Composable
private fun ConsentContent(
    state: ConsentState,
    onContinueClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                text = state.title,
                style = MaterialTheme.typography.h4
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Button(
                onContinueClick
            ) {
                Text(text = "Agree")
            }
        }
    }
}
