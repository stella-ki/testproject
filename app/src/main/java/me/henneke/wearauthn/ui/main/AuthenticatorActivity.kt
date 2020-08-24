package me.henneke.wearauthn.ui.main

import android.app.Fragment
import android.app.FragmentTransaction
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import me.henneke.wearauthn.R
import me.henneke.wearauthn.fido.context.AuthenticatorContext
import kotlin.coroutines.CoroutineContext


class AuthenticatorActivity : AppCompatActivity() , CoroutineScope{

    private val BACK_STACK = "back_stack"
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }


    override fun onResume() {
        super.onResume()
        /*launch {
            AuthenticatorContext.initAuthenticator(this@AuthenticatorActivity.applicationContext)
            AuthenticatorContext.refreshCachedWebAuthnCredentialIfNecessary(applicationContext)
        }*/
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
    }

}