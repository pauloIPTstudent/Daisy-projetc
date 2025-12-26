package com.example.daisyapp

import IdentifyPlantResponse
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import retrofit2.Call

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [IdentifyFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class IdentifyFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var plantBitmap: Bitmap? = null
    private lateinit var ivPreview: ImageView // O ImageView dentro do seu CardView
    private lateinit var tvCommonName: TextView
    private lateinit var tvSpecies: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var score: TextView

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            plantBitmap = imageBitmap
            ivPreview.setImageBitmap(imageBitmap) // Mostra a foto no Card
            // Inicia o processo de identificação
            uploadImage(imageBitmap)
        }
    }
    // Método para enviar a imagem
    private fun uploadImage(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE

        // Converter Bitmap para ByteArray para o Retrofit
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        Log.d("IdentifyFragment", "Tamanho do arquivo: ${byteArray.size} bytes")
        val requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), byteArray)
        val body = MultipartBody.Part.createFormData("image", "plant.jpg", requestFile)


        // Chamar API (Certifique-se de ter sua instância do Retrofit pronta)
        RetrofitClient.instance.identifyPlant(body).enqueue(object : retrofit2.Callback<IdentifyPlantResponse> {
            override fun onResponse(call: Call<IdentifyPlantResponse>, response: retrofit2.Response<IdentifyPlantResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    val plant = response.body()
                    tvCommonName.text = "${plant?.common_name}"
                    tvSpecies.text = "${plant?.species}"
                    if(plant?.score!=null){score.text = "${plant.score*100}% Match"}

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("IdentifyFragment", "Erro no servidor: Código ${response.code()} - $errorBody")

                    Toast.makeText(context, "Erro na identificação", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<IdentifyPlantResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Falha na conexão: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
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
        return inflater.inflate(R.layout.fragment_identify, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar a referência do ImageView
        ivPreview = view.findViewById(R.id.Identify_photo_card)
        tvCommonName = view.findViewById(R.id.tvCommonName)
        tvSpecies = view.findViewById(R.id.tvSpecies)
        progressBar = view.findViewById(R.id.progressBar)
        score = view.findViewById<TextView>(R.id.tvScore)

        // 2. Chamar a verificação de permissão e abertura da câmera automaticamente
        checkCameraPermissionAndOpen()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment IdentifyFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            IdentifyFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}