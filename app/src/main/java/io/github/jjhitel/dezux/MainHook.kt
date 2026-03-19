package io.github.jjhitel.dezux

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedInterface.ExceptionMode
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
        try {
            val clazz = classLoader.loadClass("com.zui.translator.utils.MicrosoftApiKey")
            val method = clazz.getDeclaredMethod("getCountryCode")
            hook(method).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { _ ->
                "CN"
            }
        } catch (e: Exception) {
            // Ignore or log via libxposed log()
        }
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

        try {
            val clazz = classLoader.loadClass("com.lgsi.config.LgsiFeatures")
            val method = clazz.getDeclaredMethod("enabled", String::class.java)
            hook(method).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                val args = chain.args
                if (args.isEmpty()) return@intercept chain.proceed()
                
                val feature = args[0] as? String ?: return@intercept chain.proceed()
                when (feature) {
                    in enabledFeatures -> true
                    in disabledFeatures -> false
                    else -> chain.proceed()
                }
            }
        } catch (e: Exception) {}

    }

    private fun applyAppSettingsHooks(classLoader: ClassLoader) {
        // Enable Multiple Space.
        try {
            val clazz = classLoader.loadClass("com.lenovo.settings.applications.LenovoAppHeaderPreferenceController")
            val method = clazz.getDeclaredMethod("addMultiAppEntryIfSupported", List::class.java)
            hook(method).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                isInsideHeaderCheck.set(true)
                try {
                    chain.proceed()
                } finally {
                    isInsideHeaderCheck.set(false)
                }
            }
        } catch (e: Exception) {}

        // Hide 'Google Play system update' preference.
        try {
            val clazz1 = classLoader.loadClass("com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController")
            val method1 = clazz1.getDeclaredMethod("getAvailabilityStatus")
            hook(method1).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { _ -> 3 }
        } catch (e: Exception) {}

        try {
            val clazz2 = classLoader.loadClass("com.lenovo.settings.deviceinfo.controller.MainlineModuleVersionPreferenceController")
            val method2 = clazz2.getDeclaredMethod("getAvailabilityStatus")
            hook(method2).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { _ -> 3 }
        } catch (e: Exception) {}

        try {
            val clazz3 = classLoader.loadClass("com.lenovo.common.utils.LenovoUtils")
            val method3 = clazz3.getDeclaredMethod("isPrcVersion")
            hook(method3).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                if (isInsideHeaderCheck.get() == true) true else chain.proceed()
            }
        } catch (e: Exception) {}

        try {
            val clazz4 = classLoader.loadClass("com.lenovo.settings.applications.appclone.AppCloneUtils")
            val method4 = clazz4.getDeclaredMethod("supportsAppClone")
            hook(method4).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                if (isInsideHeaderCheck.get() == true) true else chain.proceed()
            }
        } catch (e: Exception) {}

        // Enable Google Services Preference.
        try {
            val clazz5 = classLoader.loadClass("com.lenovo.settings.applications.GoogleServicesPreferenceController")
            val method5 = clazz5.getDeclaredMethod("getAvailabilityStatus")
            hook(method5).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { _ -> 0 }
        } catch (e: Exception) {}

        // Enable Restore Preinstalled Apps Preference.
        try {
            val clazz6 = classLoader.loadClass("com.lenovo.settings.applications.preinstallrestore.RestorePreinstalledAppsPreferenceController")
            val method6 = clazz6.getDeclaredMethod("getAvailabilityStatus")
            hook(method6).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { _ -> 0 }
        } catch (e: Exception) {}

    private fun applyGameServiceHooks(classLoader: ClassLoader) {
        try {
            val clazz1 = classLoader.loadClass("com.zui.util.SystemPropertiesKt")
            val method1 = clazz1.getDeclaredMethod("getSystemProperty", String::class.java)
            hook(method1).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                val key = chain.args[0] as String
                if (key == "ro.config.lgsi.region") "prc" else chain.proceed()
            }
        } catch (e: Exception) {}

        try {
            val clazz2 = classLoader.loadClass("com.zui.game.service.FeatureKey\$Companion")
            val method2 = clazz2.getDeclaredMethod("createByKeys", Array<String>::class.java)
            hook(method2).setExceptionMode(ExceptionMode.PROTECTIVE).intercept { chain ->
                @Suppress("UNCHECKED_CAST")
                val keys = chain.args[0] as? Array<String>
                if (keys != null && (keys.contains("key_we_chat") || keys.contains("key_qq"))) {
                    val filteredKeys = keys.filter { it != "key_we_chat" && it != "key_qq" }.toTypedArray()
                    return@intercept chain.proceed(arrayOf(filteredKeys))
                }
                chain.proceed()
            }
        } catch (e: Exception) {}
    }

    private fun applyOtaHooks(classLoader: ClassLoader) {
        try {
            val clazz = classLoader.loadClass("android.os.SystemProperties")
            
            val hooker = io.github.libxposed.api.XposedInterface.Hooker { chain ->
                val key = chain.args[0] as String
                when (key) {
                    "ro.product.countrycode" -> "CN"
                    "ro.odm.lenovo.region" -> "prc"
                    "ro.config.zui.region" -> "PRC"
                    else -> chain.proceed()
                }
            }

            val method1 = clazz.getDeclaredMethod("get", String::class.java)
            hook(method1).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(hooker)

            val method2 = clazz.getDeclaredMethod("get", String::class.java, String::class.java)
            hook(method2).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(hooker)
        } catch (e: Exception) {}
    }

}