package com.stripe.form.example

import com.google.common.truth.Truth.assertThat
import com.stripe.form.ContentSpec

object ContentSpecMatcher {
    inline fun <reified T : ContentSpec> matches(
        spec: ContentSpec,
        block: (T) -> Unit
    ) {
        val spec = spec as? T
        assertThat(spec).isInstanceOf(T::class.java)
        spec ?: return
        block(spec)

    }
}