package io.github.miner7222.dezux

internal class LeVoiceCaptionHooks(
    private val scope: ModernHookScope,
) {
    fun install() {
        scope.install("LeVoice Microsoft API country-code hook") {
            replaceMethod("com.zui.translator.utils.MicrosoftApiKey", "getCountryCode") {
                "CN"
            }
        }
    }
}
