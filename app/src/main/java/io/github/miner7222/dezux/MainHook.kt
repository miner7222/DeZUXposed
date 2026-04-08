package io.github.miner7222.dezux

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
class MainHook : IYukiHookXposedInit {

    private val isInsideHeaderCheck = ThreadLocal<Boolean>()

    override fun onInit() = configs {
        isDebug = false
    }

    override fun onHook() = encase {
        loadSystem {
            applyGlobalHooks()
        }

        loadApp {
            applyGlobalHooks()
        }

        loadApp("com.android.settings") {
            applyAppSettingsHooks()
        }

        loadApp("com.zui.gallery") {
            applyGalleryHooks()
        }

        loadApp("com.zui.game.service") {
            applyGameServiceHooks()
        }

		loadApp("com.lenovo.ota") {
            applyOtaHooks()
        }

		loadApp("com.lenovo.tbengine") {
            applyOtaHooks()
        }

        loadApp("com.lenovo.levoice.caption") {
            findClass("com.zui.translator.utils.MicrosoftApiKey").hook {
                injectMember {
                    method {
                        name = "getCountryCode"
                        emptyParam()
                    }
                    replaceAny { "CN" }
                }
            }
        }

    }

    private fun PackageParam.applyGalleryHooks() {
        hookGalleryVerificationRequestTagging()
        hookMainActivityPrivacyResult()

        findClass("android.content.ContextWrapper").hook {
            injectMember {
                method {
                    name = "registerReceiver"
                    param(BroadcastReceiver::class.java, IntentFilter::class.java, IntType)
                }
                beforeHook {
                    if (args.size < 3) return@beforeHook

                    val filter = args[1] as? IntentFilter ?: return@beforeHook
                    if (!filterContainsAction(filter, ACTION_GALLERY_VERIFICATION_RESPONSE)) return@beforeHook

                    val flags = args[2] as? Int ?: return@beforeHook
                    if ((flags and RECEIVER_NOT_EXPORTED) == 0) return@beforeHook

                    val newFlags = (flags and RECEIVER_NOT_EXPORTED.inv()) or RECEIVER_EXPORTED
                    args[2] = newFlags
                    Log.i(TAG, "Changed Gallery privacy receiver flags from $flags to $newFlags")
                }
            }
        }
    }

    private fun PackageParam.hookGalleryVerificationRequestTagging() {
        findClass("android.app.Activity").hook {
            injectMember {
                method {
                    name = "startActivityForResult"
                    param(Intent::class.java, IntType)
                }
                beforeHook {
                    tagGalleryVerificationIntent(args[0] as? Intent)
                }
            }
            injectMember {
                method {
                    name = "startActivityForResult"
                    param(Intent::class.java, IntType, Bundle::class.java)
                }
                beforeHook {
                    tagGalleryVerificationIntent(args[0] as? Intent)
                }
            }
        }
    }

    private fun PackageParam.hookMainActivityPrivacyResult() {
        val galleryUtilsClass = "com.zui.gallery.util.GalleryUtils".toClass()
        val mainActivityClass = "com.zui.gallery.main.ui.activity.MainActivity".toClass()
        val mediaItemClass = "com.zui.gallery.data.MediaItem".toClass()
        val menuExecutorClass = "com.zui.gallery.main.utils.MenuExecutorUtils".toClass()

        "com.zui.gallery.main.ui.activity.MainActivity".hook {
            injectMember {
                method {
                    name = "onActivityResult"
                    param(IntType, IntType, Intent::class.java)
                }
                afterHook {
                    val requestCode = args[0] as? Int ?: return@afterHook
                    val resultCode = args[1] as? Int ?: return@afterHook
                    if (requestCode != REQUEST_CONFIRM_CREDENTIAL || resultCode != Activity.RESULT_OK) return@afterHook

                    val pendingObject = galleryUtilsClass
                        .getDeclaredMethod("takeMemCache", String::class.java)
                        .invoke(null, KEY_FILES_TO_ADD_PRIVACY_GROUP)
                        ?: return@afterHook

                    val menuExecutor = mainActivityClass
                        .getDeclaredMethod("getMenuExecutor")
                        .invoke(instance)
                        ?: return@afterHook

                    when {
                        mediaItemClass.isInstance(pendingObject) -> {
                            menuExecutorClass
                                .getDeclaredMethod("addItemToPrivacyFirst", mediaItemClass)
                                .invoke(menuExecutor, pendingObject)
                            Log.i(TAG, "MainActivity privacy add triggered for single item")
                        }

                        pendingObject is List<*> -> {
                            menuExecutorClass
                                .getDeclaredMethod("addItemsToPrivacyFirst", List::class.java)
                                .invoke(menuExecutor, pendingObject)
                            val addItemsField = mainActivityClass.getDeclaredField("addItemsToPrivacyFirst")
                            addItemsField.isAccessible = true
                            addItemsField.setBoolean(null, true)
                            Log.i(TAG, "MainActivity privacy add triggered for list")
                        }
                    }
                }
            }
        }
    }

