package com.GoToExpress.daisyapp

import android.Manifest
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ViewFlipper
import androidx.annotation.RequiresPermission
import androidx.fragment.app.DialogFragment
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.widget.EditText
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import com.GoToExpress.daisyapp.R

class SensorSetupDialog() : DialogFragment() {
    interface OnWifiCredentialsListener {
        fun onWifiCredentialsEntered(ssid: String, password: String)
    }
    var wifiCredentialsListener: OnWifiCredentialsListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_set_sensor, container, false)
    }
    interface OnDestroyListener {
        fun onDestroyDialog()
    }

    var onDestroyListener: OnDestroyListener? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnFinish).setOnClickListener {
            dismiss() // Fecha o dialog
        }
        view.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            dismiss() // Fecha o dialog
        }

        view.findViewById<Button>(R.id.btnGoToStep2).setOnClickListener {
            // Lógica para iniciar o Bluetooth ou scanner
            view.findViewById<ViewFlipper>(R.id.viewFlipper).showNext()
        }// */

        val ssidEditText = view.findViewById<EditText>(R.id.ssidEditText)
        val passwordEditText = view.findViewById<EditText>(R.id.passwordEditText)
        val btnSendWifi = view.findViewById<Button>(R.id.btnSendWifi)

        btnSendWifi.setOnClickListener {
            val ssid = ssidEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (ssid.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Preencha SSID e senha", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            wifiCredentialsListener?.onWifiCredentialsEntered(ssid, password)
        }
    }
    fun addDeviceToList(results: List<ScanResult>, onClick: (ScanResult) -> Unit) {
        val container = view?.findViewById<LinearLayout>(R.id.list_container)
        container?.removeAllViews() // Limpa a lista anterior

        results.forEach { result ->
            val ssid = if (result.SSID.isEmpty()) "Rede Oculta" else result.SSID

            val btn = Button(requireContext()).apply {
                text = "$ssid\nBSSID: ${result.BSSID}"
                setOnClickListener { onClick(result) }
            }
            container?.addView(btn)
        }
    }

    // No seu SensorSetupDialog.kt
    fun addWifiNetworksToList(results: List<ScanResult>, onClick: (ScanResult) -> Unit) {
        val container = view?.findViewById<LinearLayout>(R.id.list_container)
        container?.removeAllViews() // Limpa a lista anterior

        results.forEach { result ->
            val ssid = if (result.SSID.isEmpty()) "Rede Oculta" else result.SSID

            val btn = Button(requireContext()).apply {
                text = "$ssid\nBSSID: ${result.BSSID}"
                setOnClickListener { onClick(result) }
            }
            container?.addView(btn)
        }
    }
    // Opcional: Para fazer o dialog ocupar quase toda a largura da tela
    override fun onStart() {
        super.onStart()
        dialog?.window?.setGravity(Gravity.BOTTOM)
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        // Define o fundo transparente para o arredondamento do XML aparecer
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDestroyListener?.onDestroyDialog()
    }

}