package com.example.daisyapp

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ViewFlipper
import androidx.fragment.app.DialogFragment
import com.example.daisyapp.R

class SensorSetupDialog : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_set_sensor, container, false)
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
}