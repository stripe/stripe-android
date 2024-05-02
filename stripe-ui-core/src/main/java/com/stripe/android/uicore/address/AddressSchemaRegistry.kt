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
import kotlin.collections.Map

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AddressSchemaRegistry {
    internal val defaultSchema = ZzAddressSchemaDefinition

    internal val all: Map<String, AddressSchemaDefinition> = mapOf(
        AcAddressSchemaDefinition.countryCode to AcAddressSchemaDefinition,
        AdAddressSchemaDefinition.countryCode to AdAddressSchemaDefinition,
        AeAddressSchemaDefinition.countryCode to AeAddressSchemaDefinition,
        AfAddressSchemaDefinition.countryCode to AfAddressSchemaDefinition,
        AgAddressSchemaDefinition.countryCode to AgAddressSchemaDefinition,
        AiAddressSchemaDefinition.countryCode to AiAddressSchemaDefinition,
        AlAddressSchemaDefinition.countryCode to AlAddressSchemaDefinition,
        AmAddressSchemaDefinition.countryCode to AmAddressSchemaDefinition,
        AoAddressSchemaDefinition.countryCode to AoAddressSchemaDefinition,
        AqAddressSchemaDefinition.countryCode to AqAddressSchemaDefinition,
        ArAddressSchemaDefinition.countryCode to ArAddressSchemaDefinition,
        AtAddressSchemaDefinition.countryCode to AtAddressSchemaDefinition,
        AuAddressSchemaDefinition.countryCode to AuAddressSchemaDefinition,
        AwAddressSchemaDefinition.countryCode to AwAddressSchemaDefinition,
        AxAddressSchemaDefinition.countryCode to AxAddressSchemaDefinition,
        AzAddressSchemaDefinition.countryCode to AzAddressSchemaDefinition,
        BaAddressSchemaDefinition.countryCode to BaAddressSchemaDefinition,
        BbAddressSchemaDefinition.countryCode to BbAddressSchemaDefinition,
        BdAddressSchemaDefinition.countryCode to BdAddressSchemaDefinition,
        BeAddressSchemaDefinition.countryCode to BeAddressSchemaDefinition,
        BfAddressSchemaDefinition.countryCode to BfAddressSchemaDefinition,
        BgAddressSchemaDefinition.countryCode to BgAddressSchemaDefinition,
        BhAddressSchemaDefinition.countryCode to BhAddressSchemaDefinition,
        BiAddressSchemaDefinition.countryCode to BiAddressSchemaDefinition,
        BjAddressSchemaDefinition.countryCode to BjAddressSchemaDefinition,
        BlAddressSchemaDefinition.countryCode to BlAddressSchemaDefinition,
        BmAddressSchemaDefinition.countryCode to BmAddressSchemaDefinition,
        BnAddressSchemaDefinition.countryCode to BnAddressSchemaDefinition,
        BoAddressSchemaDefinition.countryCode to BoAddressSchemaDefinition,
        BqAddressSchemaDefinition.countryCode to BqAddressSchemaDefinition,
        BrAddressSchemaDefinition.countryCode to BrAddressSchemaDefinition,
        BsAddressSchemaDefinition.countryCode to BsAddressSchemaDefinition,
        BtAddressSchemaDefinition.countryCode to BtAddressSchemaDefinition,
        BvAddressSchemaDefinition.countryCode to BvAddressSchemaDefinition,
        BwAddressSchemaDefinition.countryCode to BwAddressSchemaDefinition,
        ByAddressSchemaDefinition.countryCode to ByAddressSchemaDefinition,
        BzAddressSchemaDefinition.countryCode to BzAddressSchemaDefinition,
        CaAddressSchemaDefinition.countryCode to CaAddressSchemaDefinition,
        CdAddressSchemaDefinition.countryCode to CdAddressSchemaDefinition,
        CfAddressSchemaDefinition.countryCode to CfAddressSchemaDefinition,
        CgAddressSchemaDefinition.countryCode to CgAddressSchemaDefinition,
        ChAddressSchemaDefinition.countryCode to ChAddressSchemaDefinition,
        CiAddressSchemaDefinition.countryCode to CiAddressSchemaDefinition,
        CkAddressSchemaDefinition.countryCode to CkAddressSchemaDefinition,
        ClAddressSchemaDefinition.countryCode to ClAddressSchemaDefinition,
        CmAddressSchemaDefinition.countryCode to CmAddressSchemaDefinition,
        CnAddressSchemaDefinition.countryCode to CnAddressSchemaDefinition,
        CoAddressSchemaDefinition.countryCode to CoAddressSchemaDefinition,
        CrAddressSchemaDefinition.countryCode to CrAddressSchemaDefinition,
        CvAddressSchemaDefinition.countryCode to CvAddressSchemaDefinition,
        CwAddressSchemaDefinition.countryCode to CwAddressSchemaDefinition,
        CyAddressSchemaDefinition.countryCode to CyAddressSchemaDefinition,
        CzAddressSchemaDefinition.countryCode to CzAddressSchemaDefinition,
        DeAddressSchemaDefinition.countryCode to DeAddressSchemaDefinition,
        DjAddressSchemaDefinition.countryCode to DjAddressSchemaDefinition,
        DkAddressSchemaDefinition.countryCode to DkAddressSchemaDefinition,
        DmAddressSchemaDefinition.countryCode to DmAddressSchemaDefinition,
        DoAddressSchemaDefinition.countryCode to DoAddressSchemaDefinition,
        DzAddressSchemaDefinition.countryCode to DzAddressSchemaDefinition,
        EcAddressSchemaDefinition.countryCode to EcAddressSchemaDefinition,
        EeAddressSchemaDefinition.countryCode to EeAddressSchemaDefinition,
        EgAddressSchemaDefinition.countryCode to EgAddressSchemaDefinition,
        EhAddressSchemaDefinition.countryCode to EhAddressSchemaDefinition,
        ErAddressSchemaDefinition.countryCode to ErAddressSchemaDefinition,
        EsAddressSchemaDefinition.countryCode to EsAddressSchemaDefinition,
        EtAddressSchemaDefinition.countryCode to EtAddressSchemaDefinition,
        FiAddressSchemaDefinition.countryCode to FiAddressSchemaDefinition,
        FjAddressSchemaDefinition.countryCode to FjAddressSchemaDefinition,
        FkAddressSchemaDefinition.countryCode to FkAddressSchemaDefinition,
        FoAddressSchemaDefinition.countryCode to FoAddressSchemaDefinition,
        FrAddressSchemaDefinition.countryCode to FrAddressSchemaDefinition,
        GaAddressSchemaDefinition.countryCode to GaAddressSchemaDefinition,
        GbAddressSchemaDefinition.countryCode to GbAddressSchemaDefinition,
        GdAddressSchemaDefinition.countryCode to GdAddressSchemaDefinition,
        GeAddressSchemaDefinition.countryCode to GeAddressSchemaDefinition,
        GfAddressSchemaDefinition.countryCode to GfAddressSchemaDefinition,
        GgAddressSchemaDefinition.countryCode to GgAddressSchemaDefinition,
        GhAddressSchemaDefinition.countryCode to GhAddressSchemaDefinition,
        GiAddressSchemaDefinition.countryCode to GiAddressSchemaDefinition,
        GlAddressSchemaDefinition.countryCode to GlAddressSchemaDefinition,
        GmAddressSchemaDefinition.countryCode to GmAddressSchemaDefinition,
        GnAddressSchemaDefinition.countryCode to GnAddressSchemaDefinition,
        GpAddressSchemaDefinition.countryCode to GpAddressSchemaDefinition,
        GqAddressSchemaDefinition.countryCode to GqAddressSchemaDefinition,
        GrAddressSchemaDefinition.countryCode to GrAddressSchemaDefinition,
        GsAddressSchemaDefinition.countryCode to GsAddressSchemaDefinition,
        GtAddressSchemaDefinition.countryCode to GtAddressSchemaDefinition,
        GuAddressSchemaDefinition.countryCode to GuAddressSchemaDefinition,
        GwAddressSchemaDefinition.countryCode to GwAddressSchemaDefinition,
        GyAddressSchemaDefinition.countryCode to GyAddressSchemaDefinition,
        HkAddressSchemaDefinition.countryCode to HkAddressSchemaDefinition,
        HnAddressSchemaDefinition.countryCode to HnAddressSchemaDefinition,
        HrAddressSchemaDefinition.countryCode to HrAddressSchemaDefinition,
        HtAddressSchemaDefinition.countryCode to HtAddressSchemaDefinition,
        HuAddressSchemaDefinition.countryCode to HuAddressSchemaDefinition,
        IdAddressSchemaDefinition.countryCode to IdAddressSchemaDefinition,
        IeAddressSchemaDefinition.countryCode to IeAddressSchemaDefinition,
        IlAddressSchemaDefinition.countryCode to IlAddressSchemaDefinition,
        ImAddressSchemaDefinition.countryCode to ImAddressSchemaDefinition,
        InAddressSchemaDefinition.countryCode to InAddressSchemaDefinition,
        IoAddressSchemaDefinition.countryCode to IoAddressSchemaDefinition,
        IqAddressSchemaDefinition.countryCode to IqAddressSchemaDefinition,
        IsAddressSchemaDefinition.countryCode to IsAddressSchemaDefinition,
        ItAddressSchemaDefinition.countryCode to ItAddressSchemaDefinition,
        JeAddressSchemaDefinition.countryCode to JeAddressSchemaDefinition,
        JmAddressSchemaDefinition.countryCode to JmAddressSchemaDefinition,
        JoAddressSchemaDefinition.countryCode to JoAddressSchemaDefinition,
        JpAddressSchemaDefinition.countryCode to JpAddressSchemaDefinition,
        KeAddressSchemaDefinition.countryCode to KeAddressSchemaDefinition,
        KgAddressSchemaDefinition.countryCode to KgAddressSchemaDefinition,
        KhAddressSchemaDefinition.countryCode to KhAddressSchemaDefinition,
        KiAddressSchemaDefinition.countryCode to KiAddressSchemaDefinition,
        KmAddressSchemaDefinition.countryCode to KmAddressSchemaDefinition,
        KnAddressSchemaDefinition.countryCode to KnAddressSchemaDefinition,
        KrAddressSchemaDefinition.countryCode to KrAddressSchemaDefinition,
        KwAddressSchemaDefinition.countryCode to KwAddressSchemaDefinition,
        KyAddressSchemaDefinition.countryCode to KyAddressSchemaDefinition,
        KzAddressSchemaDefinition.countryCode to KzAddressSchemaDefinition,
        LaAddressSchemaDefinition.countryCode to LaAddressSchemaDefinition,
        LbAddressSchemaDefinition.countryCode to LbAddressSchemaDefinition,
        LcAddressSchemaDefinition.countryCode to LcAddressSchemaDefinition,
        LiAddressSchemaDefinition.countryCode to LiAddressSchemaDefinition,
        LkAddressSchemaDefinition.countryCode to LkAddressSchemaDefinition,
        LrAddressSchemaDefinition.countryCode to LrAddressSchemaDefinition,
        LsAddressSchemaDefinition.countryCode to LsAddressSchemaDefinition,
        LtAddressSchemaDefinition.countryCode to LtAddressSchemaDefinition,
        LuAddressSchemaDefinition.countryCode to LuAddressSchemaDefinition,
        LvAddressSchemaDefinition.countryCode to LvAddressSchemaDefinition,
        LyAddressSchemaDefinition.countryCode to LyAddressSchemaDefinition,
        MaAddressSchemaDefinition.countryCode to MaAddressSchemaDefinition,
        McAddressSchemaDefinition.countryCode to McAddressSchemaDefinition,
        MdAddressSchemaDefinition.countryCode to MdAddressSchemaDefinition,
        MeAddressSchemaDefinition.countryCode to MeAddressSchemaDefinition,
        MfAddressSchemaDefinition.countryCode to MfAddressSchemaDefinition,
        MgAddressSchemaDefinition.countryCode to MgAddressSchemaDefinition,
        MkAddressSchemaDefinition.countryCode to MkAddressSchemaDefinition,
        MlAddressSchemaDefinition.countryCode to MlAddressSchemaDefinition,
        MmAddressSchemaDefinition.countryCode to MmAddressSchemaDefinition,
        MnAddressSchemaDefinition.countryCode to MnAddressSchemaDefinition,
        MoAddressSchemaDefinition.countryCode to MoAddressSchemaDefinition,
        MqAddressSchemaDefinition.countryCode to MqAddressSchemaDefinition,
        MrAddressSchemaDefinition.countryCode to MrAddressSchemaDefinition,
        MsAddressSchemaDefinition.countryCode to MsAddressSchemaDefinition,
        MtAddressSchemaDefinition.countryCode to MtAddressSchemaDefinition,
        MuAddressSchemaDefinition.countryCode to MuAddressSchemaDefinition,
        MvAddressSchemaDefinition.countryCode to MvAddressSchemaDefinition,
        MwAddressSchemaDefinition.countryCode to MwAddressSchemaDefinition,
        MxAddressSchemaDefinition.countryCode to MxAddressSchemaDefinition,
        MyAddressSchemaDefinition.countryCode to MyAddressSchemaDefinition,
        MzAddressSchemaDefinition.countryCode to MzAddressSchemaDefinition,
        NaAddressSchemaDefinition.countryCode to NaAddressSchemaDefinition,
        NcAddressSchemaDefinition.countryCode to NcAddressSchemaDefinition,
        NeAddressSchemaDefinition.countryCode to NeAddressSchemaDefinition,
        NgAddressSchemaDefinition.countryCode to NgAddressSchemaDefinition,
        NiAddressSchemaDefinition.countryCode to NiAddressSchemaDefinition,
        NlAddressSchemaDefinition.countryCode to NlAddressSchemaDefinition,
        NoAddressSchemaDefinition.countryCode to NoAddressSchemaDefinition,
        NpAddressSchemaDefinition.countryCode to NpAddressSchemaDefinition,
        NrAddressSchemaDefinition.countryCode to NrAddressSchemaDefinition,
        NuAddressSchemaDefinition.countryCode to NuAddressSchemaDefinition,
        NzAddressSchemaDefinition.countryCode to NzAddressSchemaDefinition,
        OmAddressSchemaDefinition.countryCode to OmAddressSchemaDefinition,
        PaAddressSchemaDefinition.countryCode to PaAddressSchemaDefinition,
        PeAddressSchemaDefinition.countryCode to PeAddressSchemaDefinition,
        PfAddressSchemaDefinition.countryCode to PfAddressSchemaDefinition,
        PgAddressSchemaDefinition.countryCode to PgAddressSchemaDefinition,
        PhAddressSchemaDefinition.countryCode to PhAddressSchemaDefinition,
        PkAddressSchemaDefinition.countryCode to PkAddressSchemaDefinition,
        PlAddressSchemaDefinition.countryCode to PlAddressSchemaDefinition,
        PmAddressSchemaDefinition.countryCode to PmAddressSchemaDefinition,
        PnAddressSchemaDefinition.countryCode to PnAddressSchemaDefinition,
        PrAddressSchemaDefinition.countryCode to PrAddressSchemaDefinition,
        PsAddressSchemaDefinition.countryCode to PsAddressSchemaDefinition,
        PtAddressSchemaDefinition.countryCode to PtAddressSchemaDefinition,
        PyAddressSchemaDefinition.countryCode to PyAddressSchemaDefinition,
        QaAddressSchemaDefinition.countryCode to QaAddressSchemaDefinition,
        ReAddressSchemaDefinition.countryCode to ReAddressSchemaDefinition,
        RoAddressSchemaDefinition.countryCode to RoAddressSchemaDefinition,
        RsAddressSchemaDefinition.countryCode to RsAddressSchemaDefinition,
        RuAddressSchemaDefinition.countryCode to RuAddressSchemaDefinition,
        RwAddressSchemaDefinition.countryCode to RwAddressSchemaDefinition,
        SaAddressSchemaDefinition.countryCode to SaAddressSchemaDefinition,
        SbAddressSchemaDefinition.countryCode to SbAddressSchemaDefinition,
        ScAddressSchemaDefinition.countryCode to ScAddressSchemaDefinition,
        SeAddressSchemaDefinition.countryCode to SeAddressSchemaDefinition,
        SgAddressSchemaDefinition.countryCode to SgAddressSchemaDefinition,
        ShAddressSchemaDefinition.countryCode to ShAddressSchemaDefinition,
        SiAddressSchemaDefinition.countryCode to SiAddressSchemaDefinition,
        SjAddressSchemaDefinition.countryCode to SjAddressSchemaDefinition,
        SkAddressSchemaDefinition.countryCode to SkAddressSchemaDefinition,
        SlAddressSchemaDefinition.countryCode to SlAddressSchemaDefinition,
        SmAddressSchemaDefinition.countryCode to SmAddressSchemaDefinition,
        SnAddressSchemaDefinition.countryCode to SnAddressSchemaDefinition,
        SoAddressSchemaDefinition.countryCode to SoAddressSchemaDefinition,
        SrAddressSchemaDefinition.countryCode to SrAddressSchemaDefinition,
        SsAddressSchemaDefinition.countryCode to SsAddressSchemaDefinition,
        StAddressSchemaDefinition.countryCode to StAddressSchemaDefinition,
        SvAddressSchemaDefinition.countryCode to SvAddressSchemaDefinition,
        SxAddressSchemaDefinition.countryCode to SxAddressSchemaDefinition,
        SzAddressSchemaDefinition.countryCode to SzAddressSchemaDefinition,
        TaAddressSchemaDefinition.countryCode to TaAddressSchemaDefinition,
        TcAddressSchemaDefinition.countryCode to TcAddressSchemaDefinition,
        TdAddressSchemaDefinition.countryCode to TdAddressSchemaDefinition,
        TfAddressSchemaDefinition.countryCode to TfAddressSchemaDefinition,
        TgAddressSchemaDefinition.countryCode to TgAddressSchemaDefinition,
        ThAddressSchemaDefinition.countryCode to ThAddressSchemaDefinition,
        TjAddressSchemaDefinition.countryCode to TjAddressSchemaDefinition,
        TkAddressSchemaDefinition.countryCode to TkAddressSchemaDefinition,
        TlAddressSchemaDefinition.countryCode to TlAddressSchemaDefinition,
        TmAddressSchemaDefinition.countryCode to TmAddressSchemaDefinition,
        TnAddressSchemaDefinition.countryCode to TnAddressSchemaDefinition,
        ToAddressSchemaDefinition.countryCode to ToAddressSchemaDefinition,
        TrAddressSchemaDefinition.countryCode to TrAddressSchemaDefinition,
        TtAddressSchemaDefinition.countryCode to TtAddressSchemaDefinition,
        TvAddressSchemaDefinition.countryCode to TvAddressSchemaDefinition,
        TwAddressSchemaDefinition.countryCode to TwAddressSchemaDefinition,
        TzAddressSchemaDefinition.countryCode to TzAddressSchemaDefinition,
        UaAddressSchemaDefinition.countryCode to UaAddressSchemaDefinition,
        UgAddressSchemaDefinition.countryCode to UgAddressSchemaDefinition,
        UsAddressSchemaDefinition.countryCode to UsAddressSchemaDefinition,
        UyAddressSchemaDefinition.countryCode to UyAddressSchemaDefinition,
        UzAddressSchemaDefinition.countryCode to UzAddressSchemaDefinition,
        VaAddressSchemaDefinition.countryCode to VaAddressSchemaDefinition,
        VcAddressSchemaDefinition.countryCode to VcAddressSchemaDefinition,
        VeAddressSchemaDefinition.countryCode to VeAddressSchemaDefinition,
        VgAddressSchemaDefinition.countryCode to VgAddressSchemaDefinition,
        VnAddressSchemaDefinition.countryCode to VnAddressSchemaDefinition,
        VuAddressSchemaDefinition.countryCode to VuAddressSchemaDefinition,
        WfAddressSchemaDefinition.countryCode to WfAddressSchemaDefinition,
        WsAddressSchemaDefinition.countryCode to WsAddressSchemaDefinition,
        XkAddressSchemaDefinition.countryCode to XkAddressSchemaDefinition,
        YeAddressSchemaDefinition.countryCode to YeAddressSchemaDefinition,
        YtAddressSchemaDefinition.countryCode to YtAddressSchemaDefinition,
        ZaAddressSchemaDefinition.countryCode to ZaAddressSchemaDefinition,
        ZmAddressSchemaDefinition.countryCode to ZmAddressSchemaDefinition,
        ZwAddressSchemaDefinition.countryCode to ZwAddressSchemaDefinition,
        ZzAddressSchemaDefinition.countryCode to ZzAddressSchemaDefinition,
    )

    fun get(countryCode: String?): List<CountryAddressSchema>? {
        return if (countryCode != null) {
            all[countryCode]?.schemaElements()
        } else {
            defaultSchema.schemaElements()
        }
    }
}
