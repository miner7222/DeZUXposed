package io.github.miner7222.dezux

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class VerificationActivity : Activity() {

    private val requestMode by lazy {
        intent.getStringExtra(EXTRA_REQUEST_MODE) ?: REQUEST_MODE_OPEN
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate savedInstanceState=${savedInstanceState != null} requestMode=$requestMode")
        if (savedInstanceState == null) {
            startVerificationFlow()
        }
    }

    @Suppress("DEPRECATION")
    private fun startVerificationFlow() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.private_album_unlock_title)
        val confirmIntent = keyguardManager
            ?.takeIf { it.isDeviceSecure }
            ?.createConfirmDeviceCredentialIntent(title, null)

        if (confirmIntent == null) {
            Log.i(TAG, "No device credential configured, completing request directly")
            completeVerifiedRequest()
            return
        }

        Log.i(TAG, "Launching confirm credential activity")
        startActivityForResult(confirmIntent, REQUEST_CONFIRM_CREDENTIAL)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult requestCode=$requestCode resultCode=$resultCode")

        if (requestCode != REQUEST_CONFIRM_CREDENTIAL) {
            return
        }

        if (resultCode == RESULT_OK) return completeVerifiedRequest()

        setResult(RESULT_CANCELED)
        finish()
    }

    private fun completeVerifiedRequest() {
        if (requestMode == REQUEST_MODE_ADD) {
            Log.i(TAG, "Handling privacy add request")
            setResult(RESULT_OK)
            finish()
            return
        }

        openPrivateAlbum()
    }

    private fun openPrivateAlbum() {
        Thread {
            Log.i(TAG, "Trying root bridge into Gallery private album")
            val launchResult = runRootCommand(
                "id && am start --user current -n $GALLERY_COMPONENT -a $GALLERY_ACTION --ez $EXTRA_PRIVATE_GALLERY true -f $FLAG_ACTIVITY_NEW_TASK_HEX"
            )
            val hasRootAccess = launchResult.output.contains("uid=0")
            Log.i(TAG, "Root bridge result success=${launchResult.success} hasRootAccess=$hasRootAccess output=${launchResult.output}")

            when {
                !hasRootAccess -> runOnUiThread {
                    Toast.makeText(this, R.string.grant_root_access_toast, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CANCELED)
                    finish()
                }

                !launchResult.success -> {
                    Log.e(TAG, "Failed to open private album: ${launchResult.output}")
                    runOnUiThread {
                        Toast.makeText(this, R.string.private_album_launch_failed, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }

                else -> {
                    sendVerificationBroadcast()
                    runOnUiThread {
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            }
        }.start()
    }

    private fun sendVerificationBroadcast() {
        Log.i(TAG, "Sending gallery verification broadcast")
        sendBroadcast(
            Intent(ACTION_GALLERY_VERIFICATION_RESPONSE)
                .putExtra(EXTRA_PRIVACY_RESPONSE_CODE, PRIVACY_RESPONSE_CODE)
        )
    }

    private fun runRootCommand(command: String): ShellResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.waitFor()
            val success = exitCode == 0 &&
                !output.contains("Error:", ignoreCase = true) &&
                !output.contains("Exception", ignoreCase = true)
            ShellResult(success = success, output = output)
        } catch (throwable: Throwable) {
            Log.e(TAG, "Root command failed: $command", throwable)
            ShellResult(success = false, output = throwable.message.orEmpty())
        }
    }

    private data class ShellResult(
        val success: Boolean,
        val output: String
    )

    companion object {
        private const val TAG = "VerificationActivity"
        private const val REQUEST_CONFIRM_CREDENTIAL = 0x22B8

        private const val EXTRA_TITLE = "android.app.extra.TITLE"
        private const val EXTRA_REQUEST_MODE = "io.github.miner7222.dezux.PRIVACY_REQUEST_MODE"
        private const val ACTION_GALLERY_VERIFICATION_RESPONSE = "gallery_verification_response"
        private const val EXTRA_PRIVACY_RESPONSE_CODE = "PRIVACY_RESPONSE_CODE"
        private const val PRIVACY_RESPONSE_CODE = 1000
        private const val REQUEST_MODE_ADD = "add"
        private const val REQUEST_MODE_OPEN = "open"

        private const val GALLERY_COMPONENT = "com.zui.gallery/com.zui.gallery.banner.FileListActivity"
        private const val GALLERY_ACTION = "com.zui.gallery.open_privacy_gallery"
        private const val EXTRA_PRIVATE_GALLERY = "privateGallery"
        private const val FLAG_ACTIVITY_NEW_TASK_HEX = "0x04000000"
    }
}
