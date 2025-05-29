package com.stripe.android.connect.manager

import com.stripe.android.connect.FetchClientSecret
import com.stripe.android.connect.appearance.Appearance
import com.stripe.android.connect.appearance.fonts.CustomFontSource
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
@EmbeddedComponentManagerScope
internal interface EmbeddedComponentManagerComponent {

    val coordinator: EmbeddedComponentCoordinator

    @Subcomponent.Factory
    interface Factory {
        fun build(
            @BindsInstance @PublishableKey
            publishableKey: String,
            @BindsInstance fetchClientSecret: FetchClientSecret,
            @BindsInstance customFonts: List<CustomFontSource>,
            @BindsInstance appearance: Appearance,
        ): EmbeddedComponentManagerComponent
    }
}
