package io.github.miner7222.dezux

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.database.MatrixCursor
import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.util.concurrent.ConcurrentHashMap

class MainHook : XposedModule() {

    private val isInsideHeaderCheck = ThreadLocal<Boolean>()
    private val hookedPackages = ConcurrentHashMap.newKeySet<String>()

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            TAG,
            "onModuleLoaded process=${param.processName} systemServer=${param.isSystemServer} " +
                "framework=$frameworkName($frameworkVersionCode) api=$apiVersion",
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(Log.INFO, TAG, "onSystemServerStarting loader=${param.classLoader}")
        ModernHookScope(this, param.classLoader).install("System LgsiFeatures hooks") {
            applyGlobalHooks()
        }
    }

    override fun onPackageReady(param: PackageReadyParam) {
        log(
            Log.INFO,
            TAG,
            "onPackageReady package=${param.packageName} first=${param.isFirstPackage} loader=${param.classLoader}",
        )

        if (!hookedPackages.add(param.packageName)) {
            log(Log.INFO, TAG, "Skipping duplicate package hooks for ${param.packageName}")
            return
        }

        val scope = ModernHookScope(this, param.classLoader)
        scope.install("Global LgsiFeatures hooks for ${param.packageName}") {
            applyGlobalHooks()
        }

        when (param.packageName) {
            SETTINGS_PACKAGE -> scope.applyAppSettingsHooks()
            GALLERY_PACKAGE -> scope.applyGalleryHooks()
            LAUNCHER_PACKAGE -> scope.applyLauncherHooks()
            GAME_SERVICE_PACKAGE -> scope.applyGameServiceHooks()
            WALLPAPER_SETTING_PACKAGE -> scope.applyWallpaperSettingHooks()
            OTA_PACKAGE, TBENGINE_PACKAGE -> scope.applyOtaHooks()
            LEVOICE_CAPTION_PACKAGE -> scope.applyLeVoiceCaptionHooks()
        }
    }

    private fun ModernHookScope.applyLeVoiceCaptionHooks() {
        install("LeVoice Microsoft API country-code hook") {
            replaceMethod("com.zui.translator.utils.MicrosoftApiKey", "getCountryCode") {
                "CN"
            }
        }
    }

    private fun ModernHookScope.applyGalleryHooks() {
        hookGalleryVerificationRequestTagging()
        hookMainActivityPrivacyResult()

        install("Gallery privacy receiver export hook") {
            beforeMethod(
                "android.content.ContextWrapper",
                "registerReceiver",
                BroadcastReceiver::class.java,
                IntentFilter::class.java,
                Int::class.javaPrimitiveType!!,
            ) {
                if (args.size < 3) return@beforeMethod

                val filter = args[1] as? IntentFilter ?: return@beforeMethod
                if (!filterContainsAction(filter, ACTION_GALLERY_VERIFICATION_RESPONSE)) return@beforeMethod

                val flags = args[2] as? Int ?: return@beforeMethod
                if ((flags and RECEIVER_NOT_EXPORTED) == 0) return@beforeMethod

                val newFlags = (flags and RECEIVER_NOT_EXPORTED.inv()) or RECEIVER_EXPORTED
                args[2] = newFlags
                Log.i(TAG, "Changed Gallery privacy receiver flags from $flags to $newFlags")
            }
        }
    }

    private fun ModernHookScope.applyLauncherHooks() {
        install("Launcher contextual-search property update hook") {
            beforeMethod(
                "com.android.quickstep.util.ContextualSearchStateManager",
                "requestUpdateProperties",
            ) {
                forceLauncherContextualSearchPackage(instanceOrNull)
            }
        }

        install("Launcher contextual-search availability hook") {
            replaceMethod(
                "com.android.quickstep.util.ContextualSearchStateManager",
                "isContextualSearchIntentAvailable",
            ) {
                val stateManager = instanceOrNull ?: return@replaceMethod callOriginal()
                forceLauncherContextualSearchPackage(stateManager)
                val original = callOriginal() as? Boolean ?: false
                if (original) return@replaceMethod true

                val context = getLauncherStateManagerContext(stateManager) ?: return@replaceMethod false
                val packageName = getLauncherContextualSearchPackage(stateManager)
                    ?: resolveLauncherContextualSearchPackage(context)
                if (packageName.isBlank()) return@replaceMethod false

                val available = isContextualSearchIntentAvailable(context, packageName)
                if (available) {
                    setLauncherIntentAvailabilityFlag(stateManager, true)
                    Log.i(CTS_TAG, "Recovered launcher contextual-search intent availability for $packageName")
                }
                available
            }
        }
    }

    private fun ModernHookScope.hookGalleryVerificationRequestTagging() {
        install("Gallery Activity.startActivityForResult privacy tag hook") {
            beforeMethod(
                "android.app.Activity",
                "startActivityForResult",
                Intent::class.java,
                Int::class.javaPrimitiveType!!,
            ) {
                tagGalleryVerificationIntent(args[0] as? Intent)
            }
        }

        install("Gallery Activity.startActivityForResult privacy tag hook with options") {
            beforeMethod(
                "android.app.Activity",
                "startActivityForResult",
                Intent::class.java,
                Int::class.javaPrimitiveType!!,
                Bundle::class.java,
            ) {
                tagGalleryVerificationIntent(args[0] as? Intent)
            }
        }
    }

    private fun ModernHookScope.hookMainActivityPrivacyResult() {
        install("Gallery MainActivity privacy result hook") {
            val galleryUtilsClass = loadClass("com.zui.gallery.util.GalleryUtils")
            val mainActivityClass = loadClass("com.zui.gallery.main.ui.activity.MainActivity")
            val mediaItemClass = loadClass("com.zui.gallery.data.MediaItem")
            val menuExecutorClass = loadClass("com.zui.gallery.main.utils.MenuExecutorUtils")

            afterMethod(
                "com.zui.gallery.main.ui.activity.MainActivity",
                "onActivityResult",
                Int::class.javaPrimitiveType!!,
                Int::class.javaPrimitiveType!!,
                Intent::class.java,
            ) {
                val requestCode = args[0] as? Int ?: return@afterMethod
                val resultCode = args[1] as? Int ?: return@afterMethod
                if (requestCode != REQUEST_CONFIRM_CREDENTIAL || resultCode != Activity.RESULT_OK) return@afterMethod

                val pendingObject = galleryUtilsClass
                    .getDeclaredMethod("takeMemCache", String::class.java)
                    .invoke(null, KEY_FILES_TO_ADD_PRIVACY_GROUP)
                    ?: return@afterMethod

                val menuExecutor = mainActivityClass
                    .getDeclaredMethod("getMenuExecutor")
                    .invoke(instanceOrNull)
                    ?: return@afterMethod

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

    private fun ModernHookScope.applyGlobalHooks() {
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
            "ZuiHFWallpaper",
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
            "xiangbb",
        )

        install("LgsiFeatures enabled hook") {
            replaceMethod("com.lgsi.config.LgsiFeatures", "enabled", String::class.java) {
                if (args.isEmpty()) return@replaceMethod callOriginal()
                val feature = args[0] as? String ?: return@replaceMethod callOriginal()

                when (feature) {
                    in enabledFeatures -> true
                    in disabledFeatures -> false
                    else -> callOriginal()
                }
            }
        }
    }

    private fun ModernHookScope.applyAppSettingsHooks() {
        install("Settings addMultiAppEntryIfSupported hook") {
            replaceMethod(
                "com.lenovo.settings.applications.LenovoAppHeaderPreferenceController",
                "addMultiAppEntryIfSupported",
                List::class.java,
            ) {
                isInsideHeaderCheck.set(true)
                try {
                    callOriginal()
                } finally {
                    isInsideHeaderCheck.set(false)
                }
            }
        }

        install("Settings AOSP Mainline availability hook") {
            replaceMethod(
                "com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController",
                "getAvailabilityStatus",
            ) {
                3
            }
        }

        install("Settings Lenovo Mainline availability hook") {
            replaceMethod(
                "com.lenovo.settings.deviceinfo.controller.MainlineModuleVersionPreferenceController",
                "getAvailabilityStatus",
            ) {
                3
            }
        }

        install("Settings LenovoUtils.isPrcVersion hook") {
            replaceMethod("com.lenovo.common.utils.LenovoUtils", "isPrcVersion") {
                if (isInsideHeaderCheck.get() == true) {
                    true
                } else {
                    callOriginal()
                }
            }
        }

        install("Settings AppClone support hook") {
            replaceMethod("com.lenovo.settings.applications.appclone.AppCloneUtils", "supportsAppClone") {
                if (isInsideHeaderCheck.get() == true) {
                    true
                } else {
                    callOriginal()
                }
            }
        }

        install("Settings Google services availability hook") {
            replaceMethod(
                "com.lenovo.settings.applications.GoogleServicesPreferenceController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        install("Settings restore preinstalled apps availability hook") {
            replaceMethod(
                "com.lenovo.settings.applications.preinstallrestore.RestorePreinstalledAppsPreferenceController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        install("Settings advanced security availability hook") {
            replaceMethod("com.android.settings.security.SecurityAdvancedSettingsController", "getAvailabilityStatus") {
                0
            }
        }

        install("Settings top-level tether availability hook") {
            replaceMethod(
                "com.lenovo.settings.homepage.controller.TopLevelTetherPreferenceController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        install("Settings Wi-Fi tether availability hook") {
            replaceMethod("com.android.settings.wifi.tether.WifiTetherPreferenceController", "isAvailable") {
                true
            }
        }

        install("Settings Wi-Fi tether extra availability hook") {
            replaceMethod(
                "com.lenovo.settings.connections.WifiTetherSettingsController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        install("Settings DashboardCategory mainline tile filter hook") {
            afterMethod("com.android.settingslib.drawer.DashboardCategory", "getTiles") {
                val original = result as? List<*> ?: return@afterMethod
                var filtered: ArrayList<Any?>? = null
                for (tile in original) {
                    if (tile != null && shouldHideMainlineTile(tile)) {
                        if (filtered == null) {
                            filtered = ArrayList(original.size)
                            for (kept in original) {
                                if (kept === tile) break
                                filtered.add(kept)
                            }
                        }
                        continue
                    }
                    filtered?.add(tile)
                }
                if (filtered != null) {
                    result = filtered
                }
            }
        }

        install("Settings DashboardFragment mainline tile filter hook") {
            replaceMethod(
                "com.android.settings.dashboard.DashboardFragment",
                "displayTile",
                loadClass("com.android.settingslib.drawer.Tile"),
            ) {
                val tile = args[0]
                if (tile != null && shouldHideMainlineTile(tile)) {
                    false
                } else {
                    callOriginal()
                }
            }
        }
    }

    private fun shouldHideMainlineTile(tile: Any): Boolean {
        return try {
            val getIntent = tile.javaClass.getMethod("getIntent")
            val intent = getIntent.invoke(tile) as? Intent ?: return false
            val action = intent.action ?: ""
            action == "android.settings.MODULE_UPDATE_SETTINGS" ||
                action == "android.settings.MODULE_UPDATE_VERSIONS"
        } catch (ignored: Throwable) {
            false
        }
    }

    private fun ModernHookScope.applyGameServiceHooks() {
        install("Game Service region property hook") {
            replaceMethod("com.zui.util.SystemPropertiesKt", "getSystemProperty", String::class.java) {
                val key = args[0] as String
                if (key == "ro.config.lgsi.region") {
                    "prc"
                } else {
                    callOriginal()
                }
            }
        }

        install("Game Service feature-key filter hook") {
            beforeMethod(
                "com.zui.game.service.FeatureKey\$Companion",
                "createByKeys",
                Array<String>::class.java,
            ) {
                val keys = args[0] as? Array<*>
                if (keys != null && (keys.contains("key_we_chat") || keys.contains("key_qq"))) {
                    args[0] = keys.filterIsInstance<String>()
                        .filter { it != "key_we_chat" && it != "key_qq" }
                        .toTypedArray()
                }
            }
        }
    }

    private fun ModernHookScope.applyWallpaperSettingHooks() {
        hookWallpaperSettingResourceArrays()
        hookWallpaperSettingChargeStyles()
        hookWallpaperSettingPrcLockscreenSwitch()
        hookWallpaperSettingPrcSearchIndex()
        hookWallpaperSettingMultiUserRestrictions()
    }

    private fun ModernHookScope.hookWallpaperSettingResourceArrays() {
        install("WallpaperSetting resource array hook") {
            replaceMethod("android.content.res.Resources", "getStringArray", Int::class.javaPrimitiveType!!) {
                val original = callOriginal()
                val resources = instanceOrNull as? Resources ?: return@replaceMethod original
                val resId = args.getOrNull(0) as? Int ?: return@replaceMethod original
                val arrayName = resources.getWallpaperSettingArrayEntryName(resId) ?: return@replaceMethod original
                when (arrayName) {
                    in WALLPAPER_STATIC_ARRAY_NAMES -> {
                        Log.i(WALLPAPER_TAG, "Using default static wallpapers with Pandaer extras")
                        WALLPAPER_STATIC_DEFAULT_WITH_PANDAER_EXTRAS.copyOf()
                    }
                    in WALLPAPER_CHARGE_STYLE_ARRAY_NAMES -> {
                        Log.i(WALLPAPER_TAG, "Using Pandaer charge style keys for $arrayName")
                        WALLPAPER_CHARGE_STYLE_PANDAER_KEYS.copyOf()
                    }
                    in WALLPAPER_CHARGE_STYLE_NAME_ARRAY_NAMES -> {
                        Log.i(WALLPAPER_TAG, "Using Pandaer charge style names for $arrayName")
                        resources.getWallpaperSettingChargeStyleNames()
                    }
                    else -> original
                }
            }
        }
    }

    private fun ModernHookScope.hookWallpaperSettingChargeStyles() {
        listOf(
            "com.zui.wallpapersetting.activity.ChargeStyleActivity",
            "com.zui.wallpapersetting.activity.ChargeStyleDetailActivity",
        ).forEach { className ->
            install("WallpaperSetting Pandaer charge-style hook for $className") {
                replaceMethod(className, "isPandear") {
                    Log.i(WALLPAPER_TAG, "Forcing Pandaer charge style options in $className")
                    true
                }
            }
        }
    }

    private fun ModernHookScope.hookWallpaperSettingPrcLockscreenSwitch() {
        install("WallpaperSetting lockscreen onCreate PRC-mode hook") {
            beforeMethod(
                "com.zui.wallpapersetting.activity.LockscreenPushMainActivity",
                "onCreate",
                Bundle::class.java,
            ) {
                forceWallpaperSettingLockscreenPrcMode(instanceOrNull)
            }
        }

        install("WallpaperSetting lockscreen onStart PRC-mode hook") {
            beforeMethod("com.zui.wallpapersetting.activity.LockscreenPushMainActivity", "onStart") {
                forceWallpaperSettingLockscreenPrcMode(instanceOrNull)
            }
        }
    }

    private fun ModernHookScope.hookWallpaperSettingPrcSearchIndex() {
        install("WallpaperSetting PRC search-index hook") {
            replaceMethod(
                "com.zui.wallpapersetting.WallpaperSearchIndexablesProvider",
                "queryXmlResources",
                Array<String>::class.java,
            ) {
                val provider = instanceOrNull as? ContentProvider ?: return@replaceMethod callOriginal()
                val context = provider.context ?: return@replaceMethod callOriginal()
                Log.i(WALLPAPER_TAG, "Returning PRC wallpaper settings search index")
                buildWallpaperSettingPrcSearchIndexCursor(context)
            }
        }
    }

    private fun ModernHookScope.hookWallpaperSettingMultiUserRestrictions() {
        install("WallpaperSetting getUserId hook") {
            replaceMethod("android.content.ContextWrapper", "getUserId") {
                0
            }
        }

        install("WallpaperSetting isAdminUser hook") {
            replaceMethod("android.os.UserManager", "isAdminUser") {
                true
            }
        }
    }

    private fun ModernHookScope.applyOtaHooks() {
        install("OTA SystemProperties.get(key) hook") {
            replaceMethod("android.os.SystemProperties", "get", String::class.java) {
                val key = args[0] as String
                when (key) {
                    "ro.product.countrycode" -> "CN"
                    "ro.odm.lenovo.region" -> "prc"
                    "ro.config.zui.region" -> "PRC"
                    else -> callOriginal()
                }
            }
        }

        install("OTA SystemProperties.get(key, default) hook") {
            replaceMethod("android.os.SystemProperties", "get", String::class.java, String::class.java) {
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

    private fun Resources.getWallpaperSettingArrayEntryName(resId: Int): String? {
        return runCatching {
            if (getResourcePackageName(resId) == WALLPAPER_SETTING_PACKAGE &&
                getResourceTypeName(resId) == "array"
            ) {
                getResourceEntryName(resId)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun Resources.getWallpaperSettingChargeStyleNames(): Array<String> {
        return WALLPAPER_CHARGE_STYLE_NAME_RESOURCE_NAMES.map { resourceName ->
            val resId = getIdentifier(resourceName, "string", WALLPAPER_SETTING_PACKAGE)
            if (resId != 0) getString(resId) else resourceName.removePrefix("charge_style_")
        }.toTypedArray()
    }

    private fun forceWallpaperSettingLockscreenPrcMode(activity: Any?) {
        val javaClass = activity?.javaClass ?: return
        runCatching {
            val isOverseaField = javaClass.getDeclaredField("isOversea")
            isOverseaField.isAccessible = true
            isOverseaField.setBoolean(null, false)
        }.onSuccess {
            Log.i(WALLPAPER_TAG, "Forced lockscreen full-screen switch into PRC mode")
        }.onFailure {
            Log.w(WALLPAPER_TAG, "Failed to force lockscreen PRC mode", it)
        }
    }

    private fun buildWallpaperSettingPrcSearchIndexCursor(context: Context): MatrixCursor {
        val columns = getSearchIndexableXmlColumns()
        val cursor = MatrixCursor(columns)
        val xmlResId = context.resources.getIdentifier("search_info", "xml", WALLPAPER_SETTING_PACKAGE)
            .takeIf { it != 0 }
            ?: WALLPAPER_SEARCH_INFO_XML_RES_ID

        cursor.newRow()
            .add("rank", WALLPAPER_SEARCH_IGNORED_RANK)
            .add("xmlResId", xmlResId)
            .add("className", null)
            .add("iconResId", 0)
            .add("intentAction", Intent.ACTION_MAIN)
            .add("intentTargetPackage", WALLPAPER_SETTING_PACKAGE)
            .add("intentTargetClass", "com.zui.wallpapersetting.activity.CustomizaActivity")
        return cursor
    }

    private fun getSearchIndexableXmlColumns(): Array<String> {
        return runCatching {
            val contractClass = Class.forName("android.provider.SearchIndexablesContract")
            val columns = contractClass.getDeclaredField("INDEXABLES_XML_RES_COLUMNS")
            columns.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            columns.get(null) as Array<String>
        }.getOrDefault(
            arrayOf(
                "rank",
                "xmlResId",
                "className",
                "iconResId",
                "intentAction",
                "intentTargetPackage",
                "intentTargetClass",
            ),
        )
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

    private fun forceLauncherContextualSearchPackage(stateManager: Any?): Boolean {
        if (stateManager == null) return false
        val context = getLauncherStateManagerContext(stateManager) ?: return false
        val fixedPackage = resolveLauncherContextualSearchPackage(context)
        if (fixedPackage.isBlank()) return false

        val currentPackage = getLauncherContextualSearchPackage(stateManager)
        if (currentPackage == fixedPackage) return false

        val packageField = stateManager.javaClass.getDeclaredField("mContextualSearchPackage")
        packageField.isAccessible = true
        packageField.set(stateManager, fixedPackage)
        Log.i(CTS_TAG, "Patched launcher contextual-search package from '$currentPackage' to '$fixedPackage'")
        return true
    }

    private fun getLauncherStateManagerContext(stateManager: Any): Context? {
        return runCatching {
            val contextField = stateManager.javaClass.getDeclaredField("mContext")
            contextField.isAccessible = true
            contextField.get(stateManager) as? Context
        }.getOrNull()
    }

    private fun getLauncherContextualSearchPackage(stateManager: Any): String? {
        return runCatching {
            val packageField = stateManager.javaClass.getDeclaredField("mContextualSearchPackage")
            packageField.isAccessible = true
            packageField.get(stateManager) as? String
        }.getOrNull()
    }

    private fun setLauncherIntentAvailabilityFlag(stateManager: Any, value: Boolean) {
        runCatching {
            val availableField = stateManager.javaClass.getDeclaredField("c")
            availableField.isAccessible = true
            availableField.setBoolean(stateManager, value)
        }
    }

    private fun resolveLauncherContextualSearchPackage(context: Context): String {
        val internalPackage = runCatching {
            val utilitiesClass = Class.forName("com.android.launcher3.Utilities", false, context.classLoader)
            val method = utilitiesClass.getDeclaredMethod(
                "getInternalString",
                Context::class.java,
                String::class.java,
            )
            method.isAccessible = true
            method.invoke(null, context, "config_defaultContextualSearchPackageName") as? String
        }.getOrNull()

        if (!internalPackage.isNullOrBlank()) return internalPackage
        return DEFAULT_CONTEXTUAL_SEARCH_PACKAGE
    }

    private fun isContextualSearchIntentAvailable(context: Context, packageName: String): Boolean {
        return runCatching {
            val intent = Intent(ACTION_LAUNCH_CONTEXTUAL_SEARCH).setPackage(packageName)
            val matches = context.packageManager.queryIntentActivities(intent, QUERY_INTENT_FLAGS)
            matches.isNotEmpty()
        }.getOrDefault(false)
    }

    private companion object {
        private const val TAG = "DeZUXHook"
        private const val CTS_TAG = "DeZUXCtsHook"
        private const val WALLPAPER_TAG = "DeZUXWallpaperHook"
        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val GALLERY_PACKAGE = "com.zui.gallery"
        private const val LAUNCHER_PACKAGE = "com.zui.launcher"
        private const val GAME_SERVICE_PACKAGE = "com.zui.game.service"
        private const val WALLPAPER_SETTING_PACKAGE = "com.zui.wallpapersetting"
        private const val OTA_PACKAGE = "com.lenovo.ota"
        private const val TBENGINE_PACKAGE = "com.lenovo.tbengine"
        private const val LEVOICE_CAPTION_PACKAGE = "com.lenovo.levoice.caption"
        private const val WALLPAPER_SEARCH_IGNORED_RANK = 0x840
        private const val WALLPAPER_SEARCH_INFO_XML_RES_ID = 0x7f140004
        private const val ACTION_LAUNCH_CONTEXTUAL_SEARCH = "android.app.contextualsearch.action.LAUNCH_CONTEXTUAL_SEARCH"
        private const val DEFAULT_CONTEXTUAL_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
        private const val QUERY_INTENT_FLAGS = 0xC0000
        private const val ACTION_GALLERY_VERIFICATION_RESPONSE = "gallery_verification_response"
        private const val ACTION_PRIVACY_VERIFICATION = "com.lenovo.privacyspace.verification"
        private const val EXTRA_REQUEST_MODE = "io.github.miner7222.dezux.PRIVACY_REQUEST_MODE"
        private const val KEY_FILES_TO_ADD_PRIVACY_GROUP = "filesToAddToPrivacyGroup"
        private const val REQUEST_MODE_ADD = "add"
        private const val REQUEST_MODE_OPEN = "open"
        private const val REQUEST_CONFIRM_CREDENTIAL = 0x22B8
        private const val RECEIVER_EXPORTED = 0x2
        private const val RECEIVER_NOT_EXPORTED = 0x4
        private val WALLPAPER_STATIC_ARRAY_NAMES = setOf("wallpapers", "wallpapers_pandaer")
        private val WALLPAPER_CHARGE_STYLE_ARRAY_NAMES = setOf(
            "chargeStyle",
            "chargeStyle_row",
            "chargeStyle_pandaer",
        )
        private val WALLPAPER_CHARGE_STYLE_NAME_ARRAY_NAMES = setOf(
            "chargeStyleName",
            "chargeStyleName_row",
            "chargeStyleName_pandaer",
        )
        private val WALLPAPER_STATIC_DEFAULT_WITH_PANDAER_EXTRAS = arrayOf(
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
        private val WALLPAPER_CHARGE_STYLE_PANDAER_KEYS = arrayOf(
            "charge_style_pandaer",
            "charge_style_default",
            "charge_style_girl",
            "charge_style_triangle",
            "charge_style_turbo",
        )
        private val WALLPAPER_CHARGE_STYLE_NAME_RESOURCE_NAMES = arrayOf(
            "charge_style_pandaer",
            "charge_style_default",
            "charge_style_girl",
            "charge_style_triangle",
            "charge_style_turbo",
        )
        private val PRIVACY_ADD_CALLERS = setOf(
            "com.zui.gallery.main.utils.MenuExecutorUtils",
            "com.zui.gallery.app.AlbumPage",
            "com.zui.gallery.app.PhotoPage",
            "com.zui.gallery.main.ui.view.PhotoPageActionView",
            "com.zui.gallery.app.localtime.LocalTimeAlbumPage",
        )
        private val PRIVACY_OPEN_CALLERS = setOf(
            "com.zui.gallery.app.AlbumSetPage",
            "com.zui.gallery.banner.BaseActivity",
            "com.zui.gallery.banner.PrivacyBaseActivity",
        )
    }
}
