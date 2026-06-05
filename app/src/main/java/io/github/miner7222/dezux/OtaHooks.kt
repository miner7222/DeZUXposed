package io.github.miner7222.dezux

internal class OtaHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        scope.install("OTA SystemProperties.get(key) hook") {
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

        scope.install("OTA SystemProperties.get(key, default) hook") {
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
}
