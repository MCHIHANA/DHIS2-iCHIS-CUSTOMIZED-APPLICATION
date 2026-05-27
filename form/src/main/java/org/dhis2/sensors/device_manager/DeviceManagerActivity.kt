package org.dhis2.sensors.device_manager

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import androidx.fragment.app.FragmentContainerView

class DeviceManagerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val containerView =
            FragmentContainerView(this).apply {
                id = ViewCompat.generateViewId()
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
            }

        setContentView(containerView)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(containerView.id, DeviceManagerFragment())
            }
        }
    }
}
