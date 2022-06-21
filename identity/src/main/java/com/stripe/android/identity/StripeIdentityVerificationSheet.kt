package com.stripe.android.identity

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.identity.injection.DaggerIdentityVerificationSheetComponent
import com.stripe.android.identity.injection.IdentityVerificationSheetComponent
import com.stripe.android.identity.viewmodel.IdentityViewModel

internal class StripeIdentityVerificationSheet private constructor(
    activityResultCaller: ActivityResultCaller,
    context: Context,
    private val configuration: IdentityVerificationSheet.Configuration,
    identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
) : IdentityVerificationSheet, Injector {

    constructor(
        from: ComponentActivity,
        configuration: IdentityVerificationSheet.Configuration,
        identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
    ) : this(
        from as ActivityResultCaller,
        from,
        configuration,
        identityVerificationCallback
    )

    constructor(
        from: Fragment,
        configuration: IdentityVerificationSheet.Configuration,
        identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
    ) : this(
        from as ActivityResultCaller,
        from.requireContext(),
        configuration,
        identityVerificationCallback
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

    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
            IdentityVerificationSheetContract(),
            identityVerificationCallback::onVerificationFlowResult
        )

    override fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String
    ) {
        activityResultLauncher.launch(
            IdentityVerificationSheetContract.Args(
                verificationSessionId,
                ephemeralKeySecret,
                configuration.brandLogo,
                injectorKey
            )
        )
    }

    override fun inject(injectable: Injectable<*>) {
        when (injectable) {
            is IdentityViewModel.IdentityViewModelFactory -> {
                identityVerificationSheetComponent.inject(injectable)
            }
            else -> {
                throw IllegalArgumentException("invalid Injectable $injectable requested in $this")
            }
        }
    }
}
