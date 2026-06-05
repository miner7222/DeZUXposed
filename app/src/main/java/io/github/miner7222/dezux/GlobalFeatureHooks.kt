package io.github.miner7222.dezux

internal class GlobalFeatureHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        scope.install("LgsiFeatures enabled hook") {
            replaceMethod("com.lgsi.config.LgsiFeatures", "enabled", String::class.java) {
                if (args.isEmpty()) return@replaceMethod callOriginal()
                val feature = args[0] as? String ?: return@replaceMethod callOriginal()

                when (feature) {
                    in HookConstants.ENABLED_FEATURES -> true
                    in HookConstants.DISABLED_FEATURES -> false
                    else -> callOriginal()
                }
            }
        }
    }
}
