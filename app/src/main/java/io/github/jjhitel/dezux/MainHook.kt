package io.github.jjhitel.dezux

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.StringClass
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import java.lang.reflect.Modifier
import io.github.jjhitel.dezux.R

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

        loadApp("com.zui.game.service") {
            applyGameServiceHooks()
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

    private fun PackageParam.applyGlobalHooks() {
        val enabledFeatures = setOf(
            "DynamicFeature",
            "GoogleConnectivityResOverlay",
            "GoogleNetworkStackOverlay",
            "GoogleWifiResOverlay",
            "LgsiEnableMultiUser",
            "ZUISetupWizardExtROW",
            "ZuiAICloudOnlyRow",
            "ZuiDefaultAppRow"
        )

        val disabledFeatures = setOf(
            "AiFlashPoint",
            "AiWork",
            "BaiduNetdisk",
            "BaiduNetworkLocation",
            "Bilicomic",
            "Canva",
            "CapCut",
            "Capcut_prc",
            "ConnectivityResOverlay",
            "DataInterceptor",
            "DataMonitor",
            "DeviceIdService",
            "DouYin",
            "HSJPRO",
            "HyperEngine",
            "KuwoPlayerHD",
            "LeVoiceAgent",
            "LenovoSmartBuy_lenovopad",
            "LenovoStore",
            "LenovoXiaoTian",
            "LFHTianjiaoTablet",
            "LgsiDisableMultiUser",
            "LongPressPowerStartLevoice",
            "MiguPlay",
            "MiniGame",
            "Moffice_196",
            "NetworkStackOverlay",
            "SogouInput",
            "Touchnotes_HD",
            "Wangyiyun",
            "Weibo_HDwm",
            "WifiResOverlay",
            "XtsMusic",
            "XuiEasySync",
            "Youku",
            "ZUISetupWizardExtPRC",
            "ZuiAILens",
            "ZuiAILocalLlmPrc",
            "ZuiAlarm",
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
            "ZuiGallery",
            "ZuiGalleryWallpaperCropper",
            "ZuiLongWakelockStandbyControl",
            "ZuiMagicVoice",
            "ZuiMediaUsage",
            "ZuiNewBrowser",
            "ZuiNightPowerSave",
            "ZuiPackageInstaller",
            "ZuiPermission",
            "ZuiPermissionController",
            "ZuiPrivacyShield",
            "ZuiRescueParty",
            "ZuiSecurity",
            "ZuiServiceEngine",
            "ZuiSkipAiLens",
            "ZuiTianJiaoMode",
            "ZuiXlog",
            "ZuxWifiService",
            "aiwork_360"
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

}