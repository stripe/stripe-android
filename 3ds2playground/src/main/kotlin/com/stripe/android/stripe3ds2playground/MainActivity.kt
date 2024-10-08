package com.stripe.android.stripe3ds2playground

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.stripe.android.stripe3ds2.init.ui.StripeButtonCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeLabelCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeTextBoxCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeToolbarCustomization
import com.stripe.android.stripe3ds2.init.ui.StripeUiCustomization
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.security.MessageTransformer
import com.stripe.android.stripe3ds2.security.MessageTransformerFactory
import com.stripe.android.stripe3ds2.security.StripeEphemeralKeyPairGenerator
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.ChallengeContract
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestExecutor
import com.stripe.android.stripe3ds2.transaction.ChallengeRequestResult
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import com.stripe.android.stripe3ds2.transaction.TransactionStatus
import com.stripe.android.stripe3ds2.transactions.ChallengeRequestData
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.UiType
import com.stripe.android.stripe3ds2.views.ChallengeSubmitDialogFactory
import com.stripe.android.stripe3ds2.views.ChallengeViewArgs
import com.stripe.android.stripe3ds2playground.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import java.io.Serializable
import java.util.Scanner
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val viewBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val snackbarController: SnackbarController by lazy {
        SnackbarController(viewBinding.coordinator)
    }

    private val analyticsReporter = FakeErrorReporter()

    private val intentData = IntentData(
        clientSecret = "pi_123_secret_456",
        sourceId = "src_12345",
        publishableKey = "pk_123"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        init3ds2()

        val challengeLauncher = registerForActivityResult(
            ChallengeContract()
        ) { challengeResult ->
            when (challengeResult) {
                is ChallengeResult.Canceled -> "Challenge cancelled"
                is ChallengeResult.Failed -> "Challenge failed"
                is ChallengeResult.ProtocolError -> "Protocol error during challenge"
                is ChallengeResult.RuntimeError -> "Runtime error during challenge"
                is ChallengeResult.Succeeded -> "Challenge completed"
                is ChallengeResult.Timeout -> "Transaction timed-out"
            }.let {
                snackbarController.show(it)
            }
        }

        val creqData = ChallengeRequestData(
            acsTransId = ACS_TRANS_ID,
            threeDsServerTransId = SERVER_TRANS_ID,
            sdkTransId = SDK_TRANS_ID,
            messageVersion = "2.1.0",
            threeDSRequestorAppURL = "threedsrequestorappurl"
        )

        val labelCustomization = StripeLabelCustomization()
        labelCustomization.setHeadingTextColor("#000000")
        labelCustomization.setTextColor("#000000")
        labelCustomization.textFontSize = 16
        labelCustomization.headingTextFontSize = 20

        val textBoxCustomization = StripeTextBoxCustomization()
        textBoxCustomization.setBorderColor("#89000000")
        textBoxCustomization.borderWidth = 5
        textBoxCustomization.setHintTextColor("#89000000")

        val toolbarCustomization = StripeToolbarCustomization()
        toolbarCustomization.setButtonText("CANCEL")
        toolbarCustomization.setHeaderText("Secure checkout")
        toolbarCustomization.setBackgroundColor("#EC4847")
        toolbarCustomization.setTextColor("#FFFFFF")

        val cancelButtonCustomization = StripeButtonCustomization()
        cancelButtonCustomization.setTextColor("#FFFFFF")
        cancelButtonCustomization.setBackgroundColor("#EC4847")

        val submitButtonCustomization = StripeButtonCustomization()
        submitButtonCustomization.setBackgroundColor("#1CC4B4")
        submitButtonCustomization.setTextColor("#FFFFFF")
        submitButtonCustomization.cornerRadius = 18 * 4

        val resendButtonCustomization = StripeButtonCustomization()
        resendButtonCustomization.setTextColor("#1CC4B4")

        val selectButtonCustomization = StripeButtonCustomization()
        selectButtonCustomization.setBackgroundColor("#EC4847")
        selectButtonCustomization.setTextColor("#000000")

        val uiCustomization = StripeUiCustomization()
        uiCustomization.setAccentColor("#1CC4B4")
        uiCustomization.setToolbarCustomization(toolbarCustomization)
        uiCustomization.setLabelCustomization(labelCustomization)
        uiCustomization.setTextBoxCustomization(textBoxCustomization)
        uiCustomization.setButtonCustomization(
            cancelButtonCustomization,
            UiCustomization.ButtonType.CANCEL
        )
        uiCustomization.setButtonCustomization(
            submitButtonCustomization,
            UiCustomization.ButtonType.NEXT
        )
        uiCustomization.setButtonCustomization(
            resendButtonCustomization,
            UiCustomization.ButtonType.RESEND
        )
        uiCustomization.setButtonCustomization(
            submitButtonCustomization,
            UiCustomization.ButtonType.SUBMIT
        )
        uiCustomization.setButtonCustomization(
            submitButtonCustomization,
            UiCustomization.ButtonType.CONTINUE
        )
        uiCustomization.setButtonCustomization(
            selectButtonCustomization,
            UiCustomization.ButtonType.SELECT
        )

        val stripeUiCustomization = StripeUiCustomization()

        val messageTransformer = MessageTransformerFactory(false).create()
        val creqExecutorFactory = ChallengeRequestExecutor.Factory { _, _ ->
            FakeChallengeRequestExecutor(messageTransformer)
        }

        val keyPair = StripeEphemeralKeyPairGenerator(analyticsReporter).generate()
        val creqExecutorConfig = ChallengeRequestExecutor.Config(
            messageTransformer = messageTransformer,
            sdkReferenceId = UUID.randomUUID().toString(),
            acsUrl = "https://bank.com",
            creqData = creqData,
            keys = ChallengeRequestExecutor.Config.Keys(
                keyPair.private.encoded,
                keyPair.public.encoded
            )
        )

        val defaultUiCustomization =
            StripeUiCustomization.createWithAppTheme(this)

        val textChallengeOnClickListener = { v: View ->
            val customization: StripeUiCustomization = when (v.id) {
                R.id.launch_custom_challenge -> uiCustomization
                R.id.launch_default_custom_challenge -> defaultUiCustomization
                else -> stripeUiCustomization
            }

            challengeLauncher.launch(
                ChallengeViewArgs(
                    CRES_TEXT,
                    creqData,
                    customization,
                    creqExecutorConfig,
                    creqExecutorFactory,
                    TIMEOUT_MINS,
                    intentData
                )
            )
        }

        viewBinding.launchChallenge.setOnClickListener(textChallengeOnClickListener)
        viewBinding.launchCustomChallenge.setOnClickListener(textChallengeOnClickListener)
        viewBinding.launchDefaultCustomChallenge.setOnClickListener(textChallengeOnClickListener)

        val singleSelectClickListener = { v: View ->
            val options = listOf(
                ChallengeResponseData.ChallengeSelectOption(
                    "phone",
                    "Mobile **** **** 321"
                ),
                ChallengeResponseData.ChallengeSelectOption(
                    "email",
                    "Email a*******g**@g***.com"
                )
            )

            val cresData = ChallengeResponseData(
                acsTransId = ACS_TRANS_ID,
                sdkTransId = SDK_TRANS_ID,
                serverTransId = SERVER_TRANS_ID,
                messageVersion = MESSAGE_VERSION,
                uiType = UiType.SingleSelect,
                challengeSelectOptions = options,
                issuerImage = ISSUER_IMAGE,
                paymentSystemImage = PAYMENT_SYSTEM_IMAGE,
                challengeInfoHeader = "Purchase Authentication",
                challengeInfoText = CHALLENGE_INFO_TEXT,
                submitAuthenticationLabel = "Next",
                whyInfoLabel = "Learn more about authentication",
                whyInfoText = "Here is more information about authentication",
                expandInfoLabel = EXPAND_INFO_LABEL,
                expandInfoText = EXPAND_INFO_TEXT
            )

            challengeLauncher.launch(
                ChallengeViewArgs(
                    cresData,
                    creqData,
                    if (v.id == R.id.launch_single_select_challenge) {
                        stripeUiCustomization
                    } else {
                        uiCustomization
                    },
                    creqExecutorConfig,
                    creqExecutorFactory,
                    TIMEOUT_MINS,
                    intentData
                )
            )
        }

        viewBinding.launchSingleSelectChallenge
            .setOnClickListener(singleSelectClickListener)
        viewBinding.launchCustomSingleSelectChallenge
            .setOnClickListener(singleSelectClickListener)

        val multiSelectClickListener = { v: View ->
            val options = listOf(
                ChallengeResponseData.ChallengeSelectOption(
                    "phone",
                    "Mobile **** **** 321"
                ),
                ChallengeResponseData.ChallengeSelectOption(
                    "email",
                    "Email a*******g**@g***.com"
                ),
                ChallengeResponseData.ChallengeSelectOption(
                    "email2",
                    "Email b*******b**@h******.com"
                )
            )

            val cresData = ChallengeResponseData(
                acsTransId = ACS_TRANS_ID,
                sdkTransId = SDK_TRANS_ID,
                serverTransId = SERVER_TRANS_ID,
                messageVersion = MESSAGE_VERSION,
                uiType = UiType.MultiSelect,
                challengeSelectOptions = options,
                issuerImage = ISSUER_IMAGE,
                resendInformationLabel = "resend code",
                paymentSystemImage = PAYMENT_SYSTEM_IMAGE,
                challengeInfoHeader = "Purchase Authentication",
                challengeInfoText = CHALLENGE_INFO_TEXT,
                submitAuthenticationLabel = "NEXT",
                whitelistingInfoText = "Would you like to add Merchant ABC to your whitelist?",
                whyInfoLabel = "Learn more about authentication",
                whyInfoText = "Here is more information about authentication",
                expandInfoLabel = EXPAND_INFO_LABEL,
                expandInfoText = EXPAND_INFO_TEXT
            )

            challengeLauncher.launch(
                ChallengeViewArgs(
                    cresData,
                    creqData,
                    if (v.id == R.id.launch_multi_select_challenge) {
                        stripeUiCustomization
                    } else {
                        uiCustomization
                    },
                    creqExecutorConfig,
                    creqExecutorFactory,
                    TIMEOUT_MINS,
                    intentData
                )
            )
        }

        viewBinding.launchMultiSelectChallenge.setOnClickListener(multiSelectClickListener)
        viewBinding.launchCustomMultiSelectChallenge.setOnClickListener(multiSelectClickListener)

        val oobClickListener = { v: View ->
            val cresData = ChallengeResponseData(
                acsTransId = ACS_TRANS_ID,
                sdkTransId = SDK_TRANS_ID,
                serverTransId = SERVER_TRANS_ID,
                messageVersion = MESSAGE_VERSION,
                uiType = UiType.OutOfBand,
                issuerImage = ISSUER_IMAGE,
                paymentSystemImage = PAYMENT_SYSTEM_IMAGE,
                challengeInfoHeader = "Payment Security",
                challengeInfoText = OOB_CHALLENGE_INFO_TEXT,
                challengeAdditionalInfoText = "Tap continue to proceed.",
                oobAppUrl = "https://stripe.com",
                oobAppLabel = "Click here to open Your Bank App",
                oobContinueLabel = "Continue",
                whyInfoLabel = "Learn more about authentication",
                whyInfoText = "Here is more information about authentication",
                expandInfoLabel = EXPAND_INFO_LABEL,
                expandInfoText = EXPAND_INFO_TEXT
            )

            challengeLauncher.launch(
                ChallengeViewArgs(
                    cresData,
                    creqData,
                    if (v.id == R.id.launch_oob_challenge) {
                        stripeUiCustomization
                    } else {
                        uiCustomization
                    },
                    creqExecutorConfig,
                    creqExecutorFactory,
                    TIMEOUT_MINS,
                    intentData
                )
            )
        }

        viewBinding.launchOobChallenge.setOnClickListener(oobClickListener)
        viewBinding.launchCustomOobChallenge.setOnClickListener(oobClickListener)

        viewBinding.launchHtmlChallenge.setOnClickListener {
            val cresData = ChallengeResponseData(
                acsTransId = ACS_TRANS_ID,
                sdkTransId = SDK_TRANS_ID,
                serverTransId = SERVER_TRANS_ID,
                messageVersion = MESSAGE_VERSION,
                uiType = UiType.Html,
                acsHtml = loadHtmlFromAssetsFile("otp.html"),
                acsHtmlRefresh = loadHtmlFromAssetsFile("oob-refresh.html")
            )

            challengeLauncher.launch(
                ChallengeViewArgs(
                    cresData,
                    creqData,
                    stripeUiCustomization,
                    creqExecutorConfig,
                    creqExecutorFactory,
                    TIMEOUT_MINS,
                    intentData
                )
            )
        }

        viewBinding.showAreqProgressVisa.setOnClickListener {
            showAreqProgressActivity("visa")
        }
        viewBinding.showAreqProgressMastercard.setOnClickListener {
            showAreqProgressActivity("mastercard")
        }
        viewBinding.showAreqProgressAmex.setOnClickListener {
            showAreqProgressActivity("american_express")
        }
        viewBinding.showAreqProgressDiscover.setOnClickListener {
            showAreqProgressActivity("discover")
        }
        viewBinding.showAreqProgressCartesBancaires.setOnClickListener {
            showAreqProgressActivity("cartes_bancaires")
        }
        viewBinding.showAreqProgressUnionpay.setOnClickListener {
            showAreqProgressActivity("unionpay")
        }
        viewBinding.showAreqProgressUnknown.setOnClickListener {
            showAreqProgressActivity("unknown")
        }

        viewBinding.showChallengeProgressView
            .setOnClickListener {
                val context =
                    ContextThemeWrapper(this@MainActivity, com.stripe.android.stripe3ds2.R.style.Stripe3DS2Theme)
                val progressDialog =
                    ChallengeSubmitDialogFactory(context, defaultUiCustomization).create()
                progressDialog.setCancelable(true)
                progressDialog.show()
            }
    }

    private fun loadHtmlFromAssetsFile(fileName: String): String? {
        return runCatching {
            assets.open(fileName).use {
                Scanner(it).useDelimiter("\\A").next()
            }
        }.getOrNull()
    }

    // initialize common 3ds2 objects here to verify/debug
    private fun init3ds2() {
        StripeThreeDs2ServiceImpl(
            this,
            enableLogging = true,
            workContext = Dispatchers.IO
        )
    }

    private fun showAreqProgressActivity(
        directoryServerName: String
    ) {
        startActivity(
            Intent(this, ChallengeProgressActivity::class.java)
                .putExtras(
                    ChallengeProgressActivity.Args(
                        directoryServerName,
                        SdkTransactionId.create(),
                        ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                    ).toBundle()
                )
        )
    }

    private class FakeChallengeRequestExecutor(
        private val messageTransformer: MessageTransformer
    ) : ChallengeRequestExecutor, Serializable {
        override suspend fun execute(
            creqData: ChallengeRequestData
        ): ChallengeRequestResult {
            val acsTransId = UUID.randomUUID().toString()
            val threeDsServerTransId = UUID.randomUUID().toString()

            val cres = when {
                creqData.cancelReason != null -> {
                    ChallengeResponseData(
                        acsTransId = ACS_TRANS_ID,
                        sdkTransId = SDK_TRANS_ID,
                        serverTransId = threeDsServerTransId,
                        messageVersion = MESSAGE_VERSION,
                        isChallengeCompleted = true,
                        transStatus = TransactionStatus.VerificationSuccessful.code
                    )
                }
                creqData.challengeDataEntry.isNullOrBlank() -> {
                    // return a text input challenge if entry was blank
                    CRES_TEXT.copy(
                        shouldShowChallengeInfoTextIndicator = true
                    )
                }
                else -> {
                    // return a successful completion if entry was non-blank
                    ChallengeResponseData(
                        acsTransId = ACS_TRANS_ID,
                        sdkTransId = SDK_TRANS_ID,
                        serverTransId = threeDsServerTransId,
                        messageVersion = MESSAGE_VERSION,
                        isChallengeCompleted = true,
                        transStatus = TransactionStatus.VerificationSuccessful.code
                    )
                }
            }

            return ChallengeRequestResult.Success(
                ChallengeRequestData(
                    acsTransId = acsTransId,
                    threeDsServerTransId = threeDsServerTransId,
                    sdkTransId = SDK_TRANS_ID,
                    messageVersion = "2.1.0",
                    threeDSRequestorAppURL = "threeDSRequestorAppURL"

                ),
                cres,
                ChallengeRequestExecutor.Config(
                    messageTransformer,
                    UUID.randomUUID().toString(),
                    creqData,
                    "https://example.com",
                    ChallengeRequestExecutor.Config.Keys(
                        byteArrayOf(),
                        byteArrayOf()
                    )
                )
            )
        }
    }

    private class FakeErrorReporter : ErrorReporter {
        override fun reportError(t: Throwable) {
        }
    }

    companion object {
        private val SDK_TRANS_ID = SdkTransactionId.create()
        private val SERVER_TRANS_ID = UUID.randomUUID().toString()
        private val ACS_TRANS_ID = UUID.randomUUID().toString()
        private const val MESSAGE_VERSION = "2.1.0"

        private val CHALLENGE_INFO_TEXT =
            """
            We have sent you a text message with a code to your registered mobile number ending in 5309.

            You are paying Merchant ABC the amount of $500.00 on 3/4/2019.
            """.trimIndent()

        private val OOB_CHALLENGE_INFO_TEXT =
            """
            For added security, you will be authenticated with YourBank application.

            Step 1 - Open your YourBank application "directly from your phone and verify this payment.

            Step 2 - Tap continue after you have completed authentication with your YourBank application.
            """.trimIndent()

        private const val EXPAND_INFO_LABEL = "Need some help?"

        private val EXPAND_INFO_TEXT =
            """
            Contact Visa for help:

            Phone number: 867-5309
            """.trimIndent()

        private val ISSUER_IMAGE = ChallengeResponseData.Image(
            "https://3ds.selftestplatform.com/images/BankLogo_medium.png",
            "https://3ds.selftestplatform.com/images/BankLogo_high.png",
            "https://3ds.selftestplatform.com/images/BankLogo_extraHigh.png"
        )

        private val PAYMENT_SYSTEM_IMAGE = ChallengeResponseData.Image(
            "https://3ds.selftestplatform.com/images/ULCardScheme_medium.png",
            "https://3ds.selftestplatform.com/images/ULCardScheme_high.png",
            "https://3ds.selftestplatform.com/images/ULCardScheme_extraHigh.png"
        )

        private val CRES_TEXT = ChallengeResponseData(
            acsTransId = ACS_TRANS_ID,
            sdkTransId = SDK_TRANS_ID,
            serverTransId = SERVER_TRANS_ID,
            messageVersion = MESSAGE_VERSION,
            uiType = UiType.Text,
            issuerImage = ISSUER_IMAGE,
            paymentSystemImage = PAYMENT_SYSTEM_IMAGE,
            challengeInfoHeader = "Purchase Authentication",
            challengeInfoText = CHALLENGE_INFO_TEXT,
            challengeInfoLabel = "Enter your code:",
            submitAuthenticationLabel = "Submit",
            resendInformationLabel = "resend code",
            whitelistingInfoText = "Would you like to add Merchant ABC to your whitelist?",
            whyInfoLabel = "Learn more about authentication",
            whyInfoText = "Here is more information about authentication",
            expandInfoLabel = EXPAND_INFO_LABEL,
            expandInfoText = EXPAND_INFO_TEXT
        )

        private const val TIMEOUT_MINS = 1
    }
}
