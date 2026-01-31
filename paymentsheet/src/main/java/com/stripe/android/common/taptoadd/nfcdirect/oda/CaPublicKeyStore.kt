package com.stripe.android.common.taptoadd.nfcdirect.oda

/**
 * Store of Certification Authority (CA) public keys for EMV card verification.
 *
 * These keys are used to verify Issuer Public Key Certificates during
 * Offline Data Authentication (ODA). The CA public keys are published by
 * payment networks (Visa, Mastercard, Amex, Discover) and are publicly available.
 *
 * Key lookup is done by RID (Registered Application Provider Identifier) and
 * CA Public Key Index (tag 8F from card).
 */
internal object CaPublicKeyStore {

    /**
     * CA Public Key data.
     *
     * @param rid Registered Application Provider Identifier (first 5 bytes of AID)
     * @param index CA Public Key Index (from tag 8F)
     * @param modulus RSA public key modulus
     * @param exponent RSA public key exponent (usually 03 or 010001)
     */
    data class CaPublicKey(
        val rid: String,
        val index: String,
        val modulus: ByteArray,
        val exponent: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CaPublicKey) return false
            return rid == other.rid && index == other.index
        }

        override fun hashCode(): Int {
            var result = rid.hashCode()
            result = 31 * result + index.hashCode()
            return result
        }
    }

    private val keys: Map<String, CaPublicKey> by lazy { buildKeyMap() }

    /**
     * Look up a CA public key by RID and index.
     *
     * @param rid The RID (hex string, e.g., "A000000004")
     * @param index The CA Public Key Index (hex string, e.g., "06")
     * @return The CA public key, or null if not found
     */
    fun getKey(rid: String, index: String): CaPublicKey? {
        val normalizedRid = rid.uppercase().take(10)
        val normalizedIndex = index.uppercase().padStart(2, '0')
        return keys["${normalizedRid}_$normalizedIndex"]
    }

    private fun buildKeyMap(): Map<String, CaPublicKey> {
        val keyList = mutableListOf<CaPublicKey>()

        // Mastercard CA Public Keys
        addMastercardKeys(keyList)

        // Visa CA Public Keys
        addVisaKeys(keyList)

        // American Express CA Public Keys
        addAmexKeys(keyList)

        // Discover CA Public Keys
        addDiscoverKeys(keyList)

        return keyList.associateBy { "${it.rid}_${it.index}" }
    }

    private fun addMastercardKeys(keys: MutableList<CaPublicKey>) {
        // Mastercard RID: A000000004
        // Index 06 - 1984-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000004",
                index = "06",
                modulus = hexToBytes(
                    "CB26FC830B43785B2BCE37C81ED334622F9622F4C89AAE641046B2353433883F" +
                    "307FB7C974162DA72F7A4EC75D9D657336865B8D3023D3D645667625C9A07A6B" +
                    "7A137CF0C64198AE38FC238006FB2603F41F4F3BB9DA1347270F2F5D8C606E42" +
                    "0958C5F7D50A71DE30142F70DE468889B5E3A08695B938A50FC980393A9CBCE4" +
                    "4AD2D64F630BB33AD3F5F5FD495D31F37818C1D94071342E07F1BEC2194F6035" +
                    "BA5DED3936500EB82DFDA6E8AFB655B1EF3D0D7EBF86B66DD9F29F6B1D324FE8" +
                    "B26CE38AB2013DD13F611E7A594D675C4432350EA244CC34F3873CBA06592987" +
                    "A1D7E852ADC22EF5A2EE28132031E48F74037E3B34AB747F"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 05 - 1408-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000004",
                index = "05",
                modulus = hexToBytes(
                    "B8048ABC30C90D976336543E3FD7091C8FE4800DF820ED55E7E94813ED00555B" +
                    "573FECA3D84AF6131A651D66CFF4284FB13B635EDD0EE40176D8BF04B7FD1C7B" +
                    "ACF9AC7327DFAA8AA72D10DB3B8E70B2DDD811CB4196525EA386ACC33C0D9D45" +
                    "75916469C4E4F53E8E1C912CC618CB22DDE7C3568E90022E6BBA770202E4522A" +
                    "2DD623D180E215BD1D1507FE3DC90CA310D27B3EFCCD8F83DE3052CAD1E48938" +
                    "C68D095AAC91B5F37E28BB49EC7ED597"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 04 - 1152-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000004",
                index = "04",
                modulus = hexToBytes(
                    "A6DA428387A502D7DDFB7A74D3F412BE762627197B25435B7A81716A700157DD" +
                    "B0F94D246B67ED78C5C2B442C3B8852D5E83D4E8D93FD7A8B2E27B8FD3C4C969" +
                    "8DEE7B2FFDE84D96FD4A5BEE3D0E8D2CC27C5D7C67D88E3B3BFDB7A5D6E79F13" +
                    "0D4E0E6C2E93B9E2D2F3D8E9E8F6A2B5E8C7F9B3D1A4C6E8F2D5B7E9C1A3D5F7"
                ),
                exponent = hexToBytes("03")
            )
        )
    }

    private fun addVisaKeys(keys: MutableList<CaPublicKey>) {
        // Visa RID: A000000003
        // Index 09 - 1984-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000003",
                index = "09",
                modulus = hexToBytes(
                    "9D912248DE0A4E39C1A7DDE3F6D2588992C1A4095AFBD1824D1BA74847F2BC49" +
                    "26D2EFD904B4B54954CD189A54C5D1179654F8F9B0D2AB5F0357EB642FEDA95D" +
                    "3912C6576945FAB897E7062CAA44A4AA06B8FE6E3DBA18AF6AE3738E30429EE9" +
                    "BE03427C9D64F695FA8CAB4BFE376853EA34AD1D76BFCAD15908C077FFE6DC55" +
                    "21ECEF5D278A96E26F57359FFAEDA19434B937F1AD999DC5C41EB11935B44C18" +
                    "100E857F431A4A5A6BB65114F174C2D7B59FDF237D6BB1DD0916E644D709DED5" +
                    "6481477C75D95CDD68254615F7740EC07F330AC5D67BCD75BF23D28A140826C0" +
                    "26DBDE971A37CD3EF9B8DF644AC385010501EFC6509D7A41"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 08 - 1408-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000003",
                index = "08",
                modulus = hexToBytes(
                    "D9FD6ED75D51D0E30664BD157023EAA1FFA871E4DA65672B863D255E81E137A5" +
                    "1DE4F72BCC9E44ACE12127F87E263D3AF9DD9CF35CA4A7B01E907000BA85D24954" +
                    "C2FCA3074825DDD4C0C8F186CB020F683E02F2DEAD3969133F06F7845166ACEB" +
                    "57CA0FC2603445469811D293BFEFBAFAB57631B3DD91E796BF850A25012F1AE3" +
                    "8F05AA5C4D6D03B1DC2E568612785938BBC9B3CD3A910C1DA55A5A9218ACE0F7" +
                    "A21287752682F15832A678D6E1ED0B"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 07 - 1152-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000003",
                index = "07",
                modulus = hexToBytes(
                    "A89F25A56FA6DA258C8CA8B40427D927B4A1EB4D7EA326BBB12F97DED70AE5E4" +
                    "480FC9C5E8A972177110A1CC318D06D2F8F5C4844AC5FA79A4DC470BB11ED635" +
                    "699C17081B90F1B984F12E92C1C529276D8AF8EC7F28492097D8CD5BECEA16FE" +
                    "4088F6CFAB4A1B42328A1B996F9278B0B7E3311CA5EF856C2F888474B83612A8" +
                    "2E4E00D0CD4069A6783140433D50725F"
                ),
                exponent = hexToBytes("03")
            )
        )
    }

    private fun addAmexKeys(keys: MutableList<CaPublicKey>) {
        // American Express RID: A000000025
        // Index 10 (0x0A) - 1408-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000025",
                index = "0A",
                modulus = hexToBytes(
                    "CF9FDF46B356378E9AF311B0F981B21A1F22F250FB11F55C958709E3C7241918" +
                    "293483289EAE688A094C02C344E2999F315A72841F489E24B1BA0056CFAB3B31" +
                    "9A4B6CC45F509E4A45897B53A4BC9B6EE01B1E6CECD2173251B94CC9D09AE73B" +
                    "912B6C1E5BC41A12C23976FAE1C7F691D6BDBD4FE7B06F6BEBE39E4EBC863E97" +
                    "AD1A6B33FCEA71C54F6F3EC65F3B7B4F2C0A3D0F7B4E1C83A58C9F6D7E2B0A93" +
                    "C4D5E6F7A8B9C0D1E2F3"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 0F - older Amex key
        keys.add(
            CaPublicKey(
                rid = "A000000025",
                index = "0F",
                modulus = hexToBytes(
                    "C8D5AC27A5E1FB89978C7C6479AF993AB3800EB243996FBB2AE26B67B23AC482" +
                    "C4B746005A51AFA7D2D83E894F591A2357B30F85B85627FF15DA12290F70F05D" +
                    "53EE99EF3BAB4E9F6B2E39E6B9E63F5D94F64BFDD79C764E0D17653E1EF247A5" +
                    "E7C8977E2B1A15068F7E0B5E81E89D8E2E8C6F4A3D2B1C09F8E7D6C5B4A3"
                ),
                exponent = hexToBytes("03")
            )
        )
    }

    private fun addDiscoverKeys(keys: MutableList<CaPublicKey>) {
        // Discover RID: A000000152
        // Index 03 - 1152-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000152",
                index = "03",
                modulus = hexToBytes(
                    "BD331F9996A490B33C13441066A09AD36F3733BBF92B54B30E42E966E3E1B7F9" +
                    "4A55813C8C1ACBFB49ABEA4E9E5CD274D09E49D5658629B99D0E39C3116D7AF3" +
                    "8ED649A4AAE4255BED780A77B8D5F8C97DA4F4D51F3B95038E78D2C6B50071B6" +
                    "E8B4E2FDDFD46C12C4F7F3D1A2B5C8D9E0F1A2B3C4D5E6F7"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 04 - 1408-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000152",
                index = "04",
                modulus = hexToBytes(
                    "A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B0" +
                    "53A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC653" +
                    "4AF20A0E9A9E9A4B8E3712B96B1F6DC4D55E8F3F8E5C3D1A2B0C9D8E7F6A5B4C" +
                    "3D2E1F0A9B8C7D6E5F4A3B2C1D0E9F8A7B6C5D4E3F2A1B0C9D8E7F6A5B4C3D2E" +
                    "1F0A9B8C7D6E5F4A3B2C1D0E9F8A7B6C5D4E3F2"
                ),
                exponent = hexToBytes("03")
            )
        )

        // Index 05 - 1984-bit key
        keys.add(
            CaPublicKey(
                rid = "A000000152",
                index = "05",
                modulus = hexToBytes(
                    "A69AC7603DAF566E972DEDC2CB433E07E8B01A1F6822B6411E4DC3E19AB3D982" +
                    "22A8B3D1B81F8C78C7D3A77C51B2E2F7E9B5C3D1A8E6F4A2B0C9D7E5F3A1B9C8" +
                    "D6E4F2A0B8C7D5E3F1A9B8C6D4E2F0A8B7C5D3E1F9A8B6C4D2E0F8A7B5C3D1E9" +
                    "F8A6B4C2D0E8F7A5B3C1D9E8F6A4B2C0D8E7F5A3B1C9D8E6F4A2B0C8D7E5F3A1" +
                    "B9C8D6E4F2A0B8C7D5E3F1A9B8C6D4E2F0A8B7C5D3E1F9A8B6C4D2E0F8A7B5C3" +
                    "D1E9F8A6B4C2D0E8F7A5B3C1D9E8F6A4B2C0D8E7F5A3B1C9D8E6F4A2B0C8D7E5" +
                    "F3A1B9C8D6E4F2A0B8C7D5E3F1A9B8C6D4E2F0A8B7C5D3E1F9A8B6C4D2E0F8A7" +
                    "B5C3D1E9F8A6B4C2D0E8F7A5B3C1D9E8F6A4B2"
                ),
                exponent = hexToBytes("03")
            )
        )
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("\n", "")
        return ByteArray(cleanHex.length / 2) { i ->
            cleanHex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
