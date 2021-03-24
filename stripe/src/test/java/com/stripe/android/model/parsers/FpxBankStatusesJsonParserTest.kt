package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.BankStatuses
import org.json.JSONObject
import kotlin.test.Test

class FpxBankStatusesJsonParserTest {

    @Test
    fun parse_withValidData_createsExpectedObject() {
        assertThat(FpxBankStatusesJsonParser().parse(DEFAULT))
            .isEqualTo(
                BankStatuses(
                    mapOf(
                        "PBB0233" to true,
                        "MBB0228" to true,
                        "BKRM0602" to true,
                        "MB2U0227" to true,
                        "ABMB0212" to false,
                        "BIMB0340" to false,
                        "CIT0219" to false,
                        "BMMB0341" to true,
                        "KFH0346" to true,
                        "ABB0233" to true,
                        "UOB0229" to false,
                        "RHB0218" to false,
                        "OCBC0229" to true,
                        "SCB0216" to false,
                        "HLB0224" to true,
                        "ABB0234" to true,
                        "TEST0023" to true,
                        "TEST0022" to true,
                        "TEST0021" to true,
                        "UOB0226" to false,
                        "BCBB0235" to true,
                        "AMBB0209" to true,
                        "BSN0601" to true,
                        "HSBC0223" to true
                    )
                )
            )
    }

    @Test
    fun parse_withEmptyData_createsExpectedObject() {
        assertThat(FpxBankStatusesJsonParser().parse(JSONObject()))
            .isEqualTo(BankStatuses())
    }

    private companion object {
        private val DEFAULT = JSONObject(
            """
            {
                "parsed_bank_status": {
                    "PBB0233": true,
                    "MBB0228": true,
                    "BKRM0602": true,
                    "MB2U0227": true,
                    "ABMB0212": false,
                    "BIMB0340": false,
                    "CIT0219": false,
                    "BMMB0341": true,
                    "KFH0346": true,
                    "ABB0233": true,
                    "UOB0229": false,
                    "RHB0218": false,
                    "OCBC0229": true,
                    "SCB0216": false,
                    "HLB0224": true,
                    "ABB0234": true,
                    "TEST0023": true,
                    "TEST0022": true,
                    "TEST0021": true,
                    "UOB0226": false,
                    "BCBB0235": true,
                    "AMBB0209": true,
                    "BSN0601": true,
                    "HSBC0223": true
                }
            }
            """.trimIndent()
        )
    }
}
