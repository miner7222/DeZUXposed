package io.github.miner7222.dezux

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log

internal class GalleryHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        hookVerificationRequestTagging()
        hookMainActivityPrivacyResult()
        hookPrivacyReceiverExport()
    }

    private fun hookPrivacyReceiverExport() {
        scope.install("Gallery privacy receiver export hook") {
            beforeMethod(
                "android.content.ContextWrapper",
                "registerReceiver",
                BroadcastReceiver::class.java,
                IntentFilter::class.java,
                Int::class.javaPrimitiveType!!,
            ) {
                if (args.size < 3) return@beforeMethod

                val filter = args[1] as? IntentFilter ?: return@beforeMethod
                if (!filterContainsAction(filter, HookConstants.ACTION_GALLERY_VERIFICATION_RESPONSE)) {
                    return@beforeMethod
                }

                val flags = args[2] as? Int ?: return@beforeMethod
                if ((flags and HookConstants.RECEIVER_NOT_EXPORTED) == 0) return@beforeMethod

                val newFlags = (flags and HookConstants.RECEIVER_NOT_EXPORTED.inv()) or
                    HookConstants.RECEIVER_EXPORTED
                args[2] = newFlags
                Log.i(HookConstants.TAG, "Changed Gallery privacy receiver flags from $flags to $newFlags")
            }
        }
    }

    private fun hookVerificationRequestTagging() {
        scope.install("Gallery Activity.startActivityForResult privacy tag hook") {
            beforeMethod(
                "android.app.Activity",
                "startActivityForResult",
                Intent::class.java,
                Int::class.javaPrimitiveType!!,
            ) {
                tagGalleryVerificationIntent(args[0] as? Intent)
            }
        }

        scope.install("Gallery Activity.startActivityForResult privacy tag hook with options") {
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

    private fun hookMainActivityPrivacyResult() {
        scope.install("Gallery MainActivity privacy result hook") {
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
                if (requestCode != HookConstants.REQUEST_CONFIRM_CREDENTIAL ||
                    resultCode != Activity.RESULT_OK
                ) {
                    return@afterMethod
                }

                val pendingObject = galleryUtilsClass
                    .getDeclaredMethod("takeMemCache", String::class.java)
                    .invoke(null, HookConstants.KEY_FILES_TO_ADD_PRIVACY_GROUP)
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
                        Log.i(HookConstants.TAG, "MainActivity privacy add triggered for single item")
                    }

                    pendingObject is List<*> -> {
                        menuExecutorClass
                            .getDeclaredMethod("addItemsToPrivacyFirst", List::class.java)
                            .invoke(menuExecutor, pendingObject)
                        val addItemsField = mainActivityClass.getDeclaredField("addItemsToPrivacyFirst")
                        addItemsField.isAccessible = true
                        addItemsField.setBoolean(null, true)
                        Log.i(HookConstants.TAG, "MainActivity privacy add triggered for list")
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
        if (intent?.action != HookConstants.ACTION_PRIVACY_VERIFICATION) return

        val stackTraceClassNames = Throwable().stackTrace.map { it.className }
        val mode = when {
            stackTraceClassNames.any { it in HookConstants.PRIVACY_ADD_CALLERS } -> HookConstants.REQUEST_MODE_ADD
            stackTraceClassNames.any { it in HookConstants.PRIVACY_OPEN_CALLERS } -> HookConstants.REQUEST_MODE_OPEN
            else -> return
        }

        intent.putExtra(HookConstants.EXTRA_REQUEST_MODE, mode)
        Log.i(HookConstants.TAG, "Tagged Gallery privacy request mode=$mode")
    }
}
