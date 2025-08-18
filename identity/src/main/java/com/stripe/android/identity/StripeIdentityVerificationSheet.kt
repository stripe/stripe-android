package com.stripe.android.identity

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.identity.injection.DaggerIdentityVerificationSheetComponent
import com.stripe.android.identity.injection.IdentityVerificationSheetComponent

internal class StripeIdentityVerificationSheet internal constructor(
    lifecycleOwner: LifecycleOwner,
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    identityVerificationSheetContract: IdentityVerificationSheetContract,
    private val identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback,
    context: Context,
    private val configuration: IdentityVerificationSheet.Configuration
) : IdentityVerificationSheet, Injector {

    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args>

    @InjectorKey
    private val injectorKey: String =
        WeakMapInjectorRegistry.nextKey(requireNotNull(StripeIdentityVerificationSheet::class.simpleName))

    init {
        check(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED))

        activityResultLauncher = activityResultRegistryOwner.activityResultRegistry.register(
            key = "StripeIdentityVerificationSheet_ActivityResultLauncher",
            contract = identityVerificationSheetContract,
        ) { result ->
            identityVerificationCallback.onVerificationFlowResult(result)
        }

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    activityResultLauncher.unregister()
                }
            }
        )
    }

    private val identityVerificationSheetComponent: IdentityVerificationSheetComponent =
        DaggerIdentityVerificationSheetComponent.builder()
            .context(context.applicationContext)
            .build()

    override fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String
    ) {
        activityResultLauncher.launch(
            IdentityVerificationSheetContract.Args(
                verificationSessionId,
                ephemeralKeySecret,
                configuration.brandLogo,
                injectorKey,
                System.currentTimeMillis()
            )
        )
    }

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is IdentityActivity -> {
                identityVerificationSheetComponent.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }
}
