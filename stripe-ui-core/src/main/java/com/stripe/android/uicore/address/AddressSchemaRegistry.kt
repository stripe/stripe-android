package com.stripe.android.uicore.address

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.address.schemas.AcAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AqAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ArAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AxAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.AzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BbAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BhAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BjAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BqAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BvAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ByAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.BzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ChAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ClAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CvAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.CzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.DeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.DjAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.DkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.DmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.DoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.DzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.EcAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.EeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.EgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.EhAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ErAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.EsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.EtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.FiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.FjAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.FkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.FoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.FrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GbAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GhAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GpAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GqAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.GyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.HkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.HnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.HrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.HtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.HuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.IdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.IeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.IlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ImAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.InAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.IoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.IqAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.IsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ItAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.JeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.JmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.JoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.JpAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KhAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.KzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LbAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LcAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LvAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.LyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.McAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MqAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MvAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MxAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.MzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NcAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NpAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.NzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.OmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PhAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.PyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.QaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ReAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.RoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.RsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.RuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.RwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SbAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ScAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ShAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SiAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SjAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SoAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.StAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SvAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SxAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.SzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TcAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TdAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ThAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TjAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TlAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ToAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TrAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TvAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.TzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.UaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.UgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.UsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.UyAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.UzAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.VaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.VcAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.VeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.VgAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.VnAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.VuAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.WfAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.WsAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.XkAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.YeAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.YtAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ZaAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ZmAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ZwAddressSchemaDefinition
import com.stripe.android.uicore.address.schemas.ZzAddressSchemaDefinition
import kotlin.String

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AddressSchemaRegistry {
    internal val defaultSchema: AddressSchemaDefinition
        get() = ZzAddressSchemaDefinition

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    internal fun getDefinition(countryCode: String?): AddressSchemaDefinition? = when (countryCode) {
        null -> defaultSchema
        "AC" -> AcAddressSchemaDefinition
        "AD" -> AdAddressSchemaDefinition
        "AE" -> AeAddressSchemaDefinition
        "AF" -> AfAddressSchemaDefinition
        "AG" -> AgAddressSchemaDefinition
        "AI" -> AiAddressSchemaDefinition
        "AL" -> AlAddressSchemaDefinition
        "AM" -> AmAddressSchemaDefinition
        "AO" -> AoAddressSchemaDefinition
        "AQ" -> AqAddressSchemaDefinition
        "AR" -> ArAddressSchemaDefinition
        "AT" -> AtAddressSchemaDefinition
        "AU" -> AuAddressSchemaDefinition
        "AW" -> AwAddressSchemaDefinition
        "AX" -> AxAddressSchemaDefinition
        "AZ" -> AzAddressSchemaDefinition
        "BA" -> BaAddressSchemaDefinition
        "BB" -> BbAddressSchemaDefinition
        "BD" -> BdAddressSchemaDefinition
        "BE" -> BeAddressSchemaDefinition
        "BF" -> BfAddressSchemaDefinition
        "BG" -> BgAddressSchemaDefinition
        "BH" -> BhAddressSchemaDefinition
        "BI" -> BiAddressSchemaDefinition
        "BJ" -> BjAddressSchemaDefinition
        "BL" -> BlAddressSchemaDefinition
        "BM" -> BmAddressSchemaDefinition
        "BN" -> BnAddressSchemaDefinition
        "BO" -> BoAddressSchemaDefinition
        "BQ" -> BqAddressSchemaDefinition
        "BR" -> BrAddressSchemaDefinition
        "BS" -> BsAddressSchemaDefinition
        "BT" -> BtAddressSchemaDefinition
        "BV" -> BvAddressSchemaDefinition
        "BW" -> BwAddressSchemaDefinition
        "BY" -> ByAddressSchemaDefinition
        "BZ" -> BzAddressSchemaDefinition
        "CA" -> CaAddressSchemaDefinition
        "CD" -> CdAddressSchemaDefinition
        "CF" -> CfAddressSchemaDefinition
        "CG" -> CgAddressSchemaDefinition
        "CH" -> ChAddressSchemaDefinition
        "CI" -> CiAddressSchemaDefinition
        "CK" -> CkAddressSchemaDefinition
        "CL" -> ClAddressSchemaDefinition
        "CM" -> CmAddressSchemaDefinition
        "CN" -> CnAddressSchemaDefinition
        "CO" -> CoAddressSchemaDefinition
        "CR" -> CrAddressSchemaDefinition
        "CV" -> CvAddressSchemaDefinition
        "CW" -> CwAddressSchemaDefinition
        "CY" -> CyAddressSchemaDefinition
        "CZ" -> CzAddressSchemaDefinition
        "DE" -> DeAddressSchemaDefinition
        "DJ" -> DjAddressSchemaDefinition
        "DK" -> DkAddressSchemaDefinition
        "DM" -> DmAddressSchemaDefinition
        "DO" -> DoAddressSchemaDefinition
        "DZ" -> DzAddressSchemaDefinition
        "EC" -> EcAddressSchemaDefinition
        "EE" -> EeAddressSchemaDefinition
        "EG" -> EgAddressSchemaDefinition
        "EH" -> EhAddressSchemaDefinition
        "ER" -> ErAddressSchemaDefinition
        "ES" -> EsAddressSchemaDefinition
        "ET" -> EtAddressSchemaDefinition
        "FI" -> FiAddressSchemaDefinition
        "FJ" -> FjAddressSchemaDefinition
        "FK" -> FkAddressSchemaDefinition
        "FO" -> FoAddressSchemaDefinition
        "FR" -> FrAddressSchemaDefinition
        "GA" -> GaAddressSchemaDefinition
        "GB" -> GbAddressSchemaDefinition
        "GD" -> GdAddressSchemaDefinition
        "GE" -> GeAddressSchemaDefinition
        "GF" -> GfAddressSchemaDefinition
        "GG" -> GgAddressSchemaDefinition
        "GH" -> GhAddressSchemaDefinition
        "GI" -> GiAddressSchemaDefinition
        "GL" -> GlAddressSchemaDefinition
        "GM" -> GmAddressSchemaDefinition
        "GN" -> GnAddressSchemaDefinition
        "GP" -> GpAddressSchemaDefinition
        "GQ" -> GqAddressSchemaDefinition
        "GR" -> GrAddressSchemaDefinition
        "GS" -> GsAddressSchemaDefinition
        "GT" -> GtAddressSchemaDefinition
        "GU" -> GuAddressSchemaDefinition
        "GW" -> GwAddressSchemaDefinition
        "GY" -> GyAddressSchemaDefinition
        "HK" -> HkAddressSchemaDefinition
        "HN" -> HnAddressSchemaDefinition
        "HR" -> HrAddressSchemaDefinition
        "HT" -> HtAddressSchemaDefinition
        "HU" -> HuAddressSchemaDefinition
        "ID" -> IdAddressSchemaDefinition
        "IE" -> IeAddressSchemaDefinition
        "IL" -> IlAddressSchemaDefinition
        "IM" -> ImAddressSchemaDefinition
        "IN" -> InAddressSchemaDefinition
        "IO" -> IoAddressSchemaDefinition
        "IQ" -> IqAddressSchemaDefinition
        "IS" -> IsAddressSchemaDefinition
        "IT" -> ItAddressSchemaDefinition
        "JE" -> JeAddressSchemaDefinition
        "JM" -> JmAddressSchemaDefinition
        "JO" -> JoAddressSchemaDefinition
        "JP" -> JpAddressSchemaDefinition
        "KE" -> KeAddressSchemaDefinition
        "KG" -> KgAddressSchemaDefinition
        "KH" -> KhAddressSchemaDefinition
        "KI" -> KiAddressSchemaDefinition
        "KM" -> KmAddressSchemaDefinition
        "KN" -> KnAddressSchemaDefinition
        "KR" -> KrAddressSchemaDefinition
        "KW" -> KwAddressSchemaDefinition
        "KY" -> KyAddressSchemaDefinition
        "KZ" -> KzAddressSchemaDefinition
        "LA" -> LaAddressSchemaDefinition
        "LB" -> LbAddressSchemaDefinition
        "LC" -> LcAddressSchemaDefinition
        "LI" -> LiAddressSchemaDefinition
        "LK" -> LkAddressSchemaDefinition
        "LR" -> LrAddressSchemaDefinition
        "LS" -> LsAddressSchemaDefinition
        "LT" -> LtAddressSchemaDefinition
        "LU" -> LuAddressSchemaDefinition
        "LV" -> LvAddressSchemaDefinition
        "LY" -> LyAddressSchemaDefinition
        "MA" -> MaAddressSchemaDefinition
        "MC" -> McAddressSchemaDefinition
        "MD" -> MdAddressSchemaDefinition
        "ME" -> MeAddressSchemaDefinition
        "MF" -> MfAddressSchemaDefinition
        "MG" -> MgAddressSchemaDefinition
        "MK" -> MkAddressSchemaDefinition
        "ML" -> MlAddressSchemaDefinition
        "MM" -> MmAddressSchemaDefinition
        "MN" -> MnAddressSchemaDefinition
        "MO" -> MoAddressSchemaDefinition
        "MQ" -> MqAddressSchemaDefinition
        "MR" -> MrAddressSchemaDefinition
        "MS" -> MsAddressSchemaDefinition
        "MT" -> MtAddressSchemaDefinition
        "MU" -> MuAddressSchemaDefinition
        "MV" -> MvAddressSchemaDefinition
        "MW" -> MwAddressSchemaDefinition
        "MX" -> MxAddressSchemaDefinition
        "MY" -> MyAddressSchemaDefinition
        "MZ" -> MzAddressSchemaDefinition
        "NA" -> NaAddressSchemaDefinition
        "NC" -> NcAddressSchemaDefinition
        "NE" -> NeAddressSchemaDefinition
        "NG" -> NgAddressSchemaDefinition
        "NI" -> NiAddressSchemaDefinition
        "NL" -> NlAddressSchemaDefinition
        "NO" -> NoAddressSchemaDefinition
        "NP" -> NpAddressSchemaDefinition
        "NR" -> NrAddressSchemaDefinition
        "NU" -> NuAddressSchemaDefinition
        "NZ" -> NzAddressSchemaDefinition
        "OM" -> OmAddressSchemaDefinition
        "PA" -> PaAddressSchemaDefinition
        "PE" -> PeAddressSchemaDefinition
        "PF" -> PfAddressSchemaDefinition
        "PG" -> PgAddressSchemaDefinition
        "PH" -> PhAddressSchemaDefinition
        "PK" -> PkAddressSchemaDefinition
        "PL" -> PlAddressSchemaDefinition
        "PM" -> PmAddressSchemaDefinition
        "PN" -> PnAddressSchemaDefinition
        "PR" -> PrAddressSchemaDefinition
        "PS" -> PsAddressSchemaDefinition
        "PT" -> PtAddressSchemaDefinition
        "PY" -> PyAddressSchemaDefinition
        "QA" -> QaAddressSchemaDefinition
        "RE" -> ReAddressSchemaDefinition
        "RO" -> RoAddressSchemaDefinition
        "RS" -> RsAddressSchemaDefinition
        "RU" -> RuAddressSchemaDefinition
        "RW" -> RwAddressSchemaDefinition
        "SA" -> SaAddressSchemaDefinition
        "SB" -> SbAddressSchemaDefinition
        "SC" -> ScAddressSchemaDefinition
        "SE" -> SeAddressSchemaDefinition
        "SG" -> SgAddressSchemaDefinition
        "SH" -> ShAddressSchemaDefinition
        "SI" -> SiAddressSchemaDefinition
        "SJ" -> SjAddressSchemaDefinition
        "SK" -> SkAddressSchemaDefinition
        "SL" -> SlAddressSchemaDefinition
        "SM" -> SmAddressSchemaDefinition
        "SN" -> SnAddressSchemaDefinition
        "SO" -> SoAddressSchemaDefinition
        "SR" -> SrAddressSchemaDefinition
        "SS" -> SsAddressSchemaDefinition
        "ST" -> StAddressSchemaDefinition
        "SV" -> SvAddressSchemaDefinition
        "SX" -> SxAddressSchemaDefinition
        "SZ" -> SzAddressSchemaDefinition
        "TA" -> TaAddressSchemaDefinition
        "TC" -> TcAddressSchemaDefinition
        "TD" -> TdAddressSchemaDefinition
        "TF" -> TfAddressSchemaDefinition
        "TG" -> TgAddressSchemaDefinition
        "TH" -> ThAddressSchemaDefinition
        "TJ" -> TjAddressSchemaDefinition
        "TK" -> TkAddressSchemaDefinition
        "TL" -> TlAddressSchemaDefinition
        "TM" -> TmAddressSchemaDefinition
        "TN" -> TnAddressSchemaDefinition
        "TO" -> ToAddressSchemaDefinition
        "TR" -> TrAddressSchemaDefinition
        "TT" -> TtAddressSchemaDefinition
        "TV" -> TvAddressSchemaDefinition
        "TW" -> TwAddressSchemaDefinition
        "TZ" -> TzAddressSchemaDefinition
        "UA" -> UaAddressSchemaDefinition
        "UG" -> UgAddressSchemaDefinition
        "US" -> UsAddressSchemaDefinition
        "UY" -> UyAddressSchemaDefinition
        "UZ" -> UzAddressSchemaDefinition
        "VA" -> VaAddressSchemaDefinition
        "VC" -> VcAddressSchemaDefinition
        "VE" -> VeAddressSchemaDefinition
        "VG" -> VgAddressSchemaDefinition
        "VN" -> VnAddressSchemaDefinition
        "VU" -> VuAddressSchemaDefinition
        "WF" -> WfAddressSchemaDefinition
        "WS" -> WsAddressSchemaDefinition
        "XK" -> XkAddressSchemaDefinition
        "YE" -> YeAddressSchemaDefinition
        "YT" -> YtAddressSchemaDefinition
        "ZA" -> ZaAddressSchemaDefinition
        "ZM" -> ZmAddressSchemaDefinition
        "ZW" -> ZwAddressSchemaDefinition
        "ZZ" -> ZzAddressSchemaDefinition
        else -> null
    }

    fun get(countryCode: String?): List<CountryAddressSchema>? {
        return getDefinition(countryCode)?.schemaElements()
    }
}
