package com.stripe.android.paymentsheet.elements

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.specifications.IdentifierSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class SaveForFutureUseControllerTest {
    private val mandateIdentifier = IdentifierSpec.Generic("mandate")
    private val nameSectionIdentifier = IdentifierSpec.Name
    private val hiddenIdentifiers = listOf(nameSectionIdentifier, mandateIdentifier)
    private val saveForFutureUseController = SaveForFutureUseController(hiddenIdentifiers)

    @Test
    fun `Save for future use is initialized as true and no hidden items`() =
        runBlocking {
            assertThat(saveForFutureUseController.saveForFutureUse.first()).isTrue()
            assertThat(saveForFutureUseController.hiddenIdentifiers.first()).isEmpty()
        }

    @Test
    fun `Save for future use when set to false shows the hidden items specified`() =
        runBlocking {
            saveForFutureUseController.onValueChange(false)
            assertThat(saveForFutureUseController.saveForFutureUse.first()).isFalse()
            assertThat(saveForFutureUseController.hiddenIdentifiers.first()).isEqualTo(
                hiddenIdentifiers
            )
        }
}
