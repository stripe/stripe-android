package com.stripe.android.migration

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.github.detekt.test.utils.compileContentForTest
import org.junit.Test
import kotlin.test.assertEquals

class UnusedImportRemovalTest {
    
    @Test
    fun `removes unused Stripe imports only`() {
        val inputCode = """
            package com.stripe.android.test
            
            import com.stripe.android.paymentsheet.PaymentSheet
            import com.stripe.android.elements.CardBrandAcceptance
            import java.util.List
            
            fun test() {
                val x = CardBrandAcceptance.All  // PaymentSheet unused, List unused
            }
        """.trimIndent()
        
        val expectedCode = """
            package com.stripe.android.test
            
            import com.stripe.android.elements.CardBrandAcceptance
            import java.util.List
            
            fun test() {
                val x = CardBrandAcceptance.All  // PaymentSheet unused, List unused
            }
        """.trimIndent()
        
        val rule = UnusedImportRemoval(TestConfig("autoCorrect" to true))
        val ktFile = compileContentForTest(inputCode)
        rule.visitFile(ktFile)
        
        val correctedCode = ktFile.text
        assertEquals(expectedCode.trim(), correctedCode.trim())
    }
    
    @Test
    fun `does not remove used Stripe imports`() {
        val inputCode = """
            package com.stripe.android.test
            
            import com.stripe.android.paymentsheet.PaymentSheet
            import com.stripe.android.elements.CardBrandAcceptance
            
            fun test() {
                val sheet = PaymentSheet()
                val acceptance = CardBrandAcceptance.All
            }
        """.trimIndent()
        
        val rule = UnusedImportRemoval(TestConfig("autoCorrect" to true))
        val ktFile = compileContentForTest(inputCode)
        rule.visitFile(ktFile)
        
        val correctedCode = ktFile.text
        assertEquals(inputCode.trim(), correctedCode.trim())
    }
}