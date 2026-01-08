package com.example.daisyapp

import Plant
import PlantAdapter.OnItemClickListener
import android.Manifest
import android.bluetooth.BluetoothDevice
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
import com.example.daisyapp.R

class SensorSetupDialog(private val listener: OnDestroyListener) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_set_sensor, container, false)
    }
    interface OnDestroyListener {
        fun onDestroyDialog()
    }

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
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun addDeviceToList(device: BluetoothDevice, onClick: () -> Unit) {
        val container = view?.findViewById<LinearLayout>(R.id.list_container)
        val deviceName = device.name ?: "Desconhecido"

        // Evita duplicar o mesmo sensor na lista
        if (container?.findViewWithTag<View>(device.address) == null) {
            val btn = Button(requireContext()).apply {
                text = "$deviceName\n${device.address}"
                tag = device.address
                setOnClickListener { onClick() }
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
        listener.onDestroyDialog()
    }
}