package com.stripe.form

interface FormFieldSpec<T> : ContentSpec {
    val state: FormFieldState<T>
}