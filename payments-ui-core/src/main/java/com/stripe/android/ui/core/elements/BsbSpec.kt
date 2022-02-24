package com.stripe.android.ui.core.elements

import com.stripe.android.view.BecsDebitBanks
import kotlinx.parcelize.Parcelize

@Parcelize
internal object BsbSpec : SectionFieldSpec(IdentifierSpec.Generic("bsb_number")) {
    fun transform(): SectionFieldElement =
        BsbElement(
            this.identifier,
            TextFieldController(BsbConfig(banks))
        )
}

internal val banks: List<BecsDebitBanks.Bank> = listOf(
    BecsDebitBanks.Bank(
        "00",
        "STRIPE",
        "Stripe Test Bank"
    ),
    BecsDebitBanks.Bank(
        "10",
        "BSA",
        "BankSA (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "11",
        "STG and SGP",
        "St George Bank (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "12",
        "BQL",
        "Bank of Queensland"
    ),
    BecsDebitBanks.Bank(
        "14",
        "PIB",
        "Rabobank"
    ),
    BecsDebitBanks.Bank(
        "15",
        "T&C",
        "Town & Country Bank"
    ),
    BecsDebitBanks.Bank(
        "18",
        "MBL",
        "Macquarie Bank"
    ),
    BecsDebitBanks.Bank(
        "19",
        "BOM and BML",
        "Bank of Melbourne (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "21",
        "CMB",
        "JP Morgan Chase Bank"
    ),
    BecsDebitBanks.Bank(
        "22",
        "BNP",
        "BNP Paribas"
    ),
    BecsDebitBanks.Bank(
        "23",
        "BAL",
        "Bank of America"
    ),
    BecsDebitBanks.Bank(
        "24",
        "CTI",
        "Citibank"
    ),
    BecsDebitBanks.Bank(
        "25",
        "BPS",
        "BNP Paribas Securities"
    ),
    BecsDebitBanks.Bank(
        "26",
        "BTA",
        "Bankers Trust Australia (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "29",
        "BOT",
        "Bank of Tokyo-Mitsubishi"
    ),
    BecsDebitBanks.Bank(
        "30",
        "BWA",
        "Bankwest (division of Commonwealth Bank)"
    ),
    BecsDebitBanks.Bank(
        "31",
        "MCU",
        "Bankmecu"
    ),
    BecsDebitBanks.Bank(
        "33",
        "STG and SGP",
        "St George Bank (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "34",
        "HBA and HSB",
        "HSBC Bank Australia"
    ),
    BecsDebitBanks.Bank(
        "35",
        "BOC and BCA",
        "Bank of China"
    ),
    BecsDebitBanks.Bank(
        "40",
        "CST",
        "Commonwealth Bank of Australia"
    ),
    BecsDebitBanks.Bank(
        "41",
        "DBA",
        "Deutsche Bank"
    ),
    BecsDebitBanks.Bank(
        "42",
        "TBT",
        "Commonwealth Bank of Australia"
    ),
    BecsDebitBanks.Bank(
        "45",
        "OCB",
        "OCBC Bank"
    ),
    BecsDebitBanks.Bank(
        "46",
        "ADV",
        "Advance Bank (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "47",
        "CBL",
        "Challenge Bank (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "48",
        "MET or SUN",
        "Suncorp-Metway"
    ),
    BecsDebitBanks.Bank(
        "52",
        "TBT",
        "Commonwealth Bank of Australia"
    ),
    BecsDebitBanks.Bank(
        "55",
        "BOM and BML",
        "Bank of Melbourne (division of Westpac Bank)"
    ),
    BecsDebitBanks.Bank(
        "57",
        "ASL",
        "Australian Settlements"
    ),
    BecsDebitBanks.Bank(
        "61",
        "ADL",
        "Adelaide Bank (division of Bendigo and Adelaide Bank)"
    ),
    BecsDebitBanks.Bank(
        "70",
        "CUS",
        "Indue"
    ),
    BecsDebitBanks.Bank(
        "73",
        "WBC",
        "Westpac Banking Corporation"
    ),
    BecsDebitBanks.Bank(
        "76",
        "CBA",
        "Commonwealth Bank of Australia"
    ),
    BecsDebitBanks.Bank(
        "78",
        "NAB",
        "National Australia Bank"
    ),
    BecsDebitBanks.Bank(
        "80",
        "CRU",
        "Cuscal"
    ),
    BecsDebitBanks.Bank(
        "90",
        "APO",
        "Australia Post"
    ),
    BecsDebitBanks.Bank(
        "325",
        "BYB",
        "Beyond Bank Australia"
    ),
    BecsDebitBanks.Bank(
        "432",
        "SCB",
        "Standard Chartered Bank"
    ),
    BecsDebitBanks.Bank(
        "510",
        "CNA",
        "Citibank N.A."
    ),
    BecsDebitBanks.Bank(
        "512",
        "CFC",
        "Community First Credit Union"
    ),
    BecsDebitBanks.Bank(
        "514",
        "QTM",
        "QT Mutual Bank"
    ),
    BecsDebitBanks.Bank(
        "517",
        "VOL",
        "Australian Settlements Limited"
    ),
    BecsDebitBanks.Bank(
        "533",
        "BCC",
        "Bananacoast Community Credit Union"
    ),
    BecsDebitBanks.Bank(
        "611",
        "SEL",
        "Select Credit Union"
    ),
    BecsDebitBanks.Bank(
        "630",
        "ABS",
        "ABS Building Society"
    ),
    BecsDebitBanks.Bank(
        "632",
        "BAE",
        "B&E"
    ),
    BecsDebitBanks.Bank(
        "633",
        "BBL",
        "Bendigo Bank"
    ),
    BecsDebitBanks.Bank(
        "634",
        "UFS",
        "Uniting Financial Services"
    ),
    BecsDebitBanks.Bank(
        "636",
        "HAY",
        "Cuscal Limited"
    ),
    BecsDebitBanks.Bank(
        "637",
        "GBS",
        "Greater Building Society"
    ),
    BecsDebitBanks.Bank(
        "638",
        "HBS",
        "Heritage Bank"
    ),
    BecsDebitBanks.Bank(
        "639",
        "HOM",
        "Home Building Society (division of Bank of Queensland)"
    ),
    BecsDebitBanks.Bank(
        "640",
        "HUM",
        "Hume Bank"
    ),
    BecsDebitBanks.Bank(
        "641",
        "IMB and AUB",
        "IMB"
    ),
    BecsDebitBanks.Bank(
        "642",
        "ADC",
        "Australian Defence Credit Union"
    ),
    BecsDebitBanks.Bank(
        "645",
        "MPB and BAY",
        "Wide Bay Australia"
    ),
    BecsDebitBanks.Bank(
        "646",
        "MMB",
        "Maitland Mutual Building Society"
    ),
    BecsDebitBanks.Bank(
        "647",
        "IMB and AUB",
        "IMB"
    ),
    BecsDebitBanks.Bank(
        "650",
        "NEW",
        "Newcastle Permanent Building Society"
    ),
    BecsDebitBanks.Bank(
        "653",
        "PPB",
        "Pioneer Permanent Building Society (division of Bank of Queensland)"
    ),
    BecsDebitBanks.Bank(
        "654",
        "ECU",
        "ECU Australia"
    ),
    BecsDebitBanks.Bank(
        "655",
        "ROK",
        "The Rock Building Society"
    ),
    BecsDebitBanks.Bank(
        "656",
        "MPB and BAY",
        "Wide Bay Australia"
    ),
    BecsDebitBanks.Bank(
        "657",
        "GBS",
        "Greater Building Society"
    ),
    BecsDebitBanks.Bank(
        "659",
        "SGE",
        "SGE Credit Union"
    ),
    BecsDebitBanks.Bank(
        "664",
        "MET or SUN",
        "Suncorp-Metway"
    ),
    BecsDebitBanks.Bank(
        "670",
        "YOU",
        "Cuscal Limited"
    ),
    BecsDebitBanks.Bank(
        "676",
        "GTW",
        "Gateway Credit Union"
    ),
    BecsDebitBanks.Bank(
        "721",
        "HCC",
        "Holiday Coast Credit Union"
    ),
    BecsDebitBanks.Bank(
        "722",
        "SNX",
        "Southern Cross Credit"
    ),
    BecsDebitBanks.Bank(
        "723",
        "HIC",
        "Heritage Isle Credit Union"
    ),
    BecsDebitBanks.Bank(
        "724",
        "RCU",
        "Railways Credit Union"
    ),
    BecsDebitBanks.Bank(
        "725",
        "JUD",
        "Judo Bank Pty Ltd"
    ),
    BecsDebitBanks.Bank(
        "728",
        "SCU",
        "Summerland Credit Union"
    ),
    BecsDebitBanks.Bank(
        "775",
        "XIN",
        "Australian Settlements Limited"
    ),
    BecsDebitBanks.Bank(
        "777",
        "PNB",
        "Police & Nurse"
    ),
    BecsDebitBanks.Bank(
        "812",
        "TMB",
        "Teachers Mutual Bank"
    ),
    BecsDebitBanks.Bank(
        "813",
        "CAP",
        "Capricornian"
    ),
    BecsDebitBanks.Bank(
        "814",
        "CUA",
        "Credit Union Australia"
    ),
    BecsDebitBanks.Bank(
        "815",
        "PCU",
        "Police Bank"
    ),
    BecsDebitBanks.Bank(
        "817",
        "WCU",
        "Warwick Credit Union"
    ),
    BecsDebitBanks.Bank(
        "818",
        "COM",
        "Bank of Communications"
    ),
    BecsDebitBanks.Bank(
        "819",
        "IBK",
        "Industrial & Commercial Bank of China"
    ),
    BecsDebitBanks.Bank(
        "823",
        "ENC",
        "Encompass Credit Union"
    ),
    BecsDebitBanks.Bank(
        "824",
        "STH",
        "Sutherland Credit Union"
    ),
    BecsDebitBanks.Bank(
        "825",
        "SKY",
        "Big Sky Building Society"
    ),
    BecsDebitBanks.Bank(
        "833",
        "DBL",
        "Defence Bank Limited"
    ),
    BecsDebitBanks.Bank(
        "880",
        "HBS",
        "Heritage Bank"
    ),
    BecsDebitBanks.Bank(
        "882",
        "MMP",
        "Maritime Mining & Power Credit Union"
    ),
    BecsDebitBanks.Bank(
        "888",
        "CCB",
        "China Construction Bank Corporation"
    ),
    BecsDebitBanks.Bank(
        "889",
        "DBS",
        "DBS Bank Ltd."
    ),
    BecsDebitBanks.Bank(
        "911",
        "SMB",
        "Sumitomo Mitsui Banking Corporation"
    ),
    BecsDebitBanks.Bank(
        "913",
        "SSB",
        "State Street Bank & Trust Company"
    ),
    BecsDebitBanks.Bank(
        "917",
        "ARA",
        "Arab Bank Australia"
    ),
    BecsDebitBanks.Bank(
        "918",
        "MCB",
        "Mizuho Bank"
    ),
    BecsDebitBanks.Bank(
        "922",
        "UOB",
        "United Overseas Bank"
    ),
    BecsDebitBanks.Bank(
        "923",
        "ING or GNI",
        "ING Bank"
    ),
    BecsDebitBanks.Bank(
        "931",
        "ICB",
        "Mega International Commercial Bank"
    ),
    BecsDebitBanks.Bank(
        "932",
        "NEC",
        "Community Mutual"
    ),
    BecsDebitBanks.Bank(
        "936",
        "ING or GNI",
        "ING Bank"
    ),
    BecsDebitBanks.Bank(
        "939",
        "AMP",
        "AMP Bank"
    ),
    BecsDebitBanks.Bank(
        "941",
        "BCY",
        "Delphi Bank (division of Bendigo and Adelaide Bank)"
    ),
    BecsDebitBanks.Bank(
        "942",
        "LBA",
        "Bank of Sydney"
    ),
    BecsDebitBanks.Bank(
        "943",
        "TBB",
        "Taiwan Business Bank"
    ),
    BecsDebitBanks.Bank(
        "944",
        "MEB",
        "Members Equity Bank"
    ),
    BecsDebitBanks.Bank(
        "946",
        "UBS",
        "UBS AG"
    ),
    BecsDebitBanks.Bank(
        "951",
        "INV",
        "BOQ Specialist Bank"
    ),
    BecsDebitBanks.Bank(
        "952",
        "RBS",
        "Royal Bank of Scotland"
    ),
    BecsDebitBanks.Bank(
        "969",
        "MSL",
        "Tyro Payments"
    ),
    BecsDebitBanks.Bank(
        "980",
        "BOC and BCA",
        "Bank of China"
    ),
    BecsDebitBanks.Bank(
        "985",
        "HBA and HSB",
        "HSBC Bank Australia"
    ),
    BecsDebitBanks.Bank(
        "01",
        "ANZ",
        "Australia and New Zealand Banking Group"
    ),
    BecsDebitBanks.Bank(
        "03",
        "WBC",
        "Westpac Banking Corporation"
    ),
    BecsDebitBanks.Bank(
        "04",
        "WBC",
        "Westpac Banking Corporation"
    ),
    BecsDebitBanks.Bank(
        "06",
        "CBA",
        "Commonwealth Bank of Australia"
    ),
    BecsDebitBanks.Bank(
        "08",
        "NAB",
        "National Australia Bank"
    ),
    BecsDebitBanks.Bank(
        "09",
        "RBA",
        "Reserve Bank of Australia"
    ),
)
