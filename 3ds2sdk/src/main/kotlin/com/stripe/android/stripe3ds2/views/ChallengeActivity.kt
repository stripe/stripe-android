package com.stripe.android.stripe3ds2.views

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import com.stripe.android.stripe3ds2.databinding.StripeChallengeActivityBinding
import com.stripe.android.stripe3ds2.databinding.StripeChallengeFragmentBinding
import com.stripe.android.stripe3ds2.init.ui.UiCustomization
import com.stripe.android.stripe3ds2.observability.DefaultErrorReporter
import com.stripe.android.stripe3ds2.observability.ErrorReporter
import com.stripe.android.stripe3ds2.observability.Stripe3ds2ErrorReporterConfig
import com.stripe.android.stripe3ds2.transaction.ChallengeAction
import com.stripe.android.stripe3ds2.transaction.ChallengeActionHandler
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.DefaultTransactionTimer
import com.stripe.android.stripe3ds2.transaction.ErrorRequestExecutor
import com.stripe.android.stripe3ds2.transaction.StripeErrorRequestExecutor
import com.stripe.android.stripe3ds2.transaction.TransactionTimer
import com.stripe.android.stripe3ds2.transactions.ChallengeResponseData
import com.stripe.android.stripe3ds2.transactions.UiType
import kotlinx.coroutines.Dispatchers

class ChallengeActivity : AppCompatActivity() {

    private val transactionTimer: TransactionTimer by lazy {
        DefaultTransactionTimer(
            viewArgs.timeoutMins,
            errorRequestExecutor,
            viewArgs.creqData,
        )
    }

    private val errorReporter: ErrorReporter by lazy {
        DefaultErrorReporter(
            applicationContext,
            Stripe3ds2ErrorReporterConfig(viewArgs.sdkTransactionId)
        )
    }

    internal val fragment: ChallengeFragment by lazy {
        viewBinding.fragmentContainer.getFragment()
    }
    internal val fragmentViewBinding: StripeChallengeFragmentBinding by lazy {
        fragment.viewBinding
    }

    internal val viewBinding: StripeChallengeActivityBinding by lazy {
        StripeChallengeActivityBinding.inflate(layoutInflater)
    }

    private val challengeActionHandler: ChallengeActionHandler by lazy {
        ChallengeActionHandler.Default(
            viewArgs.creqData,
            errorReporter,
            viewArgs.creqExecutorFactory,
            WORK_CONTEXT
        )
    }

    private val errorRequestExecutor: ErrorRequestExecutor by lazy {
        StripeErrorRequestExecutor.Factory(WORK_CONTEXT)
            .create(
                viewArgs.creqExecutorConfig.acsUrl,
                errorReporter
            )
    }

    internal val viewModel: ChallengeActivityViewModel by viewModels {
        ChallengeActivityViewModel.Factory(
            challengeActionHandler,
            transactionTimer,
            errorReporter,
            WORK_CONTEXT
        )
    }

    private val viewArgs: ChallengeViewArgs by lazy {
        ChallengeViewArgs.create(intent.extras ?: Bundle.EMPTY)
    }

    private val keyboardController: KeyboardController by lazy {
        KeyboardController(this)
    }

    private val progressDialogFactory: ChallengeSubmitDialogFactory by lazy {
        ChallengeSubmitDialogFactory(this, viewArgs.uiCustomization)
    }

    private var progressDialog: Dialog? = null
    private var currentUITypeCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = ChallengeFragmentFactory(
            uiCustomization = viewArgs.uiCustomization,
            transactionTimer = transactionTimer,
            errorRequestExecutor = errorRequestExecutor,
            errorReporter = errorReporter,
            challengeActionHandler = challengeActionHandler,
            intentData = viewArgs.intentData,
            initialUiType = viewArgs.cresData.uiType,
            workContext = WORK_CONTEXT
        )

        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // do not call super.onBackPressed() - handle tap on system back button as
                    // a tap on the cancel button
                    viewModel.submit(
                        ChallengeAction.Cancel,
                    )
                }
            }
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(viewBinding.root)

        viewModel.submitClicked.observe(this) { challengeAction ->
            if (!isFinishing) {
                dismissKeyboard()

                progressDialog = progressDialogFactory.create().also {
                    it.show()
                }

                viewModel.submit(challengeAction)
            }
        }

        viewModel.shouldFinish.observe(this) { challengeResult ->
            setResult(
                RESULT_OK,
                Intent().putExtras(challengeResult.toBundle())
            )
            if (!isFinishing) {
                finish()
            }
        }

        configureHeaderZone()

        viewModel.nextScreen.observe(this) { cres ->
            dismissDialog()

            if (cres != null) {
                startFragment(cres)

                currentUITypeCode = cres.uiType?.code.orEmpty()
            }
        }

        if (savedInstanceState == null) {
            viewModel.onNextScreen(viewArgs.cresData)
        }

        viewModel.getTimeout().observe(this) { isTimeout ->
            if (isTimeout == true) {
                viewModel.onFinish(
                    ChallengeResult.Timeout(
                        currentUITypeCode,
                        viewArgs.cresData.uiType,
                        viewArgs.intentData
                    )
                )
            }
        }
    }

    private fun startFragment(
        cres: ChallengeResponseData
    ) {
        supportFragmentManager.commit {
            setCustomAnimations(
                AnimationConstants.SLIDE_IN,
                AnimationConstants.SLIDE_OUT,
                AnimationConstants.SLIDE_IN,
                AnimationConstants.SLIDE_OUT
            )

            replace(
                viewBinding.fragmentContainer.id,
                ChallengeFragment::class.java,
                bundleOf(ChallengeFragment.ARG_CRES to cres)
            )
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewModel.onMemoryEvent()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        viewModel.onMemoryEvent()
    }

    private fun dismissKeyboard() {
        keyboardController.hide()
    }

    override fun onPause() {
        super.onPause()
        viewModel.shouldRefreshUi = true
        viewModel.shouldAutoSubmitOOB = UiType.fromCode(currentUITypeCode) == UiType.OutOfBand
        dismissKeyboard()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.shouldAutoSubmitOOB) {
            val fragment = supportFragmentManager.fragments.first() as ChallengeFragment

            viewModel.submit(ChallengeAction.Oob(fragment.challengeZoneView.whitelistingSelection))
        } else if (viewModel.shouldRefreshUi) {
            viewModel.onRefreshUi()
        }
    }

    private fun configureHeaderZone() {
        val headerZoneCustomizer = HeaderZoneCustomizer(this)
        val cancelButton = headerZoneCustomizer.customize(
            viewArgs.uiCustomization.toolbarCustomization,
            viewArgs.uiCustomization.getButtonCustomization(UiCustomization.ButtonType.CANCEL)
        )
        cancelButton?.setOnClickListener {
            cancelButton.isClickable = false
            viewModel.submit(ChallengeAction.Cancel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissDialog()
    }

    private fun dismissDialog() {
        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        progressDialog = null
    }

    private companion object {
        private val WORK_CONTEXT = Dispatchers.IO
    }
}
