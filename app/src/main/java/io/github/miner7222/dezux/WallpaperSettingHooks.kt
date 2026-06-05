package io.github.miner7222.dezux

import android.content.ContentProvider
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.database.MatrixCursor
import android.os.Bundle
import android.util.Log

internal class WallpaperSettingHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        hookResourceArrays()
        hookChargeStyles()
        hookPrcLockscreenSwitch()
        hookPrcSearchIndex()
        hookMultiUserRestrictions()
    }

    private fun hookResourceArrays() {
        scope.install("WallpaperSetting resource array hook") {
            replaceMethod("android.content.res.Resources", "getStringArray", Int::class.javaPrimitiveType!!) {
                val original = callOriginal()
                val resources = instanceOrNull as? Resources ?: return@replaceMethod original
                val resId = args.getOrNull(0) as? Int ?: return@replaceMethod original
                val arrayName = resources.getWallpaperSettingArrayEntryName(resId) ?: return@replaceMethod original
                when (arrayName) {
                    in HookConstants.WALLPAPER_STATIC_ARRAY_NAMES -> {
                        Log.i(HookConstants.WALLPAPER_TAG, "Using default static wallpapers with Pandaer extras")
                        HookConstants.WALLPAPER_STATIC_DEFAULT_WITH_PANDAER_EXTRAS.copyOf()
                    }
                    in HookConstants.WALLPAPER_CHARGE_STYLE_ARRAY_NAMES -> {
                        Log.i(HookConstants.WALLPAPER_TAG, "Using Pandaer charge style keys for $arrayName")
                        HookConstants.WALLPAPER_CHARGE_STYLE_PANDAER_KEYS.copyOf()
                    }
                    in HookConstants.WALLPAPER_CHARGE_STYLE_NAME_ARRAY_NAMES -> {
                        Log.i(HookConstants.WALLPAPER_TAG, "Using Pandaer charge style names for $arrayName")
                        resources.getWallpaperSettingChargeStyleNames()
                    }
                    else -> original
                }
            }
        }
    }

    private fun hookChargeStyles() {
        listOf(
            "com.zui.wallpapersetting.activity.ChargeStyleActivity",
            "com.zui.wallpapersetting.activity.ChargeStyleDetailActivity",
        ).forEach { className ->
            scope.install("WallpaperSetting Pandaer charge-style hook for $className") {
                replaceMethod(className, "isPandear") {
                    Log.i(HookConstants.WALLPAPER_TAG, "Forcing Pandaer charge style options in $className")
                    true
                }
            }
        }
    }

    private fun hookPrcLockscreenSwitch() {
        scope.install("WallpaperSetting lockscreen onCreate PRC-mode hook") {
            beforeMethod(
                "com.zui.wallpapersetting.activity.LockscreenPushMainActivity",
                "onCreate",
                Bundle::class.java,
            ) {
                forceLockscreenPrcMode(instanceOrNull)
            }
        }

        scope.install("WallpaperSetting lockscreen onStart PRC-mode hook") {
            beforeMethod("com.zui.wallpapersetting.activity.LockscreenPushMainActivity", "onStart") {
                forceLockscreenPrcMode(instanceOrNull)
            }
        }
    }

    private fun hookPrcSearchIndex() {
        scope.install("WallpaperSetting PRC search-index hook") {
            replaceMethod(
                "com.zui.wallpapersetting.WallpaperSearchIndexablesProvider",
                "queryXmlResources",
                Array<String>::class.java,
            ) {
                val provider = instanceOrNull as? ContentProvider ?: return@replaceMethod callOriginal()
                val context = provider.context ?: return@replaceMethod callOriginal()
                Log.i(HookConstants.WALLPAPER_TAG, "Returning PRC wallpaper settings search index")
                buildPrcSearchIndexCursor(context)
            }
        }
    }

    private fun hookMultiUserRestrictions() {
        scope.install("WallpaperSetting getUserId hook") {
            replaceMethod("android.content.ContextWrapper", "getUserId") {
                0
            }
        }

        scope.install("WallpaperSetting isAdminUser hook") {
            replaceMethod("android.os.UserManager", "isAdminUser") {
                true
            }
        }
    }

    private fun Resources.getWallpaperSettingArrayEntryName(resId: Int): String? {
        return runCatching {
            if (getResourcePackageName(resId) == HookConstants.WALLPAPER_SETTING_PACKAGE &&
                getResourceTypeName(resId) == "array"
            ) {
                getResourceEntryName(resId)
            } else {
                null
            }
        }.getOrNull()
    }

    private fun Resources.getWallpaperSettingChargeStyleNames(): Array<String> {
        return HookConstants.WALLPAPER_CHARGE_STYLE_NAME_RESOURCE_NAMES.map { resourceName ->
            val resId = getIdentifier(resourceName, "string", HookConstants.WALLPAPER_SETTING_PACKAGE)
            if (resId != 0) getString(resId) else resourceName.removePrefix("charge_style_")
        }.toTypedArray()
    }

    private fun forceLockscreenPrcMode(activity: Any?) {
        val javaClass = activity?.javaClass ?: return
        runCatching {
            val isOverseaField = javaClass.getDeclaredField("isOversea")
            isOverseaField.isAccessible = true
            isOverseaField.setBoolean(null, false)
        }.onSuccess {
            Log.i(HookConstants.WALLPAPER_TAG, "Forced lockscreen full-screen switch into PRC mode")
        }.onFailure {
            Log.w(HookConstants.WALLPAPER_TAG, "Failed to force lockscreen PRC mode", it)
        }
    }

    private fun buildPrcSearchIndexCursor(context: Context): MatrixCursor {
        val columns = getSearchIndexableXmlColumns()
        val cursor = MatrixCursor(columns)
        val xmlResId = context.resources.getIdentifier(
            "search_info",
            "xml",
            HookConstants.WALLPAPER_SETTING_PACKAGE,
        ).takeIf { it != 0 }
            ?: HookConstants.WALLPAPER_SEARCH_INFO_XML_RES_ID

        cursor.newRow()
            .add("rank", HookConstants.WALLPAPER_SEARCH_IGNORED_RANK)
            .add("xmlResId", xmlResId)
            .add("className", null)
            .add("iconResId", 0)
            .add("intentAction", Intent.ACTION_MAIN)
            .add("intentTargetPackage", HookConstants.WALLPAPER_SETTING_PACKAGE)
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
}
