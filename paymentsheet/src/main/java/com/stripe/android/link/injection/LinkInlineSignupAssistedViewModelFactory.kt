package com.stripe.android.link.injection

import com.stripe.android.link.ui.inline.InlineSignupViewModel
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import dagger.assisted.AssistedFactory

@AssistedFactory
internal interface LinkInlineSignupAssistedViewModelFactory {
    fun create(signupMode: LinkSignupMode, initialUserInput: UserInput?): InlineSignupViewModel
}
