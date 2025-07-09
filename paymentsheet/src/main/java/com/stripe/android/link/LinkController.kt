package com.stripe.android.link

import android.content.Context
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
import com.stripe.android.link.LinkController.CreatePaymentMethodCallback
import com.stripe.android.link.LinkController.LookupConsumerCallback
import com.stripe.android.link.LinkController.PresentPaymentMethodsCallback
import com.stripe.android.link.ui.wallet.displayName
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.R
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

interface LinkController {

    fun getPaymentMethodPreview(): PaymentMethodPreview?

    fun presentPaymentMethods(email: String?)

    fun lookupConsumer(email: String)

    fun createPaymentMethod()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun setConfiguration(configuration: PaymentSheet.Configuration)

    sealed interface PresentPaymentMethodsResult {
        val preview: PaymentMethodPreview?

        class Updated(override val preview: PaymentMethodPreview?) : PresentPaymentMethodsResult

        class Failed(
            val error: Throwable,
            override val preview: PaymentMethodPreview? = null
        ) : PresentPaymentMethodsResult
    }

    sealed interface LookupConsumerResult {
        class Success(val email: String, val isConsumer: Boolean) : LookupConsumerResult
        class Failed(val email: String, val error: Throwable) : LookupConsumerResult
    }

    sealed interface CreatePaymentMethodResult {
        class Success(val paymentMethod: PaymentMethod) : CreatePaymentMethodResult
        class Failed(val error: Throwable) : CreatePaymentMethodResult
    }

    fun interface PresentPaymentMethodsCallback {
        fun onPresentPaymentMethodsResult(result: PresentPaymentMethodsResult)
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
            presentPaymentMethodsCallback: PresentPaymentMethodsCallback,
            lookupConsumerCallback: LookupConsumerCallback,
            createPaymentMethodCallback: CreatePaymentMethodCallback,
        ): LinkController {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkControllerViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkControllerViewModel::class.java]
            return RealLinkController(
                context = activity,
                lifecycleOwner = activity,
                activityResultRegistryOwner = activity,
                viewModel = viewModel,
                presentPaymentMethodsCallback = presentPaymentMethodsCallback,
                lookupConsumerCallback = lookupConsumerCallback,
                createPaymentMethodCallback = createPaymentMethodCallback,
            )
        }
    }
}

internal class RealLinkController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val viewModel: LinkControllerViewModel,
    private val presentPaymentMethodsCallback: PresentPaymentMethodsCallback,
    private val lookupConsumerCallback: LookupConsumerCallback,
    private val createPaymentMethodCallback: CreatePaymentMethodCallback,
) : LinkController {

    private val linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> =
        activityResultRegistryOwner.activityResultRegistry.register(
            key = "LinkController_LinkActivityResultLauncher",
            contract = viewModel.linkActivityContract,
        ) { result ->
            viewModel.onLinkActivityResult(context, result)
        }

    init {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.presentPaymentMethodsResult }
                        .filterNotNull()
                        .collect(presentPaymentMethodsCallback::onPresentPaymentMethodsResult)
                }

                launch {
                    viewModel.state
                        .map { it.lookupConsumerResult }
                        .filterNotNull()
                        .collect(lookupConsumerCallback::onLookupConsumerResult)
                }

                launch {
                    viewModel.state
                        .map { it.createPaymentMethodResult }
                        .filterNotNull()
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

    override fun getPaymentMethodPreview(): LinkController.PaymentMethodPreview? {
        return viewModel.state.value.paymentMethodPreview
    }

    override fun presentPaymentMethods(email: String?) {
        viewModel.onPresent(linkActivityResultLauncher, email)
    }

    override fun lookupConsumer(email: String) {
        viewModel.onLookupConsumer(email)
    }

    override fun createPaymentMethod() {
        viewModel.onCreatePaymentMethod()
    }

    override fun setConfiguration(configuration: PaymentSheet.Configuration) {
        viewModel.configuration = configuration
    }
}

internal fun LinkPaymentMethod.toPreview(context: Context): LinkController.PaymentMethodPreview {
    val sublabel = buildString {
        append(details.displayName.resolve(context))
        append(" •••• ")
        append(details.last4)
    }
    return LinkController.PaymentMethodPreview(
        iconRes = R.drawable.stripe_ic_paymentsheet_link_arrow,
        label = context.getString(com.stripe.android.R.string.stripe_link),
        sublabel = sublabel
    )
}
