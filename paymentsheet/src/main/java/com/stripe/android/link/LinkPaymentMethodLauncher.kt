package com.stripe.android.link

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stripe.android.link.ui.wallet.displayName
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

interface LinkPaymentMethodLauncher {
    var listener: Listener?

    fun present(email: String?)

    interface Listener {
        fun onPaymentMethodSelected(preview: PaymentMethodPreview)
    }

    @Parcelize
    @Poko
    class PaymentMethodPreview internal constructor(
//        val icon: Drawable, // TODO
        val label: String,
        val sublabel: String?
    ) : Parcelable

    companion object {
        fun create(activity: ComponentActivity): LinkPaymentMethodLauncher {
            val viewModelProvider = ViewModelProvider(
                owner = activity,
                factory = LinkPaymentMethodLauncherViewModel.Factory()
            )
            val viewModel = viewModelProvider[LinkPaymentMethodLauncherViewModel::class.java]
            return RealLinkPaymentMethodLauncher(activity, viewModel)
        }
    }
}

internal class RealLinkPaymentMethodLauncher(
    private val activity: ComponentActivity,
    private val viewModel: LinkPaymentMethodLauncherViewModel
) : LinkPaymentMethodLauncher {

    override var listener: LinkPaymentMethodLauncher.Listener? = null

    private var linkActivityResultLauncher: ActivityResultLauncher<LinkActivityContract.Args> =
        activity.registerForActivityResult(viewModel.linkActivityContract) { result ->
            viewModel.onResult(result)
        }

    init {
        activity.lifecycleScope.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state
                        .map { it.selectedPaymentMethod }
                        .filterNotNull()
                        .map { pm ->
                            val sublabel = buildString {
                                append(pm.details.displayName.resolve(activity))
                                append(" •••• ")
                                append(pm.details.last4)
                            }
                            LinkPaymentMethodLauncher.PaymentMethodPreview(
                                label = "Link", // TODO
                                sublabel = sublabel
                            )
                        }
                        .distinctUntilChanged()
                        .collect { listener?.onPaymentMethodSelected(it) }
                }
            }
        }
    }

    override fun present(email: String?) {
        viewModel.onPresent(linkActivityResultLauncher, email)
    }
}
