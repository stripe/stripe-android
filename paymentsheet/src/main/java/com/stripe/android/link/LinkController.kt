package com.stripe.android.link

import android.app.Activity
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LinkController @Inject internal constructor(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val viewModel: LinkControllerViewModel,
    private val selectedPaymentMethodCallback: PresentPaymentMethodsCallback,
    private val lookupConsumerCallback: LookupConsumerCallback,
    private val createPaymentMethodCallback: CreatePaymentMethodCallback,
    linkActivityContract: NativeLinkActivityContract,
) {

    val state: StateFlow<State> = viewModel.state(activity)

    private val linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> =
        activityResultRegistryOwner.activityResultRegistry.register(
            key = "LinkController_LinkActivityResultLauncher",
            contract = linkActivityContract,
        ) { result ->
            viewModel.onPresentPaymentMethodsActivityResult(result)
        }

    init {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.presentPaymentMethodsResultFlow
                        .collect(selectedPaymentMethodCallback::onPresentPaymentMethodsResult)
                }
                launch {
                    viewModel.lookupConsumerResultFlow
                        .collect(lookupConsumerCallback::onLookupConsumerResult)
                }

                launch {
                    viewModel.createPaymentMethodResultFlow
                        .collect(createPaymentMethodCallback::onCreatePaymentMethodResult)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkActivityResultLauncher.unregister()
                }
            }
        )
    }

    fun presentPaymentMethods(email: String?) {
        viewModel.onPresent(linkActivityResultLauncher, email)
    }

    fun createPaymentMethod() {
        viewModel.onCreatePaymentMethod()
    }

    fun lookupConsumer(email: String) {
        viewModel.onLookupConsumer(email)
    }

    // TODO
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setConfiguration(configuration: PaymentSheet.Configuration) {
        viewModel.configuration = configuration
    }

    @Parcelize
    @Poko
    class State @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
        val email: String? = null,
        val selectedPaymentMethodPreview: PaymentMethodPreview? = null,
        val createdPaymentMethod: PaymentMethod? = null,
    ) : Parcelable

    sealed interface PresentPaymentMethodsResult {
        data object Success : PresentPaymentMethodsResult
        class Failed internal constructor(val error: Throwable) : PresentPaymentMethodsResult
    }

    sealed interface LookupConsumerResult {
        class Success internal constructor(val email: String, val isConsumer: Boolean) : LookupConsumerResult
        class Failed internal constructor(val email: String, val error: Throwable) : LookupConsumerResult
    }

    sealed interface CreatePaymentMethodResult {
        data object Success : CreatePaymentMethodResult
        class Failed internal constructor(val error: Throwable) : CreatePaymentMethodResult
    }

    fun interface PresentPaymentMethodsCallback {
        fun onPresentPaymentMethodsResult(state: PresentPaymentMethodsResult)
    }

    fun interface LookupConsumerCallback {
        fun onLookupConsumerResult(result: LookupConsumerResult)
    }

    fun interface CreatePaymentMethodCallback {
        fun onCreatePaymentMethodResult(result: CreatePaymentMethodResult)
    }

    @Parcelize
    @Poko
    class PaymentMethodPreview(
        @DrawableRes val iconRes: Int,
        val label: String,
        val sublabel: String?
    ) : Parcelable

    companion object {
        fun create(
            activity: ComponentActivity,
            presentPaymentMethodCallback: PresentPaymentMethodsCallback,
            lookupConsumerCallback: LookupConsumerCallback,
            createPaymentMethodCallback: CreatePaymentMethodCallback,
        ): LinkController {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkControllerViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkControllerViewModel::class.java]
            return viewModel
                .controllerComponentFactory.build(
                    activity = activity,
                    lifecycleOwner = activity,
                    activityResultRegistryOwner = activity,
                    presentPaymentMethodCallback = presentPaymentMethodCallback,
                    lookupConsumerCallback = lookupConsumerCallback,
                    createPaymentMethodCallback = createPaymentMethodCallback,
                )
                .controller
        }
    }
}
