@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.mock
import java.lang.ref.WeakReference

internal class CheckoutControllerReferencesTest {
    @After
    fun tearDown() {
        CheckoutControllerReferences.clear()
    }

    @Test
    fun `lookup returns only the controller registered for the exact ID`() {
        val controller = mock<CheckoutController>()
        CheckoutControllerReferences.register("first", controller)

        assertThat(CheckoutControllerReferences["first"]).isSameInstanceAs(controller)
        assertThat(CheckoutControllerReferences["second"]).isNull()
    }

    @Test
    fun `lookup removes a cleared weak reference`() {
        val reference = WeakReference(mock<CheckoutController>())
        CheckoutControllerReferences.register("controller", reference)
        reference.clear()

        assertThat(CheckoutControllerReferences["controller"]).isNull()
        assertThat(CheckoutControllerReferences["controller"]).isNull()
    }

    @Test
    fun `register replaces the controller for the same ID`() {
        val original = mock<CheckoutController>()
        val recreated = mock<CheckoutController>()
        CheckoutControllerReferences.register("controller", original)

        CheckoutControllerReferences.register("controller", recreated)

        assertThat(CheckoutControllerReferences["controller"]).isSameInstanceAs(recreated)
    }

    @Test
    fun `stale controller cannot unregister its replacement`() {
        val original = mock<CheckoutController>()
        val recreated = mock<CheckoutController>()
        CheckoutControllerReferences.register("controller", original)
        CheckoutControllerReferences.register("controller", recreated)

        CheckoutControllerReferences.unregister("controller", original)

        assertThat(CheckoutControllerReferences["controller"]).isSameInstanceAs(recreated)
    }
}
