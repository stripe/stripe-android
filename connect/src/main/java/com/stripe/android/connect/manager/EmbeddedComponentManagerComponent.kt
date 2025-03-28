package com.stripe.android.connect.manager

import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.ClientSecretProvider
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
@EmbeddedComponentManagerScope
@OptIn(PrivateBetaConnectSDK::class)
internal interface EmbeddedComponentManagerComponent {

    val coordinator: EmbeddedComponentCoordinator

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance configuration: EmbeddedComponentManager.Configuration,
            @BindsInstance clientSecretProvider: ClientSecretProvider,
            @BindsInstance customFonts: List<CustomFontSource>,
            @BindsInstance appearance: Appearance,
        ): EmbeddedComponentManagerComponent
    }
}
