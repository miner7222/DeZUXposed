package io.github.miner7222.dezux

internal object HookConstants {
    const val TAG = "DeZUXHook"
    const val CTS_TAG = "DeZUXCtsHook"
    const val WALLPAPER_TAG = "DeZUXWallpaperHook"

    const val SETTINGS_PACKAGE = "com.android.settings"
    const val GALLERY_PACKAGE = "com.zui.gallery"
    const val LAUNCHER_PACKAGE = "com.zui.launcher"
    const val GAME_SERVICE_PACKAGE = "com.zui.game.service"
    const val WALLPAPER_SETTING_PACKAGE = "com.zui.wallpapersetting"
    const val OTA_PACKAGE = "com.lenovo.ota"
    const val TBENGINE_PACKAGE = "com.lenovo.tbengine"
    const val LEVOICE_CAPTION_PACKAGE = "com.lenovo.levoice.caption"

    const val WALLPAPER_SEARCH_IGNORED_RANK = 0x840
    const val WALLPAPER_SEARCH_INFO_XML_RES_ID = 0x7f140004
    const val ACTION_LAUNCH_CONTEXTUAL_SEARCH = "android.app.contextualsearch.action.LAUNCH_CONTEXTUAL_SEARCH"
    const val DEFAULT_CONTEXTUAL_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
    const val QUERY_INTENT_FLAGS = 0xC0000
    const val ACTION_GALLERY_VERIFICATION_RESPONSE = "gallery_verification_response"
    const val ACTION_PRIVACY_VERIFICATION = "com.lenovo.privacyspace.verification"
    const val EXTRA_REQUEST_MODE = "io.github.miner7222.dezux.PRIVACY_REQUEST_MODE"
    const val KEY_FILES_TO_ADD_PRIVACY_GROUP = "filesToAddToPrivacyGroup"
    const val REQUEST_MODE_ADD = "add"
    const val REQUEST_MODE_OPEN = "open"
    const val REQUEST_CONFIRM_CREDENTIAL = 0x22B8
    const val RECEIVER_EXPORTED = 0x2
    const val RECEIVER_NOT_EXPORTED = 0x4

    val ENABLED_FEATURES = setOf(
        "DynamicFeature",
        "GoogleConnectivityResOverlay",
        "GoogleNetworkStackOverlay",
        "GoogleWifiResOverlay",
        "GrantEMMPartner",
        "LeVision",
        "LegionSpace",
        "LgsiCCS",
        "LgsiEnableMultiUser",
        "ZUIEpos",
        "ZUILenovoLevision",
        "ZUISetupWizardExtROW",
        "ZuiAICloudOnlyRow",
        "ZuiDefaultAppRow",
        "ZuiHFWallpaper",
    )

    val DISABLED_FEATURES = setOf(
        "BaiduNetworkLocation",
        "ConnectivityResOverlay",
        "DataInterceptor",
        "DataMonitor",
        "DeviceIdService",
        "DeviceTrafficLimit",
        "DnsOptimization",
        "KeyboardShortcutInput",
        "KuwoPlayerHD",
        "LeVoiceAgent",
        "LegacyAppRestrict",
        "LenovoStore",
        "LenovoXiaoTian",
        "LFHTianjiaoTablet",
        "LgsiDisableMultiUser",
        "LongPressPowerStartLevoice",
        "NetworkStackOverlay",
        "NudOptimization",
        "SafeUrl",
        "StaticIpEnhancement",
        "Touchnotes_HD",
        "VideoPlayingMode",
        "WebLimit",
        "WifiDiagIndication",
        "WifiResOverlay",
        "XiaotianTrigger",
        "XtsMusic",
        "XuiEasySync",
        "ZUISetupWizardExtPRC",
        "ZuiAIAssistant",
        "ZuiAICloudOnlyPrc",
        "ZuiAILens",
        "ZuiAILocalLlmPrc",
        "ZuiAntiCrossSell",
        "ZuiAppFreeze",
        "ZuiAutorunManager",
        "ZuiCalculator",
        "ZuiCalendar",
        "ZuiContacts",
        "ZuiCore",
        "ZuiCoreService",
        "ZuiDefaultAppPrc",
        "ZuiDefaultAppPrcNewBrowser",
        "ZuiFaceAuth",
        "ZuiFileManager",
        "ZuiGalleryWallpaperCropper",
        "ZuiLauncherSDK",
        "ZuiLenovoLevisionProvider",
        "ZuiLongWakelockStandbyControl",
        "ZuiMagicVoice",
        "ZuiMediaUsage",
        "ZuiNewBrowser",
        "ZuiNightPowerSave",
        "ZuiOMS",
        "ZuiPackageInstaller",
        "ZuiPermission",
        "ZuiPermissionController",
        "ZuiPrivacyShield",
        "ZuiRescueParty",
        "ZuiSecurity",
        "ZuiServiceEngine",
        "ZuiSingleBuffer",
        "ZuiSkipAiLens",
        "ZuiTianJiaoMode",
        "ZuiXlog",
        "ZuxWifiService",
        "xiangbb",
    )

    val WALLPAPER_STATIC_ARRAY_NAMES = setOf("wallpapers", "wallpapers_pandaer")
    val WALLPAPER_CHARGE_STYLE_ARRAY_NAMES = setOf(
        "chargeStyle",
        "chargeStyle_row",
        "chargeStyle_pandaer",
    )
    val WALLPAPER_CHARGE_STYLE_NAME_ARRAY_NAMES = setOf(
        "chargeStyleName",
        "chargeStyleName_row",
        "chargeStyleName_pandaer",
    )
    val WALLPAPER_STATIC_DEFAULT_WITH_PANDAER_EXTRAS = arrayOf(
        "wallpaper_000",
        "wallpaper_001",
        "wallpaper_002",
        "wallpaper_003",
        "wallpaper_004",
        "wallpaper_005",
        "wallpaper_006",
        "wallpaper_007",
        "wallpaper_008",
        "wallpaper_009",
        "wallpaper_010",
        "wallpaper_011",
        "wallpaper_012",
        "wallpaper_013",
        "wallpaper_014",
        "wallpaper_015",
        "wallpaper_019",
        "wallpaper_020",
    )
    val WALLPAPER_CHARGE_STYLE_PANDAER_KEYS = arrayOf(
        "charge_style_pandaer",
        "charge_style_default",
        "charge_style_girl",
        "charge_style_triangle",
        "charge_style_turbo",
    )
    val WALLPAPER_CHARGE_STYLE_NAME_RESOURCE_NAMES = arrayOf(
        "charge_style_pandaer",
        "charge_style_default",
        "charge_style_girl",
        "charge_style_triangle",
        "charge_style_turbo",
    )
    val PRIVACY_ADD_CALLERS = setOf(
        "com.zui.gallery.main.utils.MenuExecutorUtils",
        "com.zui.gallery.app.AlbumPage",
        "com.zui.gallery.app.PhotoPage",
        "com.zui.gallery.main.ui.view.PhotoPageActionView",
        "com.zui.gallery.app.localtime.LocalTimeAlbumPage",
    )
    val PRIVACY_OPEN_CALLERS = setOf(
        "com.zui.gallery.app.AlbumSetPage",
        "com.zui.gallery.banner.BaseActivity",
        "com.zui.gallery.banner.PrivacyBaseActivity",
    )
}
