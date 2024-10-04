package com.stripe.android.link.ui.signup

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.TextFieldController

data class SignUpScreenState(
    val emailController: TextFieldController,
    val phoneNumberController: PhoneNumberController,
    val nameController: TextFieldController,
    val signUpEnabled: Boolean,
    val signUpState: SignUpState = SignUpState.InputtingPrimaryField,
    val errorMessage: ResolvableString? = null
)
