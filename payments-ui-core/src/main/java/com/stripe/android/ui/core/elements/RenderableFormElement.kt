package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.Controller
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class RenderableFormElement(
    override val identifier: IdentifierSpec,
    override val allowsUserInteraction: Boolean,
) : FormElement {
    override val controller: Controller? = null
    override val mandateText: ResolvableString? = null

    @Composable
    abstract fun ComposeUI(enabled: Boolean)
}