    private fun PackageParam.applyGlobalHooks() {
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

        findClass("com.lgsi.config.LgsiFeatures").hook {
            injectMember {
                method {
                    name = "enabled"
                    param(StringClass)
                }
                replaceAny {
                    if (args.isEmpty()) return@replaceAny callOriginal()
                    val feature = args[0] as? String ?: return@replaceAny callOriginal()

                    when (feature) {
                        in enabledFeatures -> return@replaceAny true
                        in disabledFeatures -> return@replaceAny false
                        else -> return@replaceAny callOriginal()
                    }
                }
            }
        }

    }

    private fun PackageParam.applyAppSettingsHooks() {
        // Enable Multiple Space.
        findClass("com.lenovo.settings.applications.LenovoAppHeaderPreferenceController").hook {
            injectMember {
                method {
                    name = "addMultiAppEntryIfSupported"
                    param(List::class.java)
                }
                replaceUnit {
                    isInsideHeaderCheck.set(true)
                    try {
                        callOriginal()
                    } finally {
                        isInsideHeaderCheck.set(false)
                    }
                }
            }
        }

        // Hide 'Google Play system update' preference.
        findClass("com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController").hook {
            injectMember {
                method {
                    name = "getAvailabilityStatus"
                    emptyParam()
                }
                replaceAny { 3 }
            }
        }

        findClass("com.lenovo.settings.deviceinfo.controller.MainlineModuleVersionPreferenceController").hook {
            injectMember {
                method {
                    name = "getAvailabilityStatus"
                    emptyParam()
                }
                replaceAny { 3 }
            }
        }

        findClass("com.lenovo.common.utils.LenovoUtils").hook {
            injectMember {
                method {
                    name = "isPrcVersion"
                    emptyParam()
                }
                replaceAny {
                    if (isInsideHeaderCheck.get() == true) {
                        return@replaceAny true
                    }
                    return@replaceAny callOriginal()
                }
            }
        }

        findClass("com.lenovo.settings.applications.appclone.AppCloneUtils").hook {
            injectMember {
                method {
                    name = "supportsAppClone"
                    emptyParam()
                }
                replaceAny {
                    if (isInsideHeaderCheck.get() == true) {
                        return@replaceAny true
                    }
                    return@replaceAny callOriginal()
                }
            }
        }

        // Enable Google Services Preference.
        findClass("com.lenovo.settings.applications.GoogleServicesPreferenceController").hook {
            injectMember {
                method {
                    name = "getAvailabilityStatus"
                    emptyParam()
                }
                replaceAny { 0 }
            }
        }

        // Enable Restore Preinstalled Apps Preference.
		findClass("com.lenovo.settings.applications.preinstallrestore.RestorePreinstalledAppsPreferenceController").hook {
            injectMember {
                method {
                    name = "getAvailabilityStatus"
                    emptyParam()
                }
                replaceAny { 0 }
            }
        }

    }

