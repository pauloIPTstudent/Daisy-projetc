package com.example.daisyapp

import Plant
import PlantAdapter
import PlantResponse
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [YardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class YardFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var plantAdapter: PlantAdapter

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
        return inflater.inflate(R.layout.fragment_yard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa o RecyclerView com uma lista vazia
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_plants)
        plantAdapter = PlantAdapter(emptyList())
        recyclerView.adapter = plantAdapter        // 2. Chame a função para carregar os dados da API
        val buttonSignIn = view.findViewById<Button>(R.id.btn_add_plant)
        buttonSignIn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView3, PlantFormFragment()) // R.id.fragment_container é o ID do container na AuthActivity
                .addToBackStack(null) // Permite que o utilizador volte ao carregar no botão "Retroceder"
                .commit()
        }
        loadPlants()

    }
    private fun setupRecyclerView(plantList: List<Plant>) {
        plantAdapter.updateData(plantList)
    }
    fun loadPlants() {
        // 1. Recuperar o token guardado
        val token = SessionManager.fetchAuthToken(requireContext())

        if (token != null) {
            // 2. Chamar a API passando "Bearer TOKEN"
            RetrofitClient.instance.getUserPlants("Bearer $token").enqueue(object : Callback<PlantResponse> {
                override fun onResponse(call: Call<PlantResponse>, response: Response<PlantResponse>) {
                    if (response.isSuccessful) {
                        val plantResponse = response.body()
                        val plants = plantResponse?.plants ?: emptyList()

                        // Chama a função para atualizar a interface na Main Thread
                        activity?.runOnUiThread {
                            setupRecyclerView(plants)
                        }
                        // Aqui tens a tua lista de plantas! Podes enviar para um RecyclerView
                        Log.d("API_GET", "Plantas encontradas: ${plants}")
                    } else if (response.code() == 401) {
                        // Token expirado ou inválido
                        Toast.makeText(context, "Sessão expirada. Faça login novamente.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
                    Log.e("API_GET", "Erro de rede: ${t.message}")
                }
            })
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment YardFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            YardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}