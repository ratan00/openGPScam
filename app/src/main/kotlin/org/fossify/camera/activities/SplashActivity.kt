package org.fossify.camera.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Launcher entry point. It deliberately does not extend the commons BaseSplashActivity, whose
 * Fossify-signature check would flag this fork as a "fake version" of the app.
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
