package me.henneke.wearauthn.ui.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.henneke.wearauthn.R
import me.henneke.wearauthn.bthid.*
import me.henneke.wearauthn.byteArrayToHexString
import me.henneke.wearauthn.decodeToStringOrNull
import me.henneke.wearauthn.fido.context.AuthenticatorStatus
import me.henneke.wearauthn.fido.hid.TransactionManager
import java.util.*

private const val TAG = "AuthenticatorAttachedActivity"
class AuthenticatorAttachedActivity : AppCompatActivity(){

    private var transactionManager: TransactionManager? = null
    private var hidDeviceProfile: HidDeviceProfile? = null
    private lateinit var authenticatorContext: HidAuthenticatorContext

    private lateinit var viewsToHideOnAmbient: List<View>
    lateinit var connectedToDeviceView: TextView

    private val hidIntrDataListener = object : HidIntrDataListener {
        override fun onIntrData(
            device: BluetoothDevice,
            reportId: Byte,
            data: ByteArray,
            host: InputHostWrapper
        ) {
            Log.i(TAG, "onIntrData - ${device.name} " + data.byteArrayToHexString())
            transactionManager?.handleReport(data) {
                for (rawReport in it) {
                    Log.i(TAG, "onIntrData - rawReport " + rawReport.byteArrayToHexString())
                    host.sendReport(device, reportId.toInt(), rawReport)
                }
            }
        }
    }

    private val hidProfileListener = object : HidDataSender.ProfileListener {
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            when (state) {
                BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED -> {
                    finish()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    val connectingToDeviceMessage =
                        getString(
                            R.string.connecting_to_device_message,
                            TextUtils.htmlEncode(device.identifier)
                        )
                    connectedToDeviceView.text =
                        Html.fromHtml(connectingToDeviceMessage, Html.FROM_HTML_MODE_LEGACY)
                }
                BluetoothProfile.STATE_CONNECTED -> {
                    val connectedToDeviceMessage =
                        getString(
                            R.string.connected_to_device_message,
                            TextUtils.htmlEncode(device.identifier)
                        )
                    connectedToDeviceView.text =
                        Html.fromHtml(connectedToDeviceMessage, Html.FROM_HTML_MODE_LEGACY)
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            if (!registered)
                finish()
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticator_attached)

        authenticatorContext = HidAuthenticatorContext(this)
        hidDeviceProfile = HidDataSender.register(this, hidProfileListener, hidIntrDataListener)
        connectedToDeviceView = findViewById(R.id.connectedToDeviceView)
    }

    override fun onStart() {
        super.onStart()

        if (!isBluetoothEnabled) {
            startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1)
        }

        transactionManager = TransactionManager(authenticatorContext)
        if (hidDeviceProfile == null) {
            finish()
            return
        }

        if (hidDeviceProfile!!.connectedDevices.isEmpty() && intent.hasExtra(EXTRA_DEVICE)) {
           /* if (intent.hasExtra(ComplicationProviderService.EXTRA_COMPLICATION_ID)) {
                ProviderUpdateRequester(
                    this,
                    ComponentName(this, ShortcutComplicationProviderService::class.java)
                ).requestUpdateAll()
            }*/
            val device = intent.getParcelableExtra<BluetoothDevice>(EXTRA_DEVICE)
            if (device == null || device !in defaultAdapter.bondedDevices) {
                startActivity(Intent(this, AuthenticatorActivity::class.java))
                finish()
                return
            }
            // Simulate a change to connecting state in order to update the UI immediately.
            hidProfileListener.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTING)
            HidDataSender.requestConnect(device)
        } else if (hidDeviceProfile!!.connectedDevices.isEmpty()) {
            finish()
        } else {
            check(hidDeviceProfile!!.connectedDevices.size == 1)
            val connectedDevice = hidDeviceProfile!!.connectedDevices[0]
            // Simulate a change to connected state for the currently connected device to update UI.
            hidProfileListener.onConnectionStateChanged(
                connectedDevice,
                BluetoothProfile.STATE_CONNECTED
            )
        }
    }

    override fun onStop() {
        super.onStop()

        // Do not disconnect if another activity is launched by the authenticator.
        if (authenticatorContext.status != AuthenticatorStatus.IDLE) {
            Log.e(TAG, "onStop() called during authenticator action")
            return
        }

        HidDataSender.requestConnect(null)
        transactionManager = null
    }


    override fun onDestroy() {
        super.onDestroy()
        HidDataSender.unregister(hidProfileListener, hidIntrDataListener)
    }

    companion object {
        const val EXTRA_DEVICE = "me.henneke.wearauthn.extra.DEVICE"
    }
}
