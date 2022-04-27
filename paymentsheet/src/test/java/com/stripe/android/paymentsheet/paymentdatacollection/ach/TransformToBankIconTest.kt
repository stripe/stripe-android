package com.stripe.android.paymentsheet.paymentdatacollection.ach

import com.stripe.android.paymentsheet.R
import org.junit.Assert
import org.junit.Test

class TransformToBankIconTest {
    @Test
    fun `given valid bank name, transform returns the correct bank icon`() {
        // bank of america
        Assert.assertEquals(TransformToBankIcon("Bank of America"), R.drawable.stripe_ic_bank_boa)
        Assert.assertEquals(TransformToBankIcon("bank of america"), R.drawable.stripe_ic_bank_boa)
        Assert.assertEquals(TransformToBankIcon("BANKof AMERICA"), R.drawable.stripe_ic_bank)

        // capital one
        Assert.assertEquals(TransformToBankIcon("Capital One"), R.drawable.stripe_ic_bank_capitalone)
        Assert.assertEquals(TransformToBankIcon("capital one"), R.drawable.stripe_ic_bank_capitalone)
        Assert.assertEquals(TransformToBankIcon("Capital      One"), R.drawable.stripe_ic_bank)

        // compass
        Assert.assertEquals(TransformToBankIcon("BBVA"), R.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("bbva"), R.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("Compass"), R.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("compass"), R.drawable.stripe_ic_bank_compass)
        Assert.assertEquals(TransformToBankIcon("b b v a"), R.drawable.stripe_ic_bank)

        // citi
        Assert.assertEquals(TransformToBankIcon("Citibank"), R.drawable.stripe_ic_bank_citi)
        Assert.assertEquals(TransformToBankIcon("citibank"), R.drawable.stripe_ic_bank_citi)
        Assert.assertEquals(TransformToBankIcon("Citi Bank"), R.drawable.stripe_ic_bank)

        // morgan chase
        Assert.assertEquals(TransformToBankIcon("MORGAN CHASE"), R.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("morgan chase"), R.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("JP MORGAN"), R.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("jp morgan"), R.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("Chase"), R.drawable.stripe_ic_bank_morganchase)
        Assert.assertEquals(TransformToBankIcon("chase"), R.drawable.stripe_ic_bank_morganchase)

        // nfcu
        Assert.assertEquals(TransformToBankIcon("NAVY FEDERAL CREDIT UNION"), R.drawable.stripe_ic_bank_nfcu)
        Assert.assertEquals(TransformToBankIcon("navy federal credit union"), R.drawable.stripe_ic_bank_nfcu)
        Assert.assertEquals(TransformToBankIcon("NFCU"), R.drawable.stripe_ic_bank)

        // pnc
        Assert.assertEquals(TransformToBankIcon("PNC BANK"), R.drawable.stripe_ic_bank_pnc)
        Assert.assertEquals(TransformToBankIcon("pnc bank"), R.drawable.stripe_ic_bank_pnc)
        Assert.assertEquals(TransformToBankIcon("PNC Bank"), R.drawable.stripe_ic_bank_pnc)
        Assert.assertEquals(TransformToBankIcon("PNC  BANK"), R.drawable.stripe_ic_bank)

        // suntrust
        Assert.assertEquals(TransformToBankIcon("SUNTRUST"), R.drawable.stripe_ic_bank_suntrust)
        Assert.assertEquals(TransformToBankIcon("suntrust"), R.drawable.stripe_ic_bank_suntrust)
        Assert.assertEquals(TransformToBankIcon("SunTrust Bank"), R.drawable.stripe_ic_bank_suntrust)
        Assert.assertEquals(TransformToBankIcon("suntrust bank"), R.drawable.stripe_ic_bank_suntrust)

        // svb
        Assert.assertEquals(TransformToBankIcon("Silicon Valley Bank"), R.drawable.stripe_ic_bank_svb)
        Assert.assertEquals(TransformToBankIcon("silicon valley bank"), R.drawable.stripe_ic_bank_svb)
        Assert.assertEquals(TransformToBankIcon("svb"), R.drawable.stripe_ic_bank)

        // stripe
        Assert.assertEquals(TransformToBankIcon("stripe"), R.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("Stripe"), R.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("stripe"), R.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("TestInstitution"), R.drawable.stripe_ic_bank_stripe)
        Assert.assertEquals(TransformToBankIcon("testinstitution"), R.drawable.stripe_ic_bank_stripe)

        // td
        Assert.assertEquals(TransformToBankIcon("TD Bank"), R.drawable.stripe_ic_bank_td)
        Assert.assertEquals(TransformToBankIcon("td bank"), R.drawable.stripe_ic_bank_td)
        Assert.assertEquals(TransformToBankIcon("TD"), R.drawable.stripe_ic_bank)

        // usaa
        Assert.assertEquals(TransformToBankIcon("USAA FEDERAL SAVINGS BANK"), R.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("usaa federal savings bank"), R.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("USAA Bank"), R.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("usaa bank"), R.drawable.stripe_ic_bank_usaa)
        Assert.assertEquals(TransformToBankIcon("USAA Savings Bank"), R.drawable.stripe_ic_bank)

        // us bank
        Assert.assertEquals(TransformToBankIcon("U.S. BANK"), R.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("u.s. bank"), R.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("u.s. Bank"), R.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("US Bank"), R.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("us bank"), R.drawable.stripe_ic_bank_usbank)
        Assert.assertEquals(TransformToBankIcon("usbank"), R.drawable.stripe_ic_bank)

        // wells fargo
        Assert.assertEquals(TransformToBankIcon("Wells Fargo"), R.drawable.stripe_ic_bank_wellsfargo)
        Assert.assertEquals(TransformToBankIcon("wells fargo"), R.drawable.stripe_ic_bank_wellsfargo)
        Assert.assertEquals(TransformToBankIcon("WELLS FARGO"), R.drawable.stripe_ic_bank_wellsfargo)
        Assert.assertEquals(TransformToBankIcon("Well's Fargo"), R.drawable.stripe_ic_bank)
    }
}
