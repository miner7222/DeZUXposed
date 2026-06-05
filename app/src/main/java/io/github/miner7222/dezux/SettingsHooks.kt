package io.github.miner7222.dezux

import android.content.Intent

internal class SettingsHookState {
    val isInsideHeaderCheck: ThreadLocal<Boolean> = ThreadLocal()
}

internal class SettingsHooks(
    private val scope: ModernHookScope,
    private val state: SettingsHookState,
) {
    fun install() {
        scope.install("Settings addMultiAppEntryIfSupported hook") {
            replaceMethod(
                "com.lenovo.settings.applications.LenovoAppHeaderPreferenceController",
                "addMultiAppEntryIfSupported",
                List::class.java,
            ) {
                state.isInsideHeaderCheck.set(true)
                try {
                    callOriginal()
                } finally {
                    state.isInsideHeaderCheck.set(false)
                }
            }
        }

        scope.install("Settings AOSP Mainline availability hook") {
            replaceMethod(
                "com.android.settings.deviceinfo.firmwareversion.MainlineModuleVersionPreferenceController",
                "getAvailabilityStatus",
            ) {
                3
            }
        }

        scope.install("Settings Lenovo Mainline availability hook") {
            replaceMethod(
                "com.lenovo.settings.deviceinfo.controller.MainlineModuleVersionPreferenceController",
                "getAvailabilityStatus",
            ) {
                3
            }
        }

        scope.install("Settings LenovoUtils.isPrcVersion hook") {
            replaceMethod("com.lenovo.common.utils.LenovoUtils", "isPrcVersion") {
                if (state.isInsideHeaderCheck.get() == true) {
                    true
                } else {
                    callOriginal()
                }
            }
        }

        scope.install("Settings AppClone support hook") {
            replaceMethod("com.lenovo.settings.applications.appclone.AppCloneUtils", "supportsAppClone") {
                if (state.isInsideHeaderCheck.get() == true) {
                    true
                } else {
                    callOriginal()
                }
            }
        }

        scope.install("Settings Google services availability hook") {
            replaceMethod(
                "com.lenovo.settings.applications.GoogleServicesPreferenceController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        scope.install("Settings restore preinstalled apps availability hook") {
            replaceMethod(
                "com.lenovo.settings.applications.preinstallrestore.RestorePreinstalledAppsPreferenceController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        scope.install("Settings advanced security availability hook") {
            replaceMethod("com.android.settings.security.SecurityAdvancedSettingsController", "getAvailabilityStatus") {
                0
            }
        }

        scope.install("Settings top-level tether availability hook") {
            replaceMethod(
                "com.lenovo.settings.homepage.controller.TopLevelTetherPreferenceController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        scope.install("Settings Wi-Fi tether availability hook") {
            replaceMethod("com.android.settings.wifi.tether.WifiTetherPreferenceController", "isAvailable") {
                true
            }
        }

        scope.install("Settings Wi-Fi tether extra availability hook") {
            replaceMethod(
                "com.lenovo.settings.connections.WifiTetherSettingsController",
                "getAvailabilityStatus",
            ) {
                0
            }
        }

        scope.install("Settings DashboardCategory mainline tile filter hook") {
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

        scope.install("Settings DashboardFragment mainline tile filter hook") {
            replaceMethod(
                "com.android.settings.dashboard.DashboardFragment",
                "displayTile",
                scope.loadClass("com.android.settingslib.drawer.Tile"),
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
}
