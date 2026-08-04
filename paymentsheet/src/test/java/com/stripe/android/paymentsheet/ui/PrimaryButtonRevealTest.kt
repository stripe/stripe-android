package com.stripe.android.paymentsheet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.stripe.android.screenshottesting.PaparazziRule
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

internal class PrimaryButtonRevealTest {
    @get:Rule
    val paparazziRule = PaparazziRule(
        boxModifier = Modifier.fillMaxSize(),
    )

    @Test
    fun `moves enabled primary button into simulated viewport`() {
        paparazziRule.gif(end = 1_000L) {
            val scrollState = rememberScrollState()
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            var isEnabled by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(500L)
                isEnabled = true
            }

            RevealPrimaryButtonWhenEnabled(
                isEnabled = isEnabled,
                isImeVisible = true,
                bringIntoViewRequester = bringIntoViewRequester,
            )

            Box(
                modifier = Modifier
                    .height(240.dp)
                    .clipToBounds(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                ) {
                    Text("Simulated visible viewport")
                    Spacer(modifier = Modifier.height(320.dp))
                    Button(
                        onClick = {},
                        enabled = isEnabled,
                        modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
