package com.stripe.android.paymentsheet.example.samples.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.ui.LinkButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class LinkExampleActivity : BasePaymentSheetActivity() {
    private val paymentCompleted = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val linkActivityResultLauncher = registerForActivityResult(
            LinkActivityContract(),
            ::onPaymentResult
        )

        val linkLauncherFlow = MutableStateFlow<LinkPaymentLauncher?>(null)

        prepareCheckout { customerConfig, clientSecret ->
            lifecycleScope.launch {
                linkLauncherFlow.value = LinkPaymentLauncher.create(this@LinkExampleActivity)
                    .setupForPaymentIntent(clientSecret, "Link Example, Inc")
            }
        }

        setContent {
            MaterialTheme {
                val inProgress by viewModel.inProgress.observeAsState(false)
                val paymentCompletedState by paymentCompleted.collectAsState()
                val status by viewModel.status.observeAsState("")
                val linkLauncher by linkLauncherFlow.collectAsState()

                if (status.isNotBlank()) {
                    snackbar.setText(status).show()
                    viewModel.statusDisplayed()
                }

                Surface(
                    modifier = Modifier.fillMaxHeight(),
                    color = BACKGROUND_COLOR
                ) {
                    ScrollableTopLevelColumn {
                        Receipt(inProgress)

                        linkLauncher?.let { launcher ->
                            LinkButton(
                                linkPaymentLauncher = launcher,
                                enabled = !inProgress && !paymentCompletedState,
                                onClick = {
                                    launcher.present(
                                        configuration = it,
                                        activityResultLauncher = linkActivityResultLauncher
                                    )
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                        } ?: run {
                            Text(
                                text = "Creating Payment Intent...",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onPaymentResult(paymentResult: LinkActivityResult) {
        viewModel.status.value = when (paymentResult) {
            is LinkActivityResult.Canceled -> "Payment canceled"
            is LinkActivityResult.Completed -> "Payment completed"
            is LinkActivityResult.Failed -> "Payment failed: ${paymentResult.error.localizedMessage}"
        }
        viewModel.inProgress.value = false
        if (paymentResult !is LinkActivityResult.Canceled) {
            paymentCompleted.value = true
        }
    }
}

@Composable
internal fun ScrollableTopLevelColumn(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}
