package com.stripe.android.paymentelement.confirmation.lpms.foundations

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.injection.DefaultConfirmationModule
import com.stripe.android.paymentelement.confirmation.lpms.foundations.network.StripeNetworkTestClient
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.FakeLogger
import com.stripe.android.view.ActivityStarter
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal class LpmNetworkTestActivity : AppCompatActivity() {
    val viewModel: TestViewModel by viewModels {
        TestViewModel.Factory {
            requireNotNull(Args.fromIntent(intent))
        }
    }

    val confirmationHandler by lazy {
        viewModel.confirmationHandler
    }

    val testClient by lazy {
        viewModel.testClient
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        confirmationHandler.register(activityResultCaller = this, lifecycleOwner = this)
    }

    class TestViewModel @Inject constructor(
        confirmationHandlerFactory: ConfirmationHandler.Factory,
        val testClient: StripeNetworkTestClient,
        @IOContext workContext: CoroutineContext
    ) : ViewModel() {
        val confirmationHandler = confirmationHandlerFactory.create(viewModelScope.plus(workContext))

        class Factory(
            private val starterArgsSupplier: () -> Args,
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val args = starterArgsSupplier()
                val component = DaggerLpmNetworkTestViewModelComponent.builder()
                    .application(extras.requireApplication())
                    .paymentElementCallbackIdentifier(args.paymentElementCallbackIdentifier)
                    .publishableKeyProvider { args.publishableKey }
                    .stripeAccountIdProvider { null }
                    .allowsManualConfirmation(args.allowsManualConfirmation)
                    .savedStateHandle(extras.createSavedStateHandle())
                    .build()

                @Suppress("UNCHECKED_CAST")
                return component.viewModel as T
            }
        }
    }

    @Parcelize
    data class Args(
        val publishableKey: String,
        val paymentElementCallbackIdentifier: String,
        val allowsManualConfirmation: Boolean,
    ) : ActivityStarter.Args {
        companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            args: Args,
        ): Intent {
            return Intent(context, LpmNetworkTestActivity::class.java)
                .putExtra(EXTRA_ARGS, args)
        }

        private const val EXTRA_ARGS = "extra_args"
    }
}

@Component(
    modules = [
        StripeRepositoryModule::class,
        DefaultConfirmationModule::class,
        LpmNetworkTestModule::class,
    ]
)
@Singleton
internal interface LpmNetworkTestViewModelComponent {
    val viewModel: LpmNetworkTestActivity.TestViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun publishableKeyProvider(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        ): Builder

        @BindsInstance
        fun stripeAccountIdProvider(
            @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
        ): Builder

        @BindsInstance
        fun allowsManualConfirmation(
            @Named(ALLOWS_MANUAL_CONFIRMATION) allowsManualConfirmation: Boolean
        ): Builder

        @BindsInstance
        fun paymentElementCallbackIdentifier(
            @PaymentElementCallbackIdentifier identifier: String,
        ): Builder

        @BindsInstance
        fun savedStateHandle(
            savedStateHandle: SavedStateHandle,
        ): Builder

        fun build(): LpmNetworkTestViewModelComponent
    }
}

@Module
internal object LpmNetworkTestModule {
    @Provides
    fun providesContext(application: Application): Context = application

    @Provides
    fun providesErrorReporter(): ErrorReporter = FakeErrorReporter()

    @Provides
    @Named(PRODUCT_USAGE)
    fun providesProductUsage() = setOf<String>()

    @Provides
    fun providesLogger(): Logger = FakeLogger()

    @Provides
    fun providesPaymentConfiguration(
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String,
    ): PaymentConfiguration {
        return PaymentConfiguration(
            publishableKey = publishableKeyProvider(),
            stripeAccountId = stripeAccountIdProvider(),
        )
    }

    @Provides
    @Named(STATUS_BAR_COLOR)
    fun providesStatusBarColor(): Int? = STATUS_BAR_COLOR_VALUE

    @Provides
    @Named(ENABLE_LOGGING)
    fun providesEnableLogging(): Boolean = ENABLE_LOGGING_VALUE

    @Provides
    @Singleton
    @IOContext
    fun provideWorkContext(): CoroutineContext = UnconfinedTestDispatcher()

    private val STATUS_BAR_COLOR_VALUE = null
    private const val ENABLE_LOGGING_VALUE = false
}
