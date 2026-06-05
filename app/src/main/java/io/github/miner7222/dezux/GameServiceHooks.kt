package io.github.miner7222.dezux

internal class GameServiceHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        scope.install("Game Service region property hook") {
            replaceMethod("com.zui.util.SystemPropertiesKt", "getSystemProperty", String::class.java) {
                val key = args[0] as String
                if (key == "ro.config.lgsi.region") {
                    "prc"
                } else {
                    callOriginal()
                }
            }
        }

        scope.install("Game Service feature-key filter hook") {
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
}
