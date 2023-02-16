package com.stripe.android.paymentsheet

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ScrollView
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncherContract
import com.stripe.android.link.ui.verification.LinkVerificationDialog
import com.stripe.android.paymentsheet.databinding.ActivityPaymentSheetBinding
import com.stripe.android.paymentsheet.databinding.FragmentPaymentSheetPrimaryButtonBinding
import com.stripe.android.paymentsheet.state.WalletsContainerState
import com.stripe.android.paymentsheet.ui.BaseSheetActivity
import com.stripe.android.paymentsheet.ui.ErrorMessage
import com.stripe.android.paymentsheet.ui.GooglePayButton
import com.stripe.android.paymentsheet.ui.GooglePayDividerUi
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBar
import com.stripe.android.paymentsheet.ui.convert
import com.stripe.android.paymentsheet.utils.launchAndCollectIn
import com.stripe.android.ui.core.elements.H4Text
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.stripeColors
import com.stripe.android.uicore.text.Html
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import java.security.InvalidParameterException

internal class PaymentSheetActivity : BaseSheetActivity<PaymentSheetResult>() {
    @VisibleForTesting
    internal val viewBinding by lazy {
        ActivityPaymentSheetBinding.inflate(layoutInflater)
    }

    @VisibleForTesting
    internal var viewModelFactory: ViewModelProvider.Factory = PaymentSheetViewModel.Factory {
        requireNotNull(starterArgs)
    }

    override val viewModel: PaymentSheetViewModel by viewModels { viewModelFactory }

    private val starterArgs: PaymentSheetContract.Args? by lazy {
        PaymentSheetContract.Args.fromIntent(intent)
    }

    override val rootView: ViewGroup by lazy { viewBinding.root }
    override val bottomSheet: ViewGroup by lazy { viewBinding.bottomSheet }

    private val linkAuthView: ComposeView by lazy { viewBinding.linkAuth }
    private val scrollView: ScrollView by lazy { viewBinding.scrollView }
    private val header: ComposeView by lazy { viewBinding.header }
    private val fragmentContainerParent: ViewGroup by lazy { viewBinding.fragmentContainerParent }
    private val notesView: ComposeView by lazy { viewBinding.notes }
    private val bottomSpacer: View by lazy { viewBinding.bottomSpacer }

    private val topContainer by lazy { viewBinding.topContainer }
    private val linkButton by lazy { viewBinding.linkButton }
    private val googlePayDivider by lazy { viewBinding.googlePayDivider }

    override fun onCreate(savedInstanceState: Bundle?) {
        val validationResult = initializeArgs()
        super.onCreate(savedInstanceState)

        val validatedArgs = validationResult.getOrNull()
        if (validatedArgs == null) {
            finishWithError(error = validationResult.exceptionOrNull())
            return
        }

        viewModel.registerFromActivity(this)

        viewModel.setupGooglePay(
            lifecycleScope,
            registerForActivityResult(
                GooglePayPaymentMethodLauncherContract(),
                viewModel::onGooglePayResult
            )
        )

        starterArgs?.statusBarColor?.let {
            window.statusBarColor = it
        }
        setContentView(viewBinding.root)

        fragmentContainerParent.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val elevation = resources.getDimension(R.dimen.stripe_paymentsheet_toolbar_elevation)
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewBinding.topBar.elevation = if (scrollView.scrollY > 0) {
                elevation
            } else {
                0f
            }
        }

        // This is temporary until we embed the top bar in a Scaffold
        viewBinding.topBar.clipToPadding = false

        viewBinding.topBar.setContent {
            StripeTheme {
                PaymentSheetTopBar(viewModel)
            }
        }

        setupHeader()
        setupTopContainer()
        setupNotes()

        linkButton.apply {
            onClick = viewModel::handleLinkPressed
            linkPaymentLauncher = linkLauncher
        }