    private fun PackageParam.applyGameServiceHooks() {
        findClass("com.zui.util.SystemPropertiesKt").hook {
            injectMember {
                method {
                    name = "getSystemProperty"
                    param(StringClass)
                }
                replaceAny {
                    val key = args[0] as String
                    if (key == "ro.config.lgsi.region") {
                        // Hook to prc to maintain full Game Helper functionality.
                        return@replaceAny "prc"
                    }
                    return@replaceAny callOriginal()
                }
            }
        }
        findClass("com.zui.game.service.FeatureKey\$Companion").hook {
            injectMember {
                method {
                    name = "createByKeys"
                    param(Array<String>::class.java)
                }
                beforeHook {
                    val keys = args[0] as? Array<String>
                    // Remove features considered as Chinese bloatware.
                    if (keys != null && (keys.contains("key_we_chat") || keys.contains("key_qq"))) {
                        args[0] = keys.filter { it != "key_we_chat" && it != "key_qq" }.toTypedArray()
                    }
                }
            }
        }
    }

	private fun PackageParam.applyOtaHooks() {
        findClass("android.os.SystemProperties").hook {
            injectMember {
                method {
                    name = "get"
                    param(StringClass)
                }
                replaceAny {
                    val key = args[0] as String
                    when (key) {
                        "ro.product.countrycode" -> "CN"
                        "ro.odm.lenovo.region" -> "prc"
                        "ro.config.zui.region" -> "PRC"
                        else -> callOriginal()
                    }
                }
            }
            injectMember {
                method {
                    name = "get"
                    param(StringClass, StringClass)
                }
                replaceAny {
                    val key = args[0] as String
                    when (key) {
                        "ro.product.countrycode" -> "CN"
                        "ro.odm.lenovo.region" -> "prc"
                        "ro.config.zui.region" -> "PRC"
                        else -> callOriginal()
                    }
                }
            }
        }
    }

    private fun filterContainsAction(filter: IntentFilter, targetAction: String): Boolean {
        for (index in 0 until filter.countActions()) {
            if (filter.getAction(index) == targetAction) {
                return true
            }
        }
        return false
    }

    private fun tagGalleryVerificationIntent(intent: Intent?) {
        if (intent?.action != ACTION_PRIVACY_VERIFICATION) return

        val stackTraceClassNames = Throwable().stackTrace.map { it.className }
        val mode = when {
            stackTraceClassNames.any { it in PRIVACY_ADD_CALLERS } -> REQUEST_MODE_ADD
            stackTraceClassNames.any { it in PRIVACY_OPEN_CALLERS } -> REQUEST_MODE_OPEN
            else -> return
        }

        intent.putExtra(EXTRA_REQUEST_MODE, mode)
        Log.i(TAG, "Tagged Gallery privacy request mode=$mode")
    }

    private companion object {
        private const val TAG = "DeZUXGalleryHook"
        private const val ACTION_GALLERY_VERIFICATION_RESPONSE = "gallery_verification_response"
        private const val ACTION_PRIVACY_VERIFICATION = "com.lenovo.privacyspace.verification"
        private const val EXTRA_REQUEST_MODE = "io.github.miner7222.dezux.PRIVACY_REQUEST_MODE"
        private const val KEY_FILES_TO_ADD_PRIVACY_GROUP = "filesToAddToPrivacyGroup"
        private const val REQUEST_MODE_ADD = "add"
        private const val REQUEST_MODE_OPEN = "open"
        private const val REQUEST_CONFIRM_CREDENTIAL = 0x22B8
        private const val RECEIVER_EXPORTED = 0x2
        private const val RECEIVER_NOT_EXPORTED = 0x4
        private val PRIVACY_ADD_CALLERS = setOf(
            "com.zui.gallery.main.utils.MenuExecutorUtils",
            "com.zui.gallery.app.AlbumPage",
            "com.zui.gallery.app.PhotoPage",
            "com.zui.gallery.main.ui.view.PhotoPageActionView",
            "com.zui.gallery.app.localtime.LocalTimeAlbumPage"
        )
        private val PRIVACY_OPEN_CALLERS = setOf(
            "com.zui.gallery.app.AlbumSetPage",
            "com.zui.gallery.banner.BaseActivity",
            "com.zui.gallery.banner.PrivacyBaseActivity"
        )
    }
}
