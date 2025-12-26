package com.example.daisyapp

import IdentifyPlantResponse
import android.app.Activity
import android.content.ActivityNotFoundException
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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import okhttp3.MultipartBody
import retrofit2.Call
import java.io.File
import java.io.FileOutputStream

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
                    Log.e("IdentifyFragment", getString(R.string.erro_no_servidor_c_digo)+response.code()+" - "+errorBody)

                    Toast.makeText(context,
                        getString(R.string.erro_na_identifica_o), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<IdentifyPlantResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(context,
                    getString(R.string.falha_na_conex_o)+t.message, Toast.LENGTH_SHORT).show()
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

    fun ImageView.getBitmap(): Bitmap? {
        return (drawable as? BitmapDrawable)?.bitmap
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar a referência do ImageView
        ivPreview = view.findViewById(R.id.Identify_photo_card)
        tvCommonName = view.findViewById(R.id.tvCommonName)
        tvSpecies = view.findViewById(R.id.tvSpecies)
        progressBar = view.findViewById(R.id.progressBar)
        score = view.findViewById<TextView>(R.id.tvScore)


        val btnSave = view.findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener {

            val bitmap = ivPreview.getBitmap()

            if (bitmap != null && tvCommonName.text.toString()!="" && tvSpecies.text.isNotEmpty()) {
                // 2. Chamar o seu Manager de salvamento
                ImageStorageManager.saveImage(requireContext(), -2, bitmap)
                val bundle = Bundle().apply {
                    putInt("EXTRA_ID", -2)
                    putString("EXTRA_NAME", tvCommonName.text.toString())
                    putString("EXTRA_SPECIE",tvSpecies.text.toString() )
                }

                // 2. Criar a instância do fragmento de destino
                val fragmentDestino = PlantFormFragment()
                fragmentDestino.arguments = bundle

                // 3. Realizar a transação (Trocar de tela)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainerView3, fragmentDestino) // Certifique-se que o ID é o do container do seu layout principal
                    .addToBackStack(null) // Adiciona à pilha para o botão 'voltar' funcionar
                    .commit()
            } else {
                Toast.makeText(context,
                    getString(R.string.erro_ao_salvar_planta), Toast.LENGTH_SHORT).show()
            }

        }
        val btnShare = view.findViewById<MaterialButton>(R.id.btnShare)

        btnShare.setOnClickListener {
            compartilharFoto()
        }
        // 2. Chamar a verificação de permissão e abertura da câmera automaticamente
        if (plantBitmap == null){
            checkCameraPermissionAndOpen()

        }

    }
    private fun compartilharFoto() {
        // 1. Garantir que temos um Bitmap
        // 1. Pega o drawable do ImageView
        val drawable = ivPreview.drawable

        // CONDIÇÃO DE SEGURANÇA: Se não houver imagem, avisa o usuário e sai da função
        if (drawable == null) {
            Toast.makeText(requireContext(),
                getString(R.string.none_picture_were_taken_to_share), Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Garantir que temos um Bitmap (Convertendo se necessário)
        val bitmap = if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            // Agora é seguro acessar intrinsicWidth/Height porque verificamos o null acima
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

            val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(b)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            b
        }

        if (bitmap == null) {
            Toast.makeText(requireContext(), getString(R.string.imagem_inv_lida), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 2. Criar a pasta e o arquivo no cache
            val imagesFolder = File(requireContext().cacheDir, "images")
            imagesFolder.mkdirs() // Cria a pasta se não existir
            val file = File(imagesFolder, "planta_compartilhada.png")

            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            // 3. Obter a URI via FileProvider
            val contentUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider", // Deve bater com o Manifest
                file
            )

            // 4. Criar o Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, "Olha a minha planta") // Texto opcional
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // ESSENCIAL
            }

            startActivity(Intent.createChooser(shareIntent, "Compartilhar via"))

        } catch (e: Exception) {
            Log.e("SHARE_ERROR", "Erro ao compartilhar: ${e.message}")
            Toast.makeText(requireContext(),
                getString(R.string.erro_ao_compartilhar_imagem), Toast.LENGTH_SHORT).show()
        }
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