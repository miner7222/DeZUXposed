package io.github.miner7222.dezux

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.util.concurrent.ConcurrentHashMap

class MainHook : XposedModule() {

    private val settingsHookState = SettingsHookState()
    private val hookedPackages = ConcurrentHashMap.newKeySet<String>()

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            HookConstants.TAG,
            "onModuleLoaded process=${param.processName} systemServer=${param.isSystemServer} " +
                "framework=$frameworkName($frameworkVersionCode) api=$apiVersion",
        )
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(Log.INFO, HookConstants.TAG, "onSystemServerStarting loader=${param.classLoader}")
        GlobalFeatureHooks(ModernHookScope(this, param.classLoader)).install()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        log(
            Log.INFO,
            HookConstants.TAG,
            "onPackageReady package=${param.packageName} first=${param.isFirstPackage} loader=${param.classLoader}",
        )

        if (!hookedPackages.add(param.packageName)) {
            log(Log.INFO, HookConstants.TAG, "Skipping duplicate package hooks for ${param.packageName}")
            return
        }

        installPackageHooks(ModernHookScope(this, param.classLoader), param.packageName)
    }

    private fun installPackageHooks(scope: ModernHookScope, packageName: String) {
        GlobalFeatureHooks(scope).install()

        when (packageName) {
            HookConstants.SETTINGS_PACKAGE -> SettingsHooks(scope, settingsHookState).install()
            HookConstants.GALLERY_PACKAGE -> GalleryHooks(scope).install()
            HookConstants.LAUNCHER_PACKAGE -> LauncherHooks(scope).install()
            HookConstants.GAME_SERVICE_PACKAGE -> GameServiceHooks(scope).install()
            HookConstants.WALLPAPER_SETTING_PACKAGE -> WallpaperSettingHooks(scope).install()
            HookConstants.OTA_PACKAGE, HookConstants.TBENGINE_PACKAGE -> OtaHooks(scope).install()
            HookConstants.LEVOICE_CAPTION_PACKAGE -> LeVoiceCaptionHooks(scope).install()
        }
    }
}
