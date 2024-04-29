package com.stripe.android.paymentsheet.example.playground.customersheet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stripe.android.customersheet.CustomerSheetResultCallback
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.rememberCustomerSheet
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceBottomSheetDialogFragment
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore
import com.stripe.android.paymentsheet.example.playground.activity.QrCodeActivity
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.CustomerSheetPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.SettingsUi
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentMethodSelector

internal class CustomerSheetPlaygroundActivity : AppCompatActivity() {
    companion object {
        fun createTestIntent(settingsJson: String): Intent {
            return Intent(
                Intent.ACTION_VIEW,
                CustomerSheetPlaygroundUrlHelper.createUri(settingsJson)
            )
        }
    }

    val viewModel: CustomerSheetPlaygroundViewModel by viewModels {
        CustomerSheetPlaygroundViewModel.Factory(
            applicationSupplier = { application },
            uriSupplier = { intent.data },
        )
    }

    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @OptIn(ExperimentalCustomerSheetApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val playgroundSettings by viewModel.playgroundSettingsFlow.collectAsState()
            val localPlaygroundSettings = playgroundSettings ?: return@setContent

            val playgroundState by viewModel.playgroundState.collectAsState()
            val context = LocalContext.current

            LaunchedEffect(viewModel.status) {
                viewModel.status.collect { status ->
                    Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                }
            }

            PlaygroundTheme(
                content = {
                    SettingsUi(playgroundSettings = localPlaygroundSettings)

                    AppearanceButton()

                    QrCodeButton(playgroundSettings = localPlaygroundSettings)
                },
                bottomBarContent = {
                    ReloadButton()

                    AnimatedContent(
                        label = PLAYGROUND_BOTTOM_BAR_LABEL,
                        targetState = playgroundState != null,
                    ) {
                        Column {
                            PlaygroundStateUi(
                                playgroundState = playgroundState,
                                callback = viewModel::onCustomerSheetCallback
                            )
                        }
                    }
                },
            )
        }
    }

    @Composable
    private fun AppearanceButton() {
        Button(
            onClick = {
                val bottomSheet = AppearanceBottomSheetDialogFragment.newInstance()
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Change Appearance")
        }
    }

    @Composable
    private fun QrCodeButton(playgroundSettings: CustomerSheetPlaygroundSettings) {
        val context = LocalContext.current
        Button(
            onClick = {
                context.startActivity(
                    QrCodeActivity.create(
                        context = context,
                        settingsUri = CustomerSheetPlaygroundUrlHelper.createUri(
                            playgroundSettings.snapshot().asJsonString()
                        ),
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("QR code for current settings")
        }
    }

    @Composable
    private fun ReloadButton() {
        Button(
            onClick = viewModel::reset,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(RELOAD_TEST_TAG),
        ) {
            Text("Reload")
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    @Composable
    private fun PlaygroundStateUi(
        playgroundState: CustomerSheetPlaygroundState?,
        callback: CustomerSheetResultCallback,
    ) {
        if (playgroundState == null) {
            return
        }

        val customerSheet = rememberCustomerSheet(
            configuration = playgroundState.customerSheetConfiguration(),
            customerAdapter = playgroundState.adapter,
            callback = callback,
        )

        LaunchedEffect(customerSheet) {
            viewModel.fetchOption(customerSheet)
        }

        val loaded = playgroundState.optionState as? CustomerSheetPlaygroundState.PaymentOptionState.Loaded
        val option = loaded?.paymentOption

        PaymentMethodSelector(
            isEnabled = playgroundState.optionState is CustomerSheetPlaygroundState.PaymentOptionState.Loaded,
            paymentMethodLabel = option?.label ?: "Select",
            paymentMethodPainter = option?.iconPainter,
            onClick = customerSheet::present
        )
    }
}

@Composable
private fun PlaygroundTheme(
    content: @Composable ColumnScope.() -> Unit,
    bottomBarContent: @Composable ColumnScope.() -> Unit,
) {
    val colors = if (isSystemInDarkTheme() || AppearanceStore.forceDarkMode) {
        darkColors()
    } else {
        lightColors()
    }
    MaterialTheme(
        typography = MaterialTheme.typography.copy(
            body1 = MaterialTheme.typography.body1.copy(fontSize = 14.sp)
        ),
        colors = colors,
    ) {
        Surface(
            color = MaterialTheme.colors.background,
        ) {
            Scaffold(
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.surface)
                            .animateContentSize()
                    ) {
                        Divider()
                        Column(
                            content = bottomBarContent,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        )
                    }
                },
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxSize()
                            .padding(16.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

const val RELOAD_TEST_TAG = "RELOAD"
private const val PLAYGROUND_BOTTOM_BAR_LABEL = "CustomerSheetPlaygroundBottomBar"