        viewBinding.topMessage.setContent {
            StripeTheme {
                val buttonState by viewModel.googlePayButtonState.collectAsState(initial = null)

                buttonState?.errorMessage?.let { error ->
                    ErrorMessage(
                        error = error.message,
                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 1.dp),
                    )
                }
            }
        }

        viewBinding.contentContainer.setContent {
            StripeTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                currentScreen.Content(
                    viewModel = viewModel,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        viewBinding.message.setContent {
            StripeTheme {
                val buttonState by viewModel.buyButtonState.collectAsState(initial = null)

                buttonState?.errorMessage?.let { error ->
                    ErrorMessage(
                        error = error.message,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 20.dp),
                    )
                }
            }
        }

        viewBinding.buttonContainer.setContent {
            AndroidViewBinding(
                factory = FragmentPaymentSheetPrimaryButtonBinding::inflate,
            )
        }

        viewModel.processing.filter { it }.launchAndCollectIn(this) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }

        viewModel.paymentSheetResult.filterNotNull().launchAndCollectIn(this) {
            closeSheet(it)
        }

        viewModel.buttonsEnabled.launchAndCollectIn(this) { enabled ->
            linkButton.isEnabled = enabled
        }

        viewModel.linkHandler.showLinkVerificationDialog.launchAndCollectIn(this) { show ->
            linkAuthView.setContent {
                if (show) {
                    LinkVerificationDialog(
                        linkLauncher = linkLauncher,
                        onResult = linkHandler::handleLinkVerificationResult,
                    )
                }
            }
        }

        viewModel.contentVisible.launchAndCollectIn(this) {
            scrollView.isVisible = it
        }

        viewModel.primaryButtonUIState.launchAndCollectIn(this) { state ->
            state?.let {
                bottomSpacer.isVisible = state.visible
            }
        }

        bottomSpacer.isVisible = true
    }

    private fun initializeArgs(): Result<PaymentSheetContract.Args?> {
        val starterArgs = this.starterArgs

        val result = if (starterArgs == null) {
            Result.failure(defaultInitializationError())
        } else {
            try {
                starterArgs.config?.validate()
                starterArgs.clientSecret.validate()
                starterArgs.config?.appearance?.parseAppearance()
                Result.success(starterArgs)
            } catch (e: InvalidParameterException) {
                Result.failure(e)
            }
        }

        earlyExitDueToIllegalState = result.isFailure
        return result
    }

    private fun setupTopContainer() {
        setupGooglePayButton()

        viewModel.walletsContainerState.launchAndCollectIn(this) { config ->
            linkButton.isVisible = config.showLink
            topContainer.isVisible = config.shouldShow
        }

        googlePayDivider.setContent {
            val containerState by viewModel.walletsContainerState.collectAsState(
                initial = WalletsContainerState(),
            )

            StripeTheme {
                if (containerState.shouldShow) {
                    val text = stringResource(containerState.dividerTextResource)
                    GooglePayDividerUi(text)
                }
            }
        }
    }

    private fun setupHeader() {
        header.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val text = viewModel.headerText.collectAsState(null)
                text.value?.let {
                    StripeTheme {
                        H4Text(
                            text = stringResource(it),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }

    private fun setupNotes() {
        viewModel.notesText.launchAndCollectIn(this) { text ->
            val showNotes = text != null
            text?.let {
                notesView.setContent {
                    StripeTheme {
                        Html(
                            html = text,
                            color = MaterialTheme.stripeColors.subtitle,
                            style = MaterialTheme.typography.body1.copy(
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
            notesView.isVisible = showNotes
        }
    }

    private fun setupGooglePayButton() {
        viewBinding.googlePayButton.setContent {
            val containerState by viewModel.walletsContainerState.collectAsState(
                initial = WalletsContainerState(),
            )

            val buttonState by viewModel.googlePayButtonState.collectAsState(initial = null)
            val isEnabled by viewModel.buttonsEnabled.collectAsState(initial = false)

            if (containerState.showGooglePay) {
                GooglePayButton(
                    state = buttonState?.convert(),
                    isEnabled = isEnabled,
                    onPressed = viewModel::checkoutWithGooglePay,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
        }
    }

    override fun setActivityResult(result: PaymentSheetResult) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(PaymentSheetContract.Result(result).toBundle())
        )
    }

    override fun onDestroy() {
        if (!earlyExitDueToIllegalState) {
            viewModel.unregisterFromActivity()
        }
        super.onDestroy()
    }

    private fun finishWithError(error: Throwable?) {
        val e = error ?: defaultInitializationError()
        setActivityResult(PaymentSheetResult.Failed(e))
        finish()
    }

    private fun defaultInitializationError(): IllegalArgumentException {
        return IllegalArgumentException("PaymentSheet started without arguments.")
    }

    internal companion object {
        internal const val EXTRA_STARTER_ARGS = BaseSheetActivity.EXTRA_STARTER_ARGS
    }
}
