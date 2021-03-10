package com.stripe.android.view

class NetbankingBank(
    override var id: String,
    override var code: String,
    override var displayName: String,
    override var brandIconResId: Int? = null
) : Bank() {

    companion object {
        fun values() : Array<NetbankingBank> {
            return arrayOf(
                NetbankingBank(
                    "162",
                    "kotak",
                    "Kotak Bank"
                ),
                NetbankingBank(
                    "ADB",
                    "andhra",
                    "Andhra Bank"
                ),
                NetbankingBank(
                    "ALB",
                    "allahabad",
                    "Allahabad Bank'"
                ),
                NetbankingBank(
                    "APG",
                    "andhra_pragathi",
                    "Andhra Pragathi Grameena Bank"
                ),
                NetbankingBank(
                    "ATP",
                    "airtel",
                    "Airtel Payment Bank"
                ),
                NetbankingBank(
                    "AUB",
                    "au_small_finance",
                    "AU Small Finance Bank"
                ),
                NetbankingBank(
                    "BBK",
                    "bank_of_bahrain_and_kuwait",
                    "Bank of Bahrain and Kuwait"
                ),
                NetbankingBank(
                    "BBR",
                    "bob",
                    "Bank of Baroda - Retail Banking"
                ),
                NetbankingBank(
                    "BCB",
                    "bassein_catholic",
                    "Bassein Catholic Co-operative Bank"
                ),
                NetbankingBank(
                    "BDN",
                    "bandhan",
                    "Bandhan Bank"
                ),
                NetbankingBank(
                    "BOI",
                    "bank_of_india",
                    "Bank of India"
                ),
                NetbankingBank(
                    "BOM",
                    "bank_of_maharashtra",
                    "Bank of Maharashtra"
                ),
                NetbankingBank(
                    "cbi001",
                    "central_bank_of_india",
                    "Central Bank of India"
                ),
                NetbankingBank(
                    "CNB",
                    "canara",
                    "Canara Bank"
                ),
                NetbankingBank(
                    "COB",
                    "cosmos",
                    "Cosmos Bank"
                ),
                NetbankingBank(
                    "CRP",
                    "corporation_bank",
                    "Corporation Bank"
                ),
                NetbankingBank(
                    "CSB",
                    "catholic_syrian",
                    "Catholic Syrian Bank"
                ),
                NetbankingBank(
                    "CUB",
                    "city_union",
                    "City Union Bank"
                ),
                NetbankingBank(
                    "DBK",
                    "deutsche",
                    "Deutsche Bank"
                ),
                NetbankingBank(
                    "DBS",
                    "digibank_dbs",
                    "Digibank by DBS"
                ),
                NetbankingBank(
                    "DCB",
                    "development_credit_bank",
                    "Development Credit Bank"
                ),
                NetbankingBank(
                    "DEN",
                    "dena",
                    "Dena Bank"
                ),
                NetbankingBank(
                    "DLB",
                    "dhanlakshmi",
                    "Dhanlakshmi Bank"
                ),
                NetbankingBank(
                    "EQB",
                    "equitas_small_finance",
                    "Equitas Small Finance Bank"
                ),
                NetbankingBank(
                    "ESF",
                    "esaf",
                    "ESAF Small Finance Bank"
                ),
                NetbankingBank(
                    "FBK",
                    "federal_bank",
                    "Federal Bank"
                ),
                NetbankingBank(
                    "FNC",
                    "fincare",
                    "Fincare Bank"
                ),
                NetbankingBank(
                    "HDF",
                    "hdfc",
                    "HDFC Bank"
                ),
                NetbankingBank(
                    "ICI",
                    "icici",
                    "ICICI Bank"
                ),
                NetbankingBank(
                    "IDB",
                    "idbi",
                    "IDBI Bank"
                ),
                NetbankingBank(
                    "IDN",
                    "idfc_first",
                    "IDFC FIRST Bank"
                ),
                NetbankingBank(
                    "IDS",
                    "indusind",
                    "IndusInd Bank"
                ),
                NetbankingBank(
                    "INB",
                    "indian_bank",
                    "Indian Bank"
                ),
                NetbankingBank(
                    "IOB",
                    "indian_overseas",
                    "Indian Overseas Bank"
                ),
                NetbankingBank(
                    "JKB",
                    "jnk",
                    "Jammu & Kashmir Bank"
                ),
                NetbankingBank(
                    "JNB",
                    "jana_small_finance",
                    "Jana Small Finance Bank"
                ),
                NetbankingBank(
                    "JSB",
                    "janata_sahakari_bank",
                    "Janata Sahakari Bank Ltd Pune"
                ),
                NetbankingBank(
                    "KBL",
                    "karnataka_bank",
                    "Karnataka Bank Limited"
                ),
                NetbankingBank(
                    "KJB",
                    "kalyan_janata",
                    "Kalyan Janata Sahakari Bank"
                ),
                NetbankingBank(
                    "KLB",
                    "The Kalupur Commercial Co-operative Bank",
                    "kalupur"
                ),
                NetbankingBank(
                    "KVB",
                    "karur_vysya",
                    "Karur Vysya Bank"
                ),
                NetbankingBank(
                    "KVG",
                    "kvg",
                    "Karnataka Vikas Grameena Bank"
                ),
                NetbankingBank(
                    "LVR",
                    "kvg",
                    "Laxmi Vilas Bank"
                ),
                NetbankingBank(
                    "MSB",
                    "mehsana",
                    "Mehsana urban Co-operative Bank"
                ),
                NetbankingBank(
                    "NEB",
                    "ne_small_finance",
                    "North East Small Finance Bank"
                ),
                NetbankingBank(
                    "NKB",
                    "nkgsb",
                    "NKGSB Co-op Bank"
                ),
                NetbankingBank(
                    "OBC",
                    "obc",
                    "PNB (Erstwhile-Oriental Bank of Commerce)"
                ),
                NetbankingBank(
                    "PNB",
                    "pnb",
                    "Punjab National Bank - Retail Banking"
                ),
                NetbankingBank(
                    "PSB",
                    "punjab_and_sind",
                    "Punjab & Sind Bank"
                ),
                NetbankingBank(
                    "RBL",
                    "rbl",
                    "RBL Bank Limited"
                ),
                NetbankingBank(
                    "SBI",
                    "sbi",
                    "State Bank of India"
                ),
                NetbankingBank(
                    "SCB",
                    "scb",
                    "Standard Chartered Bank"
                ),
                NetbankingBank(
                    "SHB",
                    "shivalik",
                    "Shivalik Mercantile Cooperative Bank Ltd"
                ),
                NetbankingBank(
                    "SIB",
                    "south_indian_bank",
                    "South Indian Bank"
                ),
                NetbankingBank(
                    "SRB",
                    "suryoday",
                    "Suryoday Small Finance Bank"
                ),
                NetbankingBank(
                    "SWB",
                    "saraswat",
                    "Saraswat Bank"
                ),
                NetbankingBank(
                    "SYD",
                    "syndicate",
                    "Syndicate Bank"
                ),
                NetbankingBank(
                    "TBB",
                    "thane_bharat",
                    "Thane Bharat Sahakari Bank Ltd"
                ),
                NetbankingBank(
                    "TJB",
                    "tjsb",
                    "TJSB Bank"
                ),
                NetbankingBank(
                    "TMB",
                    "tamilnad_mercantile",
                    "Tamilnad Mercantile Bank Limited"
                ),
                NetbankingBank(
                    "TNC",
                    "tnc",
                    "Tamil Nadu State Co-operative Bank"
                ),
                NetbankingBank(
                    "UBI",
                    "ubi",
                    "Union Bank of India"
                ),
                NetbankingBank(
                    "UNI",
                    "united_bank_of_india",
                    "PNB (Erstwhile-United Bank of India )"
                ),
                NetbankingBank(
                    "UTI",
                    "axis",
                    "Axis Bank"
                ),
                NetbankingBank(
                    "VJB",
                    "vijaya",
                    "Vijaya Bank"
                ),
                NetbankingBank(
                    "VRB",
                    "varachha",
                    "Varachha Co-operative Bank Limited"
                ),
                NetbankingBank(
                    "YBK",
                    "yes",
                    "Yes Bank"
                ),
                NetbankingBank(
                    "ZOB",
                    "zoroastrian",
                    "Zoroastrian Co-operative Bank Limited"
                ),
                NetbankingBank(
                    "PKB",
                    "karnataka_gramin",
                    "Karnataka Gramin Bank"
                ),
                NetbankingBank(
                    "SVC",
                    "shamrao_vithal",
                    "Shamrao Vithal Co-op Bank"
                ),
                NetbankingBank(
                    "NUT",
                    "nutan_nagrik",
                    "Nutan Nagrik Bank"
                ),
                NetbankingBank(
                    "BBC",
                    "bob_corp",
                    "Bank of Baroda - Corporate Banking"
                ),
                NetbankingBank(
                    "CPN",
                    "pnb_corp",
                    "Punjab National Bank - Corporate Banking"
                ),
                NetbankingBank(
                    "SV2",
                    "shamrao_vithal_corp",
                    "Shamrao Vithal Co-op Bank - Corporate"
                ),
                NetbankingBank(
                    "BNP",
                    "bnp_paribas",
                    "BNP Paribas"
                ),
                NetbankingBank(
                    "RTC",
                    "rbl_corp",
                    "RBL Bank Limited - Corporate Banking"
                ),
                NetbankingBank(
                    "ICO",
                    "icici_corp",
                    "ICICI Corporate Netbanking"
                ),
                NetbankingBank(
                    "IDC",
                    "idbi_corp",
                    "IDBI Corporate"
                ),
                NetbankingBank(
                    "AXC",
                    "axis_corp",
                    "Axis Bank Corporate"
                ),
                NetbankingBank(
                    "ADC",
                    "andhra_corp",
                    "Andhra Bank Corporate"
                ),
                NetbankingBank(
                    "DL2",
                    "dhanlaxmi_corp",
                    "Dhanlaxmi Bank Corporate"
                ),
                NetbankingBank(
                    "ALC",
                    "allahabad_corp",
                    "Allahabad Bank Corporate"
                ),
                NetbankingBank(
                    "CH3",
                    "hdfc_corp",
                    "HDFC Bank Corporate"
                ),
                NetbankingBank(
                    "YBC",
                    "yes_corp",
                    "Yes Bank Corporate"
                ),
                NetbankingBank(
                    "CR2",
                    "corporation_bank_corp",
                    "Corporation Bank - Corporate"
                ),
                NetbankingBank(
                    "BRL",
                    "barclays_corp",
                    "Barclays Bank - Corporate Net Banking"
                ),
            )
        }

        /* Return the [NetbankingBank] that matches the given bank code (e.g. "kotak", "andhra"),
         * or null if no match is found.
         *
         * The bank code should be obtained from [PaymentMethod.Netbanking.bank].
         */
        @JvmStatic
        fun get(bankCode: String?): NetbankingBank? {
            return NetbankingBank.values().firstOrNull { it.code == bankCode }
        }
    }
}