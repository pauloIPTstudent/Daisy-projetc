package com.GoToExpress.daisyapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import android.net.wifi.WifiManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DiagnoseFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DiagnoseFragment : Fragment(), SensorSetupDialog.OnDestroyListener {

    private val TAG = "BLE_SCAN_LOG"
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var wifiManager: WifiManager
    private var dialogRef: SensorSetupDialog? = null
    private var espNetwork: Network? = null
    // Callback para gerenciar os resultados do Scan
    private var deviceListUpdateListener: ((android.net.wifi.ScanResult) -> Unit)? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_diagnose, container, false)
    }
    // Receiver para escutar quando o scan de Wi-Fi terminar
    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wifiManager = requireContext().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val btnAddSensor = view.findViewById<FrameLayout>(R.id.btn_add_sensor)
        btnAddSensor.setOnClickListener {
            val dialog = SensorSetupDialog()
            dialogRef = dialog

            dialog.wifiCredentialsListener = object : SensorSetupDialog.OnWifiCredentialsListener {
                override fun onWifiCredentialsEntered(ssid: String, password: String) {
                    sendWifiCredentials(ssid, password)
                }
            }

            dialog.show(parentFragmentManager, "SensorDialog")

            startWifiScan()
        }
    }

    private fun startWifiScan() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        requireContext().registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            // Scan falhou (pode estar em throttle)
            scanSuccess() // Tenta ler os resultados antigos/em cache
        }
    }

    private fun scanSuccess() {
        val results = if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            wifiManager.scanResults
        } else {
            emptyList()
        }

        // Filtra para mostrar apenas redes que contenham "Daisy" no nome, se quiser
        val filteredResults = results.filter { it.SSID.contains("DAISY", ignoreCase = true) }

        dialogRef?.addWifiNetworksToList(filteredResults) { selectedNetwork ->
            connectToWifi(selectedNetwork.SSID)
        }
    }

    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private fun connectToWifi(ssid: String, password: String? = "12345678") {
        if (ssid.isBlank()) return

        val ssidClean = ssid.replace("\"", "").trim()

        // Verifica permissão
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssidClean)
                .apply { if (!password.isNullOrBlank()) setWpa2Passphrase(password) }
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val connectivityManager = requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // Cancela callback antigo
            wifiNetworkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }

            wifiNetworkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    espNetwork = network
                    Log.d("WIFI_LOG", "Rede disponível! Conectado temporariamente.")
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Conectado a $ssidClean", Toast.LENGTH_SHORT).show()
                        //updateUIConnected(ssidClean)
                        // Muda para o terceiro step do ViewFlipper
                        dialogRef?.view?.findViewById<ViewFlipper>(R.id.viewFlipper)?.showNext()
                    }
                }

                override fun onUnavailable() {
                    Log.e("WIFI_LOG", "Não foi possível conectar a $ssidClean")
                    //updateUIDisconnected()
                }

                override fun onLost(network: Network) {
                    Log.e("WIFI_LOG", "Conexão perdida com $ssidClean")
                    //updateUIDisconnected()
                }
            }

            connectivityManager.requestNetwork(request, wifiNetworkCallback!!)
        }
    }
    private fun navegarParaSensorSettings(){

    }
    /*private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            //val deviceName = device.name ?: "Desconhecido"
            val deviceName = result.scanRecord?.deviceName ?: device.name ?: "Desconhecido"
            val deviceAddress = device.address
            // Aqui enviamos o dispositivo encontrado para o Dialog através do Listener
            deviceListUpdateListener?.invoke(device)
            Log.d(TAG, "Dispositivo Encontrado: Nome: $deviceName | MAC: $deviceAddress")
        }
        override fun onScanFailed(errorCode: Int) {
            // LOG: Caso ocorra algum erro no scan
            Log.e(TAG, "Erro no Scan: Código $errorCode")
        }
    }*/

    /*private fun navegarParaReadings(dadosJson: String) {
        // Criamos uma nova instância do fragmento de leituras
        val novoFragmento = ReadingsFragment().apply {
            arguments = Bundle().apply {
                putString("dados_sensor", dadosJson)
            }
        }

        // Realiza a transição
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView3, novoFragmento) // Use o ID do seu container principal
            .addToBackStack(null) // Permite que o usuário volte ao sensor ao clicar em "voltar"
            .commit()
    }*/

    // Função para esconder o "Add Sensor" e mostrar o "Read"
    private fun updateUIConnected(sensorName: String) {
        val cardAddSensor = view?.findViewById<ConstraintLayout>(R.id.add_sensor_card)
        val cardSensor = view?.findViewById<ConstraintLayout>(R.id.sensor_card)
        val tvDeviceName = view?.findViewById<TextView>(R.id.tvDeviceName)

        // 1. Esconde o botão de adicionar
        cardAddSensor?.visibility = View.GONE

        // 2. Mostra o botão de leitura
        cardSensor?.visibility = View.VISIBLE

        // 3. Atualiza o texto com o nome do sensor
        tvDeviceName?.text = "$sensorName"
    }

    private fun updateUIDisconnected() {
        view?.findViewById<ConstraintLayout>(R.id.add_sensor_card)?.visibility = View.VISIBLE
        view?.findViewById<ConstraintLayout>(R.id.sensor_card)?.visibility = View.GONE

    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(wifiScanReceiver)
        } catch (e: Exception) { }
    }
    private fun sendWifiCredentials(ssid: String, password: String) {

        val network = espNetwork ?: return

        Thread {
            try {
                val url = URL("http://192.168.4.1/wifi")

                val json = JSONObject().apply {
                    put("ssid", ssid)
                    put("password", password)
                }

                val connection = network.openConnection(url) as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.write(json.toString().toByteArray())

                val responseCode = connection.responseCode

                activity?.runOnUiThread {
                    if (responseCode == 200) {
                        Toast.makeText(requireContext(), "Credenciais enviadas!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Erro $responseCode", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    override fun onDestroyDialog() {

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DiagnoseFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DiagnoseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}