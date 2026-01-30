/*
 * SPDX-FileCopyrightText: 2025 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.gamebar

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.gamebar.R

class GameBarSettingsActivity : AppCompatActivity() {

    private val LAUNCHER_ALIAS_NAME = "com.android.gamebar.GameBarLauncher"
    
    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
        private const val REQUEST_CODE_OPEN_CSV = 1001
        private const val PREFS_NAME = "GameBarSettings"
        private const val KEY_SHOW_LAUNCHER_ICON = "show_launcher_icon"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_bar)
        
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        val collapsingToolbar = findViewById<com.google.android.material.appbar.CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        collapsingToolbar.title = getString(R.string.game_bar_title)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, GameBarFragment())
                .commit()
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, R.string.overlay_permission_granted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.overlay_permission_denied, Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_OPEN_CSV -> {
                if (resultCode == RESULT_OK) {
                    data?.data?.also { uri ->
                        handleExternalLogFile(uri)
                    }
                }
            }
        }
    }
    

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // ...
    }
    
    // Public methods to be called from Fragment
    fun setLauncherIconEnabled(enabled: Boolean) {
        val componentName = ComponentName(this, LAUNCHER_ALIAS_NAME)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP)
    }
    
    fun openExternalLogFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
        }
        
        try {
            startActivityForResult(intent, REQUEST_CODE_OPEN_CSV)
        } catch (e: Exception) {
            Toast.makeText(this, "File picker not available: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun handleExternalLogFile(uri: Uri) {
        try {
            // Copy file content to temporary location
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = java.io.File(cacheDir, "temp_external_log.csv")
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Open analytics activity with this file
            val intent = Intent(this, LogAnalyticsActivity::class.java).apply {
                putExtra(LogAnalyticsActivity.EXTRA_LOG_FILE_PATH, tempFile.absolutePath)
                putExtra(LogAnalyticsActivity.EXTRA_LOG_FILE_NAME, uri.lastPathSegment ?: "External Log")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening file: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
