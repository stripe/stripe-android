package com.stripe.android.view

internal enum class NetbankingBank(
    override val id: String,
    override val code: String,
    override val displayName: String,
) : Bank {
    KotakBank(
        "162",
        "kotak",
        "Kotak Bank"
    ),
    AndraBank(
        "ADB",
        "andhra",
        "Andhra Bank"
    ),
    AllahabadBank(
        "ALB",
        "allahabad",
        "Allahabad Bank"
    ),
    AndhraPragathiBank(
        "APG",
        "andhra_pragathi",
        "Andhra Pragathi Grameena Bank"
    ),
    AirtelBank(
        "ATP",
        "airtel",
        "Airtel Payment Bank"
    ),
    AUSmallFinanceBank(
        "AUB",
        "au_small_finance",
        "AU Small Finance Bank"
    ),
    BankOfBahrainAndKuwait(
        "BBK",
        "bank_of_bahrain_and_kuwait",
        "Bank of Bahrain and Kuwait"
    ),
    BankOfBaroda(
        "BBR",
        "bob",
        "Bank of Baroda - Retail Banking"
    ),
    BasseinCatholicBank(
        "BCB",
        "bassein_catholic",
        "Bassein Catholic Co-operative Bank"
    ),
    BandhanBank(
        "BDN",
        "bandhan",
        "Bandhan Bank"
    ),
    BankOfIndia(
        "BOI",
        "bank_of_india",
        "Bank of India"
    ),
    BankofMaharashtra(
        "BOM",
        "bank_of_maharashtra",
        "Bank of Maharashtra"
    ),
    CentralBankOfIndia(
        "cbi001",
        "central_bank_of_india",
        "Central Bank of India"
    ),
    CanaraBank(
        "CNB",
        "canara",
        "Canara Bank"
    ),
    CosmosBank(
        "COB",
        "cosmos",
        "Cosmos Bank"
    ),
    CorporationBank(
        "CRP",
        "corporation_bank",
        "Corporation Bank"
    ),
    CatholicSyrianBank(
        "CSB",
        "catholic_syrian",
        "Catholic Syrian Bank"
    ),
    CityUnionBank(
        "CUB",
        "city_union",
        "City Union Bank"
    ),
    DeutscheBank(
        "DBK",
        "deutsche",
        "Deutsche Bank"
    ),
    Digibank(
        "DBS",
        "digibank_dbs",
        "Digibank by DBS"
    ),
    DevelopmentCreditBank(
        "DCB",
        "development_credit_bank",
        "Development Credit Bank"
    ),
    DenaBank(
        "DEN",
        "dena",
        "Dena Bank"
    ),
    DhanlakshmiBank(
        "DLB",
        "dhanlakshmi",
        "Dhanlakshmi Bank"
    ),
    EquitasSmallFinanceBank(
        "EQB",
        "equitas_small_finance",
        "Equitas Small Finance Bank"
    ),
    ESAFBank(
        "ESF",
        "esaf",
        "ESAF Small Finance Bank"
    ),
    FederalBank(
        "FBK",
        "federal_bank",
        "Federal Bank"
    ),
    FincareBank(
        "FNC",
        "fincare",
        "Fincare Bank"
    ),
    HDFCBank(
        "HDF",
        "hdfc",
        "HDFC Bank"
    ),
    ICICIBank(
        "ICI",
        "icici",
        "ICICI Bank"
    ),
    IDBIBank(
        "IDB",
        "idbi",
        "IDBI Bank"
    ),
    IDFCBank(
        "IDN",
        "idfc_first",
        "IDFC FIRST Bank"
    ),
    IndusIndBank(
        "IDS",
        "indusind",
        "IndusInd Bank"
    ),
    IndianBank(
        "INB",
        "indian_bank",
        "Indian Bank"
    ),
    IndianOverseasBank(
        "IOB",
        "indian_overseas",
        "Indian Overseas Bank"
    ),
    JammuKashmirBank(
        "JKB",
        "jnk",
        "Jammu & Kashmir Bank"
    ),
    JanaBank(
        "JNB",
        "jana_small_finance",
        "Jana Small Finance Bank"
    ),
    JanataSahakariBank(
        "JSB",
        "janata_sahakari_bank",
        "Janata Sahakari Bank Ltd Pune"
    ),
    KarnatakaBank(
        "KBL",
        "karnataka_bank",
        "Karnataka Bank Limited"
    ),
    KalyanJanataBank(
        "KJB",
        "kalyan_janata",
        "Kalyan Janata Sahakari Bank"
    ),
    KalpurBank(
        "KLB",
        "The Kalupur Commercial Co-operative Bank",
        "kalupur"
    ),
    KarurVysyaBank(
        "KVB",
        "karur_vysya",
        "Karur Vysya Bank"
    ),
    KarnatakaVikasGrameenaBank(
        "KVG",
        "kvg",
        "Karnataka Vikas Grameena Bank"
    ),
    LaxmiVilasBank(
        "LVR",
        "kvg",
        "Laxmi Vilas Bank"
    ),
    MehsanaBank(
        "MSB",
        "mehsana",
        "Mehsana urban Co-operative Bank"
    ),
    NEBank(
        "NEB",
        "ne_small_finance",
        "North East Small Finance Bank"
    ),
    NKGSBBank(
        "NKB",
        "nkgsb",
        "NKGSB Co-op Bank"
    ),
    OBCBank(
        "OBC",
        "obc",
        "PNB (Erstwhile-Oriental Bank of Commerce)"
    ),
    PunjabNationalBank(
        "PNB",
        "pnb",
        "Punjab National Bank - Retail Banking"
    ),
    PunjabAndSindBank(
        "PSB",
        "punjab_and_sind",
        "Punjab & Sind Bank"
    ),
    RBLBank(
        "RBL",
        "rbl",
        "RBL Bank Limited"
    ),
    SBI(
        "SBI",
        "sbi",
        "State Bank of India"
    ),
    StandardCharteredBank(
        "SCB",
        "scb",
        "Standard Chartered Bank"
    ),
    ShivalikMercantileBank(
        "SHB",
        "shivalik",
        "Shivalik Mercantile Cooperative Bank Ltd"
    ),
    SouthIndianBank(
        "SIB",
        "south_indian_bank",
        "South Indian Bank"
    ),
    SuryodayBank(
        "SRB",
        "suryoday",
        "Suryoday Small Finance Bank"
    ),
    SaraswatBank(
        "SWB",
        "saraswat",
        "Saraswat Bank"
    ),
    SyndicateBank(
        "SYD",
        "syndicate",
        "Syndicate Bank"
    ),
    ThaneBharatBank(
        "TBB",
        "thane_bharat",
        "Thane Bharat Sahakari Bank Ltd"
    ),
    TJSBBank(
        "TJB",
        "tjsb",
        "TJSB Bank"
    ),
    TamilnadMercantileBank(
        "TMB",
        "tamilnad_mercantile",
        "Tamilnad Mercantile Bank Limited"
    ),
    TamilNaduStateBank(
        "TNC",
        "tnc",
        "Tamil Nadu State Co-operative Bank"
    ),
    UBI(
        "UBI",
        "ubi",
        "Union Bank of India"
    ),
    UNIBank(
        "UNI",
        "united_bank_of_india",
        "PNB (Erstwhile-United Bank of India)"
    ),
    AxisBank(
        "UTI",
        "axis",
        "Axis Bank"
    ),
    VijayaBank(
        "VJB",
        "vijaya",
        "Vijaya Bank"
    ),
    VarachhaBank(
        "VRB",
        "varachha",
        "Varachha Co-operative Bank Limited"
    ),
    YesBank(
        "YBK",
        "yes",
        "Yes Bank"
    ),
    ZoroastrianBank(
        "ZOB",
        "zoroastrian",
        "Zoroastrian Co-operative Bank Limited"
    ),
    KarnatakaGraminBank(
        "PKB",
        "karnataka_gramin",
        "Karnataka Gramin Bank"
    ),
    ShamraoVithalBank(
        "SVC",
        "shamrao_vithal",
        "Shamrao Vithal Co-op Bank"
    ),
    NutanNagrikBank(
        "NUT",
        "nutan_nagrik",
        "Nutan Nagrik Bank"
    ),
    BOBBank(
        "BBC",
        "bob_corp",
        "Bank of Baroda - Corporate Banking"
    ),
    PunjabNationalCorpBank(
        "CPN",
        "pnb_corp",
        "Punjab National Bank - Corporate Banking"
    ),
    ShamraoVithalCorpBank(
        "SV2",
        "shamrao_vithal_corp",
        "Shamrao Vithal Co-op Bank - Corporate"
    ),
    BNPParibas(
        "BNP",
        "bnp_paribas",
        "BNP Paribas"
    ),
    RBLCorpBank(
        "RTC",
        "rbl_corp",
        "RBL Bank Limited - Corporate Banking"
    ),
    ICICICorpBank(
        "ICO",
        "icici_corp",
        "ICICI Corporate Netbanking"
    ),
    IDBICorpBank(
        "IDC",
        "idbi_corp",
        "IDBI Corporate"
    ),
    AxisCorpBank(
        "AXC",
        "axis_corp",
        "Axis Bank Corporate"
    ),
    AndhraCorpBank(
        "ADC",
        "andhra_corp",
        "Andhra Bank Corporate"
    ),
    DhanlaxmiCorpBank(
        "DL2",
        "dhanlaxmi_corp",
        "Dhanlaxmi Bank Corporate"
    ),
    AllahabadCorpBank(
        "ALC",
        "allahabad_corp",
        "Allahabad Bank Corporate"
    ),
    HFDCCorpBank(
        "CH3",
        "hdfc_corp",
        "HDFC Bank Corporate"
    ),
    YesCorpBank(
        "YBC",
        "yes_corp",
        "Yes Bank Corporate"
    ),
    CorporationBankCorporate(
        "CR2",
        "corporation_bank_corp",
        "Corporation Bank - Corporate"
    ),
    BarclaysCorpBank(
        "BRL",
        "barclays_corp",
        "Barclays Bank - Corporate Net Banking"
    );

    companion object {
        /**
         * Return the [NetbankingBank] that matches the given bank code (e.g. "bank_of_india"),
         * or null if no match is found.
         *
         * The bank code should be obtained from [PaymentMethod.NetbankingBank.bank].
         */
        @JvmStatic
        fun get(bankCode: String?): NetbankingBank? {
            return entries.firstOrNull { it.code == bankCode }
        }
    }
}
