/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.gradle.plugin.checklicenses

@Suppress("ktlint:enum-entry-name-case", "EnumNaming")
enum class SPDXLicense(val csvName: String) {
    _0BSD("0BSD"),
    AAL("AAL"),
    ABSTYLES("Abstyles"),
    ADOBE_2006("Adobe-2006"),
    ADOBE_GLYPH("Adobe-Glyph"),
    ADSL("ADSL"),
    AFL_1_1("AFL-1.1"),
    AFL_1_2("AFL-1.2"),
    AFL_2_0("AFL-2.0"),
    AFL_2_1("AFL-2.1"),
    AFL_3_0("AFL-3.0"),
    AFMPARSE("Afmparse"),
    AGPL_1_0_ONLY("AGPL-1.0-only"),
    AGPL_1_0_OR_LATER("AGPL-1.0-or-later"),
    AGPL_3_0_ONLY("AGPL-3.0-only"),
    AGPL_3_0_OR_LATER("AGPL-3.0-or-later"),
    ALADDIN("Aladdin"),
    AMDPLPA("AMDPLPA"),
    AML("AML"),
    AMPAS("AMPAS"),
    ANTLR_PD("ANTLR-PD"),
    APACHE_1_0("Apache-1.0"),
    APACHE_1_1("Apache-1.1"),
    APACHE_2_0("Apache-2.0"),
    APAFML("APAFML"),
    APL_1_0("APL-1.0"),
    APSL_1_0("APSL-1.0"),
    APSL_1_1("APSL-1.1"),
    APSL_1_2("APSL-1.2"),
    APSL_2_0("APSL-2.0"),
    ARTISTIC_1_0("Artistic-1.0"),
    ARTISTIC_1_0_CL8("Artistic-1.0-cl8"),
    ARTISTIC_1_0_PERL("Artistic-1.0-Perl"),
    ARTISTIC_2_0("Artistic-2.0"),
    BAHYPH("Bahyph"),
    BARR("Barr"),
    BEERWARE("Beerware"),
    BITTORRENT_1_0("BitTorrent-1.0"),
    BITTORRENT_1_1("BitTorrent-1.1"),
    BLESSING("blessing"),
    BLUEOAK_1_0_0("BlueOak-1.0.0"),
    BORCEUX("Borceux"),
    BSD_1_CLAUSE("BSD-1-Clause"),
    BSD_2_CLAUSE("BSD-2-Clause"),
    BSD_2_CLAUSE_FREEBSD("BSD-2-Clause-FreeBSD"),
    BSD_2_CLAUSE_NETBSD("BSD-2-Clause-NetBSD"),
    BSD_2_CLAUSE_PATENT("BSD-2-Clause-Patent"),
    BSD_3_CLAUSE("BSD-3-Clause"),
    BSD_3_CLAUSE_ATTRIBUTION("BSD-3-Clause-Attribution"),
    BSD_3_CLAUSE_CLEAR("BSD-3-Clause-Clear"),
    BSD_3_CLAUSE_LBNL("BSD-3-Clause-LBNL"),
    BSD_3_CLAUSE_NO_NUCLEAR_LICENSE("BSD-3-Clause-No-Nuclear-License"),
    BSD_3_CLAUSE_NO_NUCLEAR_LICENSE_2014("BSD-3-Clause-No-Nuclear-License-2014"),
    BSD_3_CLAUSE_NO_NUCLEAR_WARRANTY("BSD-3-Clause-No-Nuclear-Warranty"),
    BSD_3_CLAUSE_OPEN_MPI("BSD-3-Clause-Open-MPI"),
    BSD_4_CLAUSE("BSD-4-Clause"),
    BSD_4_CLAUSE_UC("BSD-4-Clause-UC"),
    BSD_PROTECTION("BSD-Protection"),
    BSD_SOURCE_CODE("BSD-Source-Code"),
    BSL_1_0("BSL-1.0"),
    BOUNCY_CASTLE("BouncyCastle-License"),
    BZIP2_1_0_5("bzip2-1.0.5"),
    BZIP2_1_0_6("bzip2-1.0.6"),
    CALDERA("Caldera"),
    CATOSL_1_1("CATOSL-1.1"),
    CC_BY_1_0("CC-BY-1.0"),
    CC_BY_2_0("CC-BY-2.0"),
    CC_BY_2_5("CC-BY-2.5"),
    CC_BY_3_0("CC-BY-3.0"),
    CC_BY_4_0("CC-BY-4.0"),
    CC_BY_NC_1_0("CC-BY-NC-1.0"),
    CC_BY_NC_2_0("CC-BY-NC-2.0"),
    CC_BY_NC_2_5("CC-BY-NC-2.5"),
    CC_BY_NC_3_0("CC-BY-NC-3.0"),
    CC_BY_NC_4_0("CC-BY-NC-4.0"),
    CC_BY_NC_ND_1_0("CC-BY-NC-ND-1.0"),
    CC_BY_NC_ND_2_0("CC-BY-NC-ND-2.0"),
    CC_BY_NC_ND_2_5("CC-BY-NC-ND-2.5"),
    CC_BY_NC_ND_3_0("CC-BY-NC-ND-3.0"),
    CC_BY_NC_ND_4_0("CC-BY-NC-ND-4.0"),
    CC_BY_NC_SA_1_0("CC-BY-NC-SA-1.0"),
    CC_BY_NC_SA_2_0("CC-BY-NC-SA-2.0"),
    CC_BY_NC_SA_2_5("CC-BY-NC-SA-2.5"),
    CC_BY_NC_SA_3_0("CC-BY-NC-SA-3.0"),
    CC_BY_NC_SA_4_0("CC-BY-NC-SA-4.0"),
    CC_BY_ND_1_0("CC-BY-ND-1.0"),
    CC_BY_ND_2_0("CC-BY-ND-2.0"),
    CC_BY_ND_2_5("CC-BY-ND-2.5"),
    CC_BY_ND_3_0("CC-BY-ND-3.0"),
    CC_BY_ND_4_0("CC-BY-ND-4.0"),
    CC_BY_SA_1_0("CC-BY-SA-1.0"),
    CC_BY_SA_2_0("CC-BY-SA-2.0"),
    CC_BY_SA_2_5("CC-BY-SA-2.5"),
    CC_BY_SA_3_0("CC-BY-SA-3.0"),
    CC_BY_SA_4_0("CC-BY-SA-4.0"),
    CC_PDDC("CC-PDDC"),
    CC0_1_0("CC0-1.0"),
    CDDL_1_0("CDDL-1.0"),
    CDDL_1_1("CDDL-1.1"),
    CDLA_PERMISSIVE_1_0("CDLA-Permissive-1.0"),
    CDLA_SHARING_1_0("CDLA-Sharing-1.0"),
    CECILL_1_0("CECILL-1.0"),
    CECILL_1_1("CECILL-1.1"),
    CECILL_2_0("CECILL-2.0"),
    CECILL_2_1("CECILL-2.1"),
    CECILL_B("CECILL-B"),
    CECILL_C("CECILL-C"),
    CERN_OHL_1_1("CERN-OHL-1.1"),
    CERN_OHL_1_2("CERN-OHL-1.2"),
    CLARTISTIC("ClArtistic"),
    CNRI_JYTHON("CNRI-Jython"),
    CNRI_PYTHON("CNRI-Python"),
    CNRI_PYTHON_GPL_COMPATIBLE("CNRI-Python-GPL-Compatible"),
    CONDOR_1_1("Condor-1.1"),
    COPYLEFT_NEXT_0_3_0("copyleft-next-0.3.0"),
    COPYLEFT_NEXT_0_3_1("copyleft-next-0.3.1"),
    CPAL_1_0("CPAL-1.0"),
    CPL_1_0("CPL-1.0"),
    CPOL_1_02("CPOL-1.02"),
    CROSSWORD("Crossword"),
    CRYSTALSTACKER("CrystalStacker"),
    CUA_OPL_1_0("CUA-OPL-1.0"),
    CUBE("Cube"),
    CURL("curl"),
    D_FSL_1_0("D-FSL-1.0"),
    DIFFMARK("diffmark"),
    DOC("DOC"),
    DOTSEQN("Dotseqn"),
    DSDP("DSDP"),
    DVIPDFM("dvipdfm"),
    ECL_1_0("ECL-1.0"),
    ECL_2_0("ECL-2.0"),
    EFL_1_0("EFL-1.0"),
    EFL_2_0("EFL-2.0"),
    EGENIX("eGenix"),
    ENTESSA("Entessa"),
    EPL_1_0("EPL-1.0"),
    EPL_2_0("EPL-2.0"),
    ERLPL_1_1("ErlPL-1.1"),
    ETALAB_2_0("etalab-2.0"),
    EUDATAGRID("EUDatagrid"),
    EUPL_1_0("EUPL-1.0"),
    EUPL_1_1("EUPL-1.1"),
    EUPL_1_2("EUPL-1.2"),
    EUROSYM("Eurosym"),
    FAIR("Fair"),
    FRAMEWORX_1_0("Frameworx-1.0"),
    FREEIMAGE("FreeImage"),
    FSFAP("FSFAP"),
    FSFUL("FSFUL"),
    FSFULLR("FSFULLR"),
    FTL("FTL"),
    GFDL_1_1_ONLY("GFDL-1.1-only"),
    GFDL_1_1_OR_LATER("GFDL-1.1-or-later"),
    GFDL_1_2_ONLY("GFDL-1.2-only"),
    GFDL_1_2_OR_LATER("GFDL-1.2-or-later"),
    GFDL_1_3_ONLY("GFDL-1.3-only"),
    GFDL_1_3_OR_LATER("GFDL-1.3-or-later"),
    GIFTWARE("Giftware"),
    GL2PS("GL2PS"),
    GLIDE("Glide"),
    GLULXE("Glulxe"),
    GNUPLOT("gnuplot"),
    GPL_1_0_ONLY("GPL-1.0-only"),
    GPL_1_0_OR_LATER("GPL-1.0-or-later"),
    GPL_2_0_ONLY("GPL-2.0-only"),
    GPL_2_0_OR_LATER("GPL-2.0-or-later"),
    GPL_3_0_ONLY("GPL-3.0-only"),
    GPL_3_0_OR_LATER("GPL-3.0-or-later"),
    GSOAP_1_3B("gSOAP-1.3b"),
    HASKELLREPORT("HaskellReport"),
    HPND("HPND"),
    HPND_SELL_VARIANT("HPND-sell-variant"),
    IBM_PIBS("IBM-pibs"),
    ICU("ICU"),
    IJG("IJG"),
    IMAGEMAGICK("ImageMagick"),
    IMATIX("iMatix"),
    IMLIB2("Imlib2"),
    INFO_ZIP("Info-ZIP"),
    INTEL("Intel"),
    INTEL_ACPI("Intel-ACPI"),
    INTERBASE_1_0("Interbase-1.0"),
    IPA("IPA"),
    IPL_1_0("IPL-1.0"),
    ISC("ISC"),
    JASPER_2_0("JasPer-2.0"),
    JPNIC("JPNIC"),
    JSON("JSON"),
    LAL_1_2("LAL-1.2"),
    LAL_1_3("LAL-1.3"),
    LATEX2E("Latex2e"),
    LEPTONICA("Leptonica"),
    LGPL_2_0_ONLY("LGPL-2.0-only"),
    LGPL_2_0_OR_LATER("LGPL-2.0-or-later"),
    LGPL_2_1_ONLY("LGPL-2.1-only"),
    LGPL_2_1_OR_LATER("LGPL-2.1-or-later"),
    LGPL_3_0_ONLY("LGPL-3.0-only"),
    LGPL_3_0_OR_LATER("LGPL-3.0-or-later"),
    LGPLLR("LGPLLR"),
    LIBPNG("Libpng"),
    LIBPNG_2_0("libpng-2.0"),
    LIBTIFF("libtiff"),
    LILIQ_P_1_1("LiLiQ-P-1.1"),
    LILIQ_R_1_1("LiLiQ-R-1.1"),
    LILIQ_RPLUS_1_1("LiLiQ-Rplus-1.1"),
    LINUX_OPENIB("Linux-OpenIB"),
    LPL_1_0("LPL-1.0"),
    LPL_1_02("LPL-1.02"),
    LPPL_1_0("LPPL-1.0"),
    LPPL_1_1("LPPL-1.1"),
    LPPL_1_2("LPPL-1.2"),
    LPPL_1_3A("LPPL-1.3a"),
    LPPL_1_3C("LPPL-1.3c"),
    MAKEINDEX("MakeIndex"),
    MIROS("MirOS"),
    MIT("MIT"),
    MIT_0("MIT-0"),
    MIT_ADVERTISING("MIT-advertising"),
    MIT_CMU("MIT-CMU"),
    MIT_ENNA("MIT-enna"),
    MIT_FEH("MIT-feh"),
    MITNFA("MITNFA"),
    MOTOSOTO("Motosoto"),
    MPICH2("mpich2"),
    MPL_1_0("MPL-1.0"),
    MPL_1_1("MPL-1.1"),
    MPL_2_0("MPL-2.0"),
    MPL_2_0_NO_COPYLEFT_EXCEPTION("MPL-2.0-no-copyleft-exception"),
    MS_PL("MS-PL"),
    MS_RL("MS-RL"),
    MTLL("MTLL"),
    MULANPSL_1_0("MulanPSL-1.0"),
    MULTICS("Multics"),
    MUP("Mup"),
    NASA_1_3("NASA-1.3"),
    NAUMEN("Naumen"),
    NBPL_1_0("NBPL-1.0"),
    NCSA("NCSA"),
    NET_SNMP("Net-SNMP"),
    NETCDF("NetCDF"),
    NEWSLETR("Newsletr"),
    NGPL("NGPL"),
    NLOD_1_0("NLOD-1.0"),
    NLPL("NLPL"),
    NOKIA("Nokia"),
    NOSL("NOSL"),
    NOWEB("Noweb"),
    NPL_1_0("NPL-1.0"),
    NPL_1_1("NPL-1.1"),
    NPOSL_3_0("NPOSL-3.0"),
    NRL("NRL"),
    NTP("NTP"),
    OCCT_PL("OCCT-PL"),
    OCLC_2_0("OCLC-2.0"),
    ODBL_1_0("ODbL-1.0"),
    ODC_BY_1_0("ODC-By-1.0"),
    OFL_1_0("OFL-1.0"),
    OFL_1_1("OFL-1.1"),
    OGL_CANADA_2_0("OGL-Canada-2.0"),
    OGL_UK_1_0("OGL-UK-1.0"),
    OGL_UK_2_0("OGL-UK-2.0"),
    OGL_UK_3_0("OGL-UK-3.0"),
    OGTSL("OGTSL"),
    OLDAP_1_1("OLDAP-1.1"),
    OLDAP_1_2("OLDAP-1.2"),
    OLDAP_1_3("OLDAP-1.3"),
    OLDAP_1_4("OLDAP-1.4"),
    OLDAP_2_0("OLDAP-2.0"),
    OLDAP_2_0_1("OLDAP-2.0.1"),
    OLDAP_2_1("OLDAP-2.1"),
    OLDAP_2_2("OLDAP-2.2"),
    OLDAP_2_2_1("OLDAP-2.2.1"),
    OLDAP_2_2_2("OLDAP-2.2.2"),
    OLDAP_2_3("OLDAP-2.3"),
    OLDAP_2_4("OLDAP-2.4"),
    OLDAP_2_5("OLDAP-2.5"),
    OLDAP_2_6("OLDAP-2.6"),
    OLDAP_2_7("OLDAP-2.7"),
    OLDAP_2_8("OLDAP-2.8"),
    OML("OML"),
    OPENSSL("OpenSSL"),
    OPL_1_0("OPL-1.0"),
    OSET_PL_2_1("OSET-PL-2.1"),
    OSL_1_0("OSL-1.0"),
    OSL_1_1("OSL-1.1"),
    OSL_2_0("OSL-2.0"),
    OSL_2_1("OSL-2.1"),
    OSL_3_0("OSL-3.0"),
    PARITY_6_0_0("Parity-6.0.0"),
    PDDL_1_0("PDDL-1.0"),
    PHP_3_0("PHP-3.0"),
    PHP_3_01("PHP-3.01"),
    PLEXUS("Plexus"),
    POSTGRESQL("PostgreSQL"),
    PSFRAG("psfrag"),
    PSUTILS("psutils"),
    PUBLIC_DOMAIN("Public Domain"),
    PYTHON_2_0("Python-2.0"),
    QHULL("Qhull"),
    QPL_1_0("QPL-1.0"),
    RDISC("Rdisc"),
    RHECOS_1_1("RHeCos-1.1"),
    RPL_1_1("RPL-1.1"),
    RPL_1_5("RPL-1.5"),
    RPSL_1_0("RPSL-1.0"),
    RSA_MD("RSA-MD"),
    RSCPL("RSCPL"),
    RUBY("Ruby"),
    SAX_PD("SAX-PD"),
    SAXPATH("Saxpath"),
    SCEA("SCEA"),
    SENDMAIL("Sendmail"),
    SENDMAIL_8_23("Sendmail-8.23"),
    SGI_B_1_0("SGI-B-1.0"),
    SGI_B_1_1("SGI-B-1.1"),
    SGI_B_2_0("SGI-B-2.0"),
    SHL_0_5("SHL-0.5"),
    SHL_0_51("SHL-0.51"),
    SIMPL_2_0("SimPL-2.0"),
    SISSL("SISSL"),
    SISSL_1_2("SISSL-1.2"),
    SLEEPYCAT("Sleepycat"),
    SMLNJ("SMLNJ"),
    SMPPL("SMPPL"),
    SNIA("SNIA"),
    SPENCER_86("Spencer-86"),
    SPENCER_94("Spencer-94"),
    SPENCER_99("Spencer-99"),
    SPL_1_0("SPL-1.0"),
    SSH_OPENSSH("SSH-OpenSSH"),
    SSH_SHORT("SSH-short"),
    SSPL_1_0("SSPL-1.0"),
    SUGARCRM_1_1_3("SugarCRM-1.1.3"),
    SWL("SWL"),
    TAPR_OHL_1_0("TAPR-OHL-1.0"),
    TCL("TCL"),
    TCP_WRAPPERS("TCP-wrappers"),
    TMATE("TMate"),
    TORQUE_1_1("TORQUE-1.1"),
    TOSL("TOSL"),
    TU_BERLIN_1_0("TU-Berlin-1.0"),
    TU_BERLIN_2_0("TU-Berlin-2.0"),
    UCL_1_0("UCL-1.0"),
    UNICODE_DFS_2015("Unicode-DFS-2015"),
    UNICODE_DFS_2016("Unicode-DFS-2016"),
    UNICODE_TOU("Unicode-TOU"),
    UNLICENSE("Unlicense"),
    UPL_1_0("UPL-1.0"),
    VIM("Vim"),
    VOSTROM("VOSTROM"),
    VSL_1_0("VSL-1.0"),
    W3C("W3C"),
    W3C_19980720("W3C-19980720"),
    W3C_20150513("W3C-20150513"),
    WATCOM_1_0("Watcom-1.0"),
    WSUIPA("Wsuipa"),
    WTFPL("WTFPL"),
    X11("X11"),
    XEROX("Xerox"),
    XFREE86_1_1("XFree86-1.1"),
    XINETD("xinetd"),
    XNET("Xnet"),
    XPP("xpp"),
    XSKAT("XSkat"),
    YPL_1_0("YPL-1.0"),
    YPL_1_1("YPL-1.1"),
    ZED("Zed"),
    ZEND_2_0("Zend-2.0"),
    ZIMBRA_1_3("Zimbra-1.3"),
    ZIMBRA_1_4("Zimbra-1.4"),
    ZLIB("Zlib"),
    ZLIB_ACKNOWLEDGEMENT("zlib-acknowledgement"),
    ZPL_1_1("ZPL-1.1"),
    ZPL_2_0("ZPL-2.0"),
    ZPL_2_1("ZPL-2.1")
}
