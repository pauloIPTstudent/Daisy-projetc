package com.example.daisyapp

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PlantFormFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PlantFormFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var plantBitmap: Bitmap? = null
    private lateinit var ivPreview: ImageView // O ImageView dentro do seu CardView

    // 1. Registra o launcher para tirar a foto
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            plantBitmap = imageBitmap
            ivPreview.setImageBitmap(imageBitmap) // Mostra a foto no Card
        }
    }
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
        return inflater.inflate(R.layout.fragment_plant_form, container, false)
    }
    // Launcher para pedir a permissão de câmera
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Se o usuário aceitou agora, abre a câmera
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Permissão de câmera negada", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        ivPreview = view.findViewById(R.id.add_plant_camera_icon)
        val cardPhoto = view.findViewById<View>(R.id.cardPhoto)
        val btnSave = view.findViewById<Button>(R.id.save_plant_btn)
        val etPlantName = view.findViewById<EditText>(R.id.etPlantName)

        // 2. Clique no Card para abrir a câmera
        cardPhoto.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                checkCameraPermissionAndOpen()
            } catch (e: ActivityNotFoundException) {
                // Tratar erro caso não haja app de câmera
            }
        }

        // 3. Clique no botão Salvar
        btnSave.setOnClickListener {
            val name = etPlantName.text.toString()

            if (plantBitmap != null && name.isNotEmpty()) {
                // AQUI VOCÊ TEM ACESSO À FOTO (plantBitmap) E AO NOME
                salvarPlanta(name, plantBitmap!!)
            } else {
                Toast.makeText(context, "Preencha o nome e tire uma foto!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun salvarPlanta(nome: String, foto: Bitmap) {
        // Lógica para salvar no Banco de Dados ou enviar para API
    }
    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Câmera não encontrada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            // Caso 1: A permissão já foi concedida
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            // Caso 2: A permissão ainda não foi pedida ou foi negada antes
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PlantFormFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PlantFormFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}