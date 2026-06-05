package io.github.miner7222.dezux

import android.content.Context
import android.content.Intent
import android.util.Log

internal class LauncherHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        scope.install("Launcher contextual-search property update hook") {
            beforeMethod(
                "com.android.quickstep.util.ContextualSearchStateManager",
                "requestUpdateProperties",
            ) {
                forceContextualSearchPackage(instanceOrNull)
            }
        }

        scope.install("Launcher contextual-search availability hook") {
            replaceMethod(
                "com.android.quickstep.util.ContextualSearchStateManager",
                "isContextualSearchIntentAvailable",
            ) {
                val stateManager = instanceOrNull ?: return@replaceMethod callOriginal()
                forceContextualSearchPackage(stateManager)
                val original = callOriginal() as? Boolean ?: false
                if (original) return@replaceMethod true

                val context = getStateManagerContext(stateManager) ?: return@replaceMethod false
                val packageName = getContextualSearchPackage(stateManager)
                    ?: resolveContextualSearchPackage(context)
                if (packageName.isBlank()) return@replaceMethod false

                val available = isContextualSearchIntentAvailable(context, packageName)
                if (available) {
                    setIntentAvailabilityFlag(stateManager, true)
                    Log.i(HookConstants.CTS_TAG, "Recovered launcher contextual-search intent availability for $packageName")
                }
                available
            }
        }
    }

    private fun forceContextualSearchPackage(stateManager: Any?): Boolean {
        if (stateManager == null) return false
        val context = getStateManagerContext(stateManager) ?: return false
        val fixedPackage = resolveContextualSearchPackage(context)
        if (fixedPackage.isBlank()) return false

        val currentPackage = getContextualSearchPackage(stateManager)
        if (currentPackage == fixedPackage) return false

        val packageField = stateManager.javaClass.getDeclaredField("mContextualSearchPackage")
        packageField.isAccessible = true
        packageField.set(stateManager, fixedPackage)
        Log.i(HookConstants.CTS_TAG, "Patched launcher contextual-search package from '$currentPackage' to '$fixedPackage'")
        return true
    }

    private fun getStateManagerContext(stateManager: Any): Context? {
        return runCatching {
            val contextField = stateManager.javaClass.getDeclaredField("mContext")
            contextField.isAccessible = true
            contextField.get(stateManager) as? Context
        }.getOrNull()
    }

    private fun getContextualSearchPackage(stateManager: Any): String? {
        return runCatching {
            val packageField = stateManager.javaClass.getDeclaredField("mContextualSearchPackage")
            packageField.isAccessible = true
            packageField.get(stateManager) as? String
        }.getOrNull()
    }

    private fun setIntentAvailabilityFlag(stateManager: Any, value: Boolean) {
        runCatching {
            val availableField = stateManager.javaClass.getDeclaredField("c")
            availableField.isAccessible = true
            availableField.setBoolean(stateManager, value)
        }
    }

    private fun resolveContextualSearchPackage(context: Context): String {
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
        return HookConstants.DEFAULT_CONTEXTUAL_SEARCH_PACKAGE
    }

    private fun isContextualSearchIntentAvailable(context: Context, packageName: String): Boolean {
        return runCatching {
            val intent = Intent(HookConstants.ACTION_LAUNCH_CONTEXTUAL_SEARCH).setPackage(packageName)
            val matches = context.packageManager.queryIntentActivities(intent, HookConstants.QUERY_INTENT_FLAGS)
            matches.isNotEmpty()
        }.getOrDefault(false)
    }
}
