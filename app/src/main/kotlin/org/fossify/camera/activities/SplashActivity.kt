package org.fossify.camera.activities

import android.content.Intent
import android.os.Bundle
import org.fossify.commons.activities.BaseSplashActivity
import org.fossify.commons.extensions.baseConfig
import org.fossify.commons.helpers.SIDELOADING_FALSE

class SplashActivity : BaseSplashActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // This fork is signed with its own key, so the Fossify-signature check in commons would always
        // flag it as a "fake version". Mark it verified before the base class runs that check.
        baseConfig.appSideloadingStatus = SIDELOADING_FALSE
        super.onCreate(savedInstanceState)
    }

    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
