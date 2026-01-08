package com.GoToExpress.daisyapp

import PlantRequest
import CreatePlantResponse
import EditPlantRequest
import EditPlantResponse
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

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
    private var plantId: Int = -1
    private var plantName: String? = null
    private var plantSpecie: String? = null
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
            plantId = it.getInt("EXTRA_ID")
            plantName = it.getString("EXTRA_NAME")
            plantSpecie = it.getString("EXTRA_SPECIE")
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
            Toast.makeText(requireContext(), R.string.camera_permission_error, Toast.LENGTH_SHORT).show()
        }
    }

    fun ImageView.getBitmap(): Bitmap? {
        return (drawable as? BitmapDrawable)?.bitmap
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        ivPreview = view.findViewById(R.id.add_plant_camera_icon)
        val cardPhoto = view.findViewById<View>(R.id.cardPhoto)
        val btnSave = view.findViewById<Button>(R.id.save_plant_btn)
        val etPlantName = view.findViewById<EditText>(R.id.etPlantName)
        val etPlantSpecie = view.findViewById<EditText>(R.id.etNickname)

        etPlantName.setText(plantName)
        etPlantSpecie.setText(plantSpecie)
        if(plantId!=-1){
            val bitmap = ImageStorageManager.getImage(requireContext(), plantId)

            if (bitmap != null) {
                // Se a foto existir, carrega o Bitmap
                ivPreview.setImageBitmap(bitmap)
            }
        }
        if(plantId ==-2){
            plantBitmap = ivPreview.getBitmap()
        }

        // Clique no Card para abrir a câmera
        cardPhoto.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                checkCameraPermissionAndOpen()
            } catch (e: ActivityNotFoundException) {
                // Tratar erro caso não haja app de câmera
            }
        }

        // Clique no botão Salvar
        btnSave.setOnClickListener {
            val name = etPlantName.text.toString()
            val specie = etPlantSpecie.text.toString()
            if(plantId>-1){
                //editPlant
                editPlant(plantId,name,specie)

            }
            else{
                if (plantBitmap != null && name.isNotEmpty()) {
                    // AQUI VOCÊ TEM ACESSO À FOTO (plantBitmap) E AO NOME
                    savePlant(name, specie,plantBitmap!!,requireContext())
                } else {
                    Toast.makeText(context, R.string.plant_form_fillerror, Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun savePlant(name: String,speecie:String, foto: Bitmap, context: Context) {
        // Lógica para salvar no Banco de Dados ou enviar para API
        sendAddPlantResquest(name,speecie){ idRetornado ->
            if (idRetornado != null) {
                // Se o ID existir, salva a foto usando o ID como nome
                val sucesso = ImageStorageManager.saveImage( context,idRetornado, foto)

                if (sucesso) {
                    // Feedback de sucesso (Ex: Toast ou navegar para outra tela)
                    Toast.makeText(context, R.string.plant_form_add_sucess, Toast.LENGTH_SHORT).show()
                    //parentFragmentManager.popBackStack()

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainerView3, YardFragment()) // R.id.fragment_container é o ID do container na AuthActivity
                        .commit()
                }
            } else {
                // Tratar erro: A API falhou em gerar um ID
                Toast.makeText(context, R.string.plant_form_add_error, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun editPlant(id: Int, name: String, specie: String) {
        sendEditResquest(id, name, specie) { sucesso ->
            if (sucesso == true) {
                // Se o texto foi editado com sucesso, verificamos se há uma nova foto para salvar
                plantBitmap?.let { novoBitmap ->
                    ImageStorageManager.saveImage(requireContext(), id, novoBitmap)
                }
                Toast.makeText(context, R.string.plant_form_edit_sucess, Toast.LENGTH_SHORT).show()

                // Retorna para o YardFragment (a lista)
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(context, R.string.api_form_plant_servererror, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            takePictureLauncher.launch(takePictureIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.camera_permission_error, Toast.LENGTH_SHORT).show()
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

    private fun sendAddPlantResquest(name: String, specie: String, onResult: (Int?) -> Unit) {
        val plantRequest = PlantRequest(name, specie, "Descrição opcional")
        val token = SessionManager.fetchAuthToken(requireContext())

        if (token != null) {
            RetrofitClient.instance.createPlant("Bearer $token", plantRequest)
                .enqueue(object : Callback<CreatePlantResponse> {
                    override fun onResponse(call: Call<CreatePlantResponse>, response: Response<CreatePlantResponse>) {
                        if (response.isSuccessful) {
                            val newId = response.body()?.id
                            //Toast.makeText(context, R.string.plant_form_edit_sucess, Toast.LENGTH_SHORT).show()
                            onResult(newId) // "Retorna" o ID para quem chamou
                        } else {
                            onResult(null)
                        }
                    }

                    override fun onFailure(call: Call<CreatePlantResponse>, t: Throwable) {
                        onResult(null)
                    }
                })
        } else {
            onResult(null)
        }
    }

    private fun sendEditResquest(id: Int,name: String, specie: String, onResult: (Boolean?) -> Unit) {
        Log.d("DEBUG_EDIT", "Iniciando Edição - ID: $id, Nome: $name, Especie: $specie")
        val editplantRequest = EditPlantRequest(id,name, specie, "Descrição opcional")
        val token = SessionManager.fetchAuthToken(requireContext())

        if (token != null) {
            Log.d("DEBUG_EDIT", "Token encontrado: ${token.take(10)}...") // Loga apenas o início do token por segurança
            RetrofitClient.instance.editPlant("Bearer $token", editplantRequest)
                .enqueue(object : Callback<EditPlantResponse> {
                    override fun onResponse(call: Call<EditPlantResponse>, response: Response<EditPlantResponse>) {
                        if (response.isSuccessful) {
                            val sucess = response.body()?.success
                            Log.d("DEBUG_EDIT", "Sucesso no Servidor! Resposta: $sucess")
                            //Toast.makeText(context, R.string.plant_form_edit_sucess, Toast.LENGTH_SHORT).show()
                            onResult(sucess) // "Retorna" o ID para quem chamou
                        } else {
                            val erroCorpo = response.errorBody()?.string()
                            val codigoErro = response.code()
                            Log.e("DEBUG_EDIT", "Erro no Servidor - Código: $codigoErro")
                            Log.e("DEBUG_EDIT", "Corpo do Erro: $erroCorpo")

                            Toast.makeText(context, "${R.string.api_form_plant_servererror} ($codigoErro)", Toast.LENGTH_SHORT).show()
                            onResult(null)                        }
                    }

                    override fun onFailure(call: Call<EditPlantResponse>, t: Throwable) {
                        onResult(null)
                    }
                })
        } else {
            onResult(null)
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