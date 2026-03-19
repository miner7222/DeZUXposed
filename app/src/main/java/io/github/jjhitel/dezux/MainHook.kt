package io.github.jjhitel.dezux

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import io.github.jjhitel.dezux.R

class MainHook : XposedModule() {

    private val isInsideHeaderCheck = ThreadLocal<Boolean>()

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        super.onSystemServerStarting(param)
        applyGlobalHooks(param.classLoader)
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        super.onPackageLoaded(param)
        
        if (param.isFirstPackage) {
            applyGlobalHooks(param.defaultClassLoader)
         }

        when (param.packageName) {
            "com.android.settings" -> applyAppSettingsHooks(param.defaultClassLoader)
            "com.zui.game.service" -> applyGameServiceHooks(param.defaultClassLoader)
            "com.lenovo.ota", "com.lenovo.tbengine" -> applyOtaHooks(param.defaultClassLoader)
            "com.lenovo.levoice.caption" -> applyCaptionHooks(param.defaultClassLoader)
        }
    }

    private fun applyCaptionHooks(classLoader: ClassLoader) {
        /* TODO: Translate to libxposed API in Commit 3
        findClass("com.zui.translator.utils.MicrosoftApiKey").hook {
        */
    }

    private fun applyGlobalHooks(classLoader: ClassLoader) {
        val enabledFeatures = setOf(
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
            "ZuiHFWallpaper"
        )

        val disabledFeatures = setOf(
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
            "xiangbb"
        )

        /* TODO: Translate to libxposed API in Commit 3
        */

    }

    private fun applyAppSettingsHooks(classLoader: ClassLoader) {
        // Enable Multiple Space.
        /* TODO: Translate to libxposed API in Commit 3

        // Hide 'Google Play system update' preference.

        // Enable Google Services Preference.

        // Enable Restore Preinstalled Apps Preference.

    private fun applyGameServiceHooks(classLoader: ClassLoader) {
        /* TODO: Translate to libxposed API in Commit 3
        */
    }

    private fun applyOtaHooks(classLoader: ClassLoader) {
        /* TODO: Translate to libxposed API in Commit 3
        */
    }

}