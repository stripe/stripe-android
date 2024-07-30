package com.stripe.android.stripe3ds2.views

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.os.BundleCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.stripe.android.stripe3ds2.R
import com.stripe.android.stripe3ds2.databinding.StripeChallengeFragmentBinding
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.transaction.ChallengeAction
import com.stripe.android.stripe3ds2.transaction.ChallengeActionHandler
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResult
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.ErrorRequestExecutor
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transaction.TransactionTimer
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.ErrorData
import com.stripe.android.stripe3ds2.transactions.UiType
import kotlin.coroutines.CoroutineContext

internal class ChallengeFragment(
    private val uiCustomization: StripeUiCustomization,
    private val transactionTimer: TransactionTimer,
    private val errorRequestExecutor: ErrorRequestExecutor,
    private val errorReporter: ErrorReporter,
    private val challengeActionHandler: ChallengeActionHandler,
    private val initialUiType: UiType?,
    private val intentData: IntentData,
    private val workContext: CoroutineContext
) : Fragment(R.layout.stripe_challenge_fragment) {

    private lateinit var cresData: ChallengeResponseData

    private val uiTypeCode: String by lazy { cresData.uiType?.code.orEmpty() }

    internal val viewModel: ChallengeActivityViewModel by activityViewModels {
        ChallengeActivityViewModel.Factory(
            challengeActionHandler,
            transactionTimer,
            errorReporter,
            workContext
        )
    }

    private val challengeEntryViewFactory: ChallengeEntryViewFactory by lazy {
        ChallengeEntryViewFactory(requireActivity())
    }

    private var _viewBinding: StripeChallengeFragmentBinding? = null
    internal val viewBinding get() = requireNotNull(_viewBinding)

    private val challengeZoneView: ChallengeZoneView by lazy { viewBinding.caChallengeZone }
    private val brandZoneView: BrandZoneView by lazy { viewBinding.caBrandZone }

    internal val challengeZoneTextView: ChallengeZoneTextView? by lazy {
        if (cresData.uiType == UiType.Text) {
            challengeEntryViewFactory.createChallengeEntryTextView(
                cresData,
                uiCustomization
            )
        } else {
            null
        }
    }
    internal val challengeZoneSelectView: ChallengeZoneSelectView? by lazy {
        if (
            cresData.uiType == UiType.SingleSelect ||
            cresData.uiType == UiType.MultiSelect
        ) {
            challengeEntryViewFactory
                .createChallengeEntrySelectView(
                    cresData,
                    uiCustomization
                )
        } else {
            null
        }
    }
    internal val challengeZoneWebView: ChallengeZoneWebView? by lazy {
        if (cresData.uiType == UiType.Html) {
            challengeEntryViewFactory.createChallengeEntryWebView(cresData)
        } else {
            null
        }
    }

    internal val userEntry: String
        @VisibleForTesting
        get() {
            return when (cresData.uiType) {
                UiType.Text -> challengeZoneTextView?.userEntry
                UiType.SingleSelect,
                UiType.MultiSelect -> {
                    challengeZoneSelectView?.userEntry
                }
                UiType.Html -> challengeZoneWebView?.userEntry
                else -> ""
            }.orEmpty()
        }

    private val challengeAction: ChallengeAction
        get() {
            return when (cresData.uiType) {
                UiType.OutOfBand -> ChallengeAction.Oob
                UiType.Html -> ChallengeAction.HtmlForm(userEntry)
                else -> ChallengeAction.NativeForm(userEntry)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nullableCres = arguments?.let {
            BundleCompat.getParcelable(it, ARG_CRES, ChallengeResponseData::class.java)
        }
        if (nullableCres == null) {
            onError(
                IllegalArgumentException("Could not start challenge screen. Challenge response data was null.")
            )
            return
        }
        cresData = nullableCres

        _viewBinding = StripeChallengeFragmentBinding.bind(view)

        viewModel.challengeText.observe(viewLifecycleOwner) { challengeText ->
            challengeZoneTextView?.setText(challengeText)
        }

        viewModel.refreshUi.observe(viewLifecycleOwner) {
            refreshUi()
        }

        viewModel.challengeRequestResult.observe(viewLifecycleOwner) { challengeRequestResult ->
            if (challengeRequestResult != null) {
                onChallengeRequestResult(challengeRequestResult)
            }
        }

        updateBrandZoneImages()
        configure(
            challengeZoneTextView,
            challengeZoneSelectView,
            challengeZoneWebView
        )
        configureInformationZoneView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    private fun configure(
        challengeZoneTextView: ChallengeZoneTextView?,
        challengeZoneSelectView: ChallengeZoneSelectView?,
        challengeZoneWebView: ChallengeZoneWebView?
    ) {
        when {
            challengeZoneTextView != null -> {
                challengeZoneView.setChallengeEntryView(challengeZoneTextView)
                challengeZoneView.setSubmitButton(
                    cresData.submitAuthenticationLabel,
                    uiCustomization.getButtonCustomization(UiCustomization.ButtonType.SUBMIT)
                )
                challengeZoneView.setResendButtonLabel(
                    cresData.resendInformationLabel,
                    uiCustomization.getButtonCustomization(UiCustomization.ButtonType.RESEND)
                )
            }
            challengeZoneSelectView != null -> {
                challengeZoneView.setChallengeEntryView(challengeZoneSelectView)
                challengeZoneView.setSubmitButton(
                    cresData.submitAuthenticationLabel,
                    uiCustomization.getButtonCustomization(UiCustomization.ButtonType.NEXT)
                )
                challengeZoneView.setResendButtonLabel(
                    cresData.resendInformationLabel,
                    uiCustomization.getButtonCustomization(UiCustomization.ButtonType.RESEND)
                )
            }
            challengeZoneWebView != null -> {
                challengeZoneView.setChallengeEntryView(challengeZoneWebView)
                challengeZoneView.setInfoHeaderText(null, null)
                challengeZoneView.setInfoText(null, null)
                challengeZoneView.setSubmitButton(null, null)
                challengeZoneWebView.setOnClickListener {
                    viewModel.onSubmitClicked(challengeAction)
                }
                brandZoneView.isGone = true
            }
            cresData.uiType == UiType.OutOfBand -> {
                challengeZoneView.setSubmitButton(
                    cresData.oobContinueLabel,
                    uiCustomization.getButtonCustomization(UiCustomization.ButtonType.CONTINUE)
                )
            }
        }

        configureChallengeZoneView()
    }

    private fun updateBrandZoneImages() {
        val brandZoneView = viewBinding.caBrandZone
        mapOf(
            brandZoneView.issuerImageView to cresData.issuerImage,
            brandZoneView.paymentSystemImageView to cresData.paymentSystemImage
        ).forEach { (imageView, imageData) ->
            viewModel.getImage(
                imageData,
                resources.displayMetrics.densityDpi
            ).observe(viewLifecycleOwner) { bitmap ->
                when {
                    bitmap != null -> {
                        imageView.isVisible = true
                        imageView.setImageBitmap(bitmap)
                    }
                    else -> imageView.isGone = true
                }
            }
        }
    }

    private fun configureInformationZoneView() {
        val informationZoneView = viewBinding.caInformationZone
        informationZoneView.setWhyInfo(
            cresData.whyInfoLabel,
            cresData.whyInfoText,
            uiCustomization.labelCustomization
        )
        informationZoneView.setExpandInfo(
            cresData.expandInfoLabel,
            cresData.expandInfoText,
            uiCustomization.labelCustomization
        )
        uiCustomization.accentColor?.let { accentColor ->
            informationZoneView.toggleColor = Color.parseColor(accentColor)
        }
    }

    private fun configureChallengeZoneView() {
        challengeZoneView.setInfoHeaderText(
            cresData.challengeInfoHeader,
            uiCustomization.labelCustomization
        )
        challengeZoneView.setInfoText(
            cresData.challengeInfoText,
            uiCustomization.labelCustomization
        )
        challengeZoneView.setInfoTextIndicator(
            if (cresData.shouldShowChallengeInfoTextIndicator) {
                R.drawable.stripe_3ds2_ic_indicator
            } else {
                0
            }
        )

        challengeZoneView.setWhitelistingLabel(
            cresData.whitelistingInfoText,
            uiCustomization.labelCustomization,
            uiCustomization.getButtonCustomization(UiCustomization.ButtonType.SELECT)
        )

        challengeZoneView.setSubmitButtonClickListener {
            viewModel.onSubmitClicked(challengeAction)
        }
        challengeZoneView.setResendButtonClickListener {
            viewModel.submit(ChallengeAction.Resend)
        }
    }

    fun refreshUi() {
        if (cresData.uiType == UiType.Html &&
            !cresData.acsHtmlRefresh.isNullOrBlank()
        ) {
            challengeZoneWebView?.loadHtml(cresData.acsHtmlRefresh)
        } else if (cresData.uiType == UiType.OutOfBand &&
            !cresData.challengeAdditionalInfoText.isNullOrBlank()
        ) {
            challengeZoneView.setInfoText(
                cresData.challengeAdditionalInfoText,
                uiCustomization.labelCustomization
            )
            challengeZoneView.setInfoTextIndicator(0)
        }
    }

    private fun onChallengeRequestResult(
        result: ChallengeRequestResult
    ) {
        when (result) {
            is ChallengeRequestResult.Success -> onSuccess(
                result.creqData,
                result.cresData
            )
            is ChallengeRequestResult.ProtocolError -> onError(result.data)
            is ChallengeRequestResult.RuntimeError -> onError(result.throwable)
            is ChallengeRequestResult.Timeout -> onTimeout(result.data)
        }
    }

    private fun onSuccess(
        creqData: ChallengeRequestData,
        cresData: ChallengeResponseData
    ) {
        if (cresData.isChallengeCompleted) {
            viewModel.stopTimer()

            val challengeResult = if (creqData.cancelReason != null) {
                ChallengeResult.Canceled(
                    uiTypeCode,
                    initialUiType,
                    intentData
                )
            } else {
                val transStatus = cresData.transStatus.orEmpty()

                if ("Y" == transStatus) {
                    ChallengeResult.Succeeded(
                        uiTypeCode,
                        initialUiType,
                        intentData
                    )
                } else {
                    ChallengeResult.Failed(
                        uiTypeCode,
                        initialUiType,
                        intentData
                    )
                }
            }

            viewModel.onFinish(challengeResult)
        } else {
            viewModel.onNextScreen(cresData)
        }
    }

    private fun onError(data: ErrorData) {
        viewModel.onFinish(
            ChallengeResult.ProtocolError(
                data,
                initialUiType,
                intentData
            )
        )
        viewModel.stopTimer()
        errorRequestExecutor.executeAsync(data)
    }

    private fun onTimeout(data: ErrorData) {
        viewModel.stopTimer()
        errorRequestExecutor.executeAsync(data)

        viewModel.onFinish(
            ChallengeResult.Timeout(
                uiTypeCode,
                initialUiType,
                intentData
            )
        )
    }

    private fun onError(throwable: Throwable) {
        viewModel.onFinish(
            ChallengeResult.RuntimeError(
                throwable,
                initialUiType,
                intentData
            )
        )
    }

    fun clickSubmitButton() {
        viewModel.submit(challengeAction)
    }

    internal companion object {
        const val ARG_CRES = "arg_cres"
    }
}
