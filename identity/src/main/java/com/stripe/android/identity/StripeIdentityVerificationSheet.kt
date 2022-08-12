package com.stripe.android.identity

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.identity.injection.DaggerIdentityVerificationSheetComponent
import com.stripe.android.identity.injection.IdentityVerificationSheetComponent

internal class StripeIdentityVerificationSheet internal constructor(
    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args>,
    context: Context,
    private val configuration: IdentityVerificationSheet.Configuration
) : IdentityVerificationSheet, Injector {

    constructor(
        from: ComponentActivity,
        configuration: IdentityVerificationSheet.Configuration,
        identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
    ) : this(
        from.registerForActivityResult(
            IdentityVerificationSheetContract(),
            identityVerificationCallback::onVerificationFlowResult
        ),
        from,
        configuration
    )

    constructor(
        from: Fragment,
        configuration: IdentityVerificationSheet.Configuration,
        identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
    ) : this(
        from.registerForActivityResult(
            IdentityVerificationSheetContract(),
            identityVerificationCallback::onVerificationFlowResult
        ),
        from.requireContext(),
        configuration
    )

    @InjectorKey
    private val injectorKey: String =
        WeakMapInjectorRegistry.nextKey(requireNotNull(StripeIdentityVerificationSheet::class.simpleName))

    init {
        WeakMapInjectorRegistry.register(this, injectorKey)
    }

    private val identityVerificationSheetComponent: IdentityVerificationSheetComponent =
        DaggerIdentityVerificationSheetComponent.builder()
            .context(context)
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
