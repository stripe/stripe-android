package com.stripe.android.checkout

import androidx.annotation.VisibleForTesting
import com.stripe.android.paymentelement.CheckoutSessionPreview
import java.lang.ref.WeakReference

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutControllerReferences {
    private val controllers = mutableMapOf<String, WeakReference<CheckoutController>>()

    @Synchronized
    operator fun get(instanceId: String): CheckoutController? {
        val controller = controllers[instanceId]?.get()
        if (controller == null) {
            controllers.remove(instanceId)
        }
        return controller
    }

    @Synchronized
    fun register(instanceId: String, controller: CheckoutController) {
        controllers[instanceId] = WeakReference(controller)
    }

    @VisibleForTesting
    @Synchronized
    fun register(instanceId: String, controller: WeakReference<CheckoutController>) {
        controllers[instanceId] = controller
    }

    @Synchronized
    fun unregister(instanceId: String, controller: CheckoutController) {
        if (controllers[instanceId]?.get() === controller) {
            controllers.remove(instanceId)
        }
    }

    @VisibleForTesting
    @Synchronized
    fun clear() {
        controllers.clear()
    }
}
