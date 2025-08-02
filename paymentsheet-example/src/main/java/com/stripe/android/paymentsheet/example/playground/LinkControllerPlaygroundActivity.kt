package com.stripe.android.paymentsheet.example.playground

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.stripe.android.paymentsheet.example.samples.ui.shared.PaymentSheetExampleTheme

@Suppress("LongMethod")
internal class LinkControllerPlaygroundActivity : AppCompatActivity() {
    private val viewModel: LinkControllerPlaygroundViewModel by viewModels()
    private val linkController get() = viewModel.linkController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val playgroundState = intent.getParcelableExtra<PlaygroundState.Payment?>(PLAYGROUND_STATE_KEY)!!
        val linkControllerConfig = playgroundState.asPaymentState()?.linkControllerConfiguration()

        if (linkControllerConfig == null) {
            finish()
            return
        }

        viewModel.configureLinkController(linkControllerConfig)

        setContent {
            val linkControllerPresenter = remember {
                viewModel.linkController.createPresenter(
                    activity = this,
                    presentPaymentMethodsCallback = viewModel::onLinkControllerPresentPaymentMethod,
                    authenticationCallback = viewModel::onLinkControllerAuthentication,
                )
            }

            val linkControllerPlaygroundState by viewModel.linkControllerState.collectAsState()
            val linkControllerState by linkController.state(this).collectAsState()

            PaymentSheetExampleTheme {
                Scaffold { paddingValues ->
                    LinkControllerUi(
                        modifier = Modifier.padding(paddingValues),
                        controllerState = linkControllerState,
                        playgroundState = linkControllerPlaygroundState,
                        onEmailChange = viewModel::onEmailChange,
                        onPaymentMethodButtonClick = { email ->
                            linkControllerPresenter.paymentSelectionHint =
                                "Lorem ipsum dolor sit amet consectetur adipiscing elit."
                            linkControllerPresenter.presentPaymentMethods(email = email.takeIf { it.isNotBlank() })
                        },
                        onCreatePaymentMethodClick = viewModel::onCreatePaymentMethodClick,
                        onAuthenticationClick = { email, existingOnly ->
                            val cleanedEmail = email.takeIf { it.isNotBlank() } ?: ""
                            if (existingOnly) {
                                linkControllerPresenter.authenticateExistingConsumer(cleanedEmail)
                            } else {
                                linkControllerPresenter.authenticate(cleanedEmail)
                            }
                        },
                        onRegisterConsumerClick = viewModel::onRegisterConsumerClick,
                        onErrorMessage = { viewModel.status.value = StatusMessage(it) },
                    )
                }
            }

            val status by viewModel.status.collectAsState()
            val context = LocalContext.current
            LaunchedEffect(status) {
                if (!status?.message.isNullOrEmpty() && status?.hasBeenDisplayed == false) {
                    Toast.makeText(context, status?.message, Toast.LENGTH_LONG).show()
                }
                viewModel.status.value = status?.copy(hasBeenDisplayed = true)
            }
        }
    }

    companion object {
        private const val PLAYGROUND_STATE_KEY = "playgroundState"

        fun create(context: Context, playgroundState: PlaygroundState.Payment): Intent {
            return Intent(context, LinkControllerPlaygroundActivity::class.java).apply {
                putExtra(PLAYGROUND_STATE_KEY, playgroundState)
            }
        }
    }
}
