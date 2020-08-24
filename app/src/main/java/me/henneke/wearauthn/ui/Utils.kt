package me.henneke.wearauthn.ui

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.asDeferred
import kotlinx.coroutines.withContext
import me.henneke.wearauthn.BuildConfig
import me.henneke.wearauthn.R

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

val Context.keyguardManager
    get() = ContextCompat.getSystemService(this, KeyguardManager::class.java)
val Context.notificationManager
    get() = ContextCompat.getSystemService(this, NotificationManager::class.java)
val Context.powerManager
    get() = ContextCompat.getSystemService(this, PowerManager::class.java)
val Context.vibrator
    get() = ContextCompat.getSystemService(this, Vibrator::class.java)

val Context.defaultSharedPreferences: SharedPreferences
    get() = PreferenceManager.getDefaultSharedPreferences(this)

fun Context.sharedPreferences(name: String): SharedPreferences =
    getSharedPreferences(name, Context.MODE_PRIVATE)
