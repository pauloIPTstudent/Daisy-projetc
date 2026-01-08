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
import androidx.core.content.ContextCompat
import org.json.JSONObject
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

    // UUIDs do seu código ESP32
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHAR_READ_SENSOR = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val CHAR_NAME_CONTROL = UUID.fromString("82c8bb2a-4309-11ec-81d3-0242ac130003")

    private var bluetoothGatt: BluetoothGatt? = null

    // Objetos de sistema
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val bleScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    // Callback para gerenciar os resultados do Scan
    private var deviceListUpdateListener: ((BluetoothDevice) -> Unit)? = null

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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Associa ação ao botão btnSingIn
        val btnAddSensor = view.findViewById<FrameLayout>(R.id.btn_add_sensor)
        btnAddSensor.setOnClickListener {

            val dialog = SensorSetupDialog(this)

            // Configuramos o listener ANTES de começar o scan
            deviceListUpdateListener = @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) { device ->
                // Esta função deve ser criada dentro do seu SensorSetupDialog.kt
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                }
                dialog.addDeviceToList(device) {
                    // O que acontece ao clicar no dispositivo da lista:
                    stopBleScan()
                    connectToDevice(device)
                    dialog.dismiss()
                }
            }

            dialog.show(parentFragmentManager, "SensorDialog")

            val btnLerSensor = view.findViewById<Button>(R.id.btn_sensor_read)
            btnLerSensor.setOnClickListener @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                }
                solicitarLeituraDoSensor()
            }

            // Agora verificamos permissões e iniciamos o scan
            checkBluetoothPermissionAndStart()


        }
    }
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        Log.d("BLE_LOG", "Iniciando conexão com: ${device.name}")
        // Garante que qualquer tentativa anterior seja limpa antes de nova conexão
        bluetoothGatt?.close()
        bluetoothGatt = null
        // false indica que queremos conectar diretamente agora, sem esperar
        bluetoothGatt = device.connectGatt(requireContext(), false, gattCallback)
    }
    private fun checkBluetoothPermissionAndStart() {
        // Definimos as permissões necessárias baseadas na versão do Android
        val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        // Verifica se TODAS as permissões do array já estão concedidas
        val allPermissionsGranted = bluetoothPermissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        when {
            allPermissionsGranted -> {
                startBleScan()
            }
            else -> {
                // Lança o pedido para o array de permissões definido acima
                requestBluetoothPermissionLauncher.launch(bluetoothPermissions)
            }
        }
    }

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Verifica se todas foram aceitas
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startBleScan() // Sua função para começar a busca
        } else {
            // Exiba uma mensagem: "Permissão negada. Não podemos buscar o sensor."
        }
    }
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Desconhecido"
            val deviceAddress = device.address
            // Aqui enviamos o dispositivo encontrado para o Dialog através do Listener
            deviceListUpdateListener?.invoke(device)
            Log.d(TAG, "Dispositivo Encontrado: Nome: $deviceName | MAC: $deviceAddress")
        }
        override fun onScanFailed(errorCode: Int) {
            // LOG: Caso ocorra algum erro no scan
            Log.e(TAG, "Erro no Scan: Código $errorCode")
        }
    }
    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        // Verifique se tem permissões antes!
        Log.d(TAG, "Iniciando o Scan Bluetooth...")
        bleScanner?.startScan(scanCallback)
    }
    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        Log.d(TAG, "Parando o Scan Bluetooth.")
        bleScanner?.stopScan(scanCallback)
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun solicitarLeituraDoSensor() {
        if (bluetoothGatt == null) {
            Log.e("BLE_LOG", "GATT não conectado!")
            return
        }

        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_READ_SENSOR)

        if (characteristic != null) {
            // Envia o pedido de leitura para o ESP32
            bluetoothGatt?.readCharacteristic(characteristic)
            Log.d("BLE_LOG", "Pedido de leitura enviado ao ESP32...")
        } else {
            Log.e("BLE_LOG", "Característica de leitura não encontrada!")
        }
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // A conexão aconteceu em Background...
                gatt.discoverServices()
                // ...então "pedimos licença" para a Main Thread para mexer na UI
                activity?.runOnUiThread {
                    updateUIConnected(gatt.device.name ?: "Sensor Daisy")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w("BLE_LOG", "Desconectado do servidor GATT.")

                // ESSENCIAL: Fechar e limpar o objeto
                gatt.close()
                if (gatt == bluetoothGatt) {
                    bluetoothGatt = null
                }
                activity?.runOnUiThread {
                    updateUIDisconnected()
                }
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE_LOG", "Serviços prontos. Aguardando comando do botão.")
            }
        }

        // ESTE É O MÉTODO QUE RECEBE A RESPOSTA DO SEU BOTÃO
        override fun onCharacteristicRead(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //Dado a natureza do BLE só consigo passa 23 bytes por emissão,
                //Então ara evitar complicações a resposta esperada foi comprimida
                //A respota tem entre 20-22 bytes
                // estrutura: "l,98,28,6.5,75"
                //l - leitura
                //98 - bateria
                //28 - temperatura
                //6.5 - ph
                //75 - indicie de incidendia de luz



                // Supondo que 'valorRecebido' seja "l,98,28,6.5,75"
                val valorRecebido = char.getStringValue(0)
                Log.d("BLE_DEBUG", "1. Raw Data recebido: '$valorRecebido'") // Ver se chegou algo

                if (valorRecebido == null) {
                    Log.e("BLE_DEBUG", "ERRO: Valor recebido é nulo!")
                    return
                }

                val partes = valorRecebido.split(",")
                Log.d("BLE_DEBUG", "2. Número de partes após split: ${partes.size}")

// Verificamos se é uma leitura (comando 'l') e se tem todos os dados
                if (partes.size >= 5 && partes[0] == "l") {
                    Log.i("BLE_DEBUG", "3. Protocolo 'l' detectado. Iniciando conversão...")
                    try {
                        val jsonFinal = JSONObject()
                        jsonFinal.put("sensor_name", "Daisy_Sensor_V2")

                        val bateria = partes[1].trim().toInt()
                        jsonFinal.put("battery", bateria)

                        val temp = partes[2].trim().toInt()
                        val soil = partes[3].trim().toDouble() // Mantendo Double para não perder precisão no log
                        val sun = partes[4].trim().toInt()

                        jsonFinal.put("temperature", temp)
                        jsonFinal.put("soil_ph", soil) // Se quiser Int no JSON, use .toInt()
                        jsonFinal.put("sunlight", sun)


                        val jsonString = jsonFinal.toString()
                        Log.d("BLE_DEBUG", "4. JSON gerado com sucesso: $jsonString")

                        activity?.runOnUiThread {
                            Log.d("BLE_DEBUG", "5. Disparando navegação para ReadingsFragment")
                            navegarParaReadings(jsonString)
                        }

                    } catch (e: Exception) {
                        Log.e("BLE_DEBUG", "ERRO na conversão: ${e.message}")
                        e.printStackTrace()
                    }
                } else {
                    Log.w("BLE_DEBUG", "AVISO: Dados ignorados. Prefixo: ${partes.getOrNull(0)}, Tamanho: ${partes.size}")
                }




            }
        }
    }
    private fun navegarParaReadings(dadosJson: String) {
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
    }
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
            // Garante que o GATT seja fechado e o objeto zerado
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            bluetoothGatt?.close()
            bluetoothGatt = null
            // Para o scan caso ele ainda esteja ativo
            stopBleScan()
        } catch (e: Exception) {
            Log.e("BLE_LOG", "Erro ao limpar recursos BLE no onDestroy: ${e.message}")
        }
    }

    override fun onDestroyDialog() {
        try {
            // Garante que o GATT seja fechado e o objeto zerado
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
            }
            bluetoothGatt?.close()
            bluetoothGatt = null
            // Para o scan caso ele ainda esteja ativo
            stopBleScan()
        } catch (e: Exception) {
            Log.e("BLE_LOG", "Erro ao limpar recursos BLE no onDestroy: ${e.message}")
        }
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