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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.mvrx.compose.mavericksViewModel
import com.stripe.android.financialconnections.presentation.ConsentViewModel

@Composable
internal fun ConsentScreen() {
    val viewModel: ConsentViewModel = mavericksViewModel()
    ConsentContent { viewModel.onContinueClick() }
}

@Composable
private fun ConsentContent(
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
                text = "Company works with Stripe to link your accounts",
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