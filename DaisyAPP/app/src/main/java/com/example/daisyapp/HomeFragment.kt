package com.example.daisyapp

import Plant
import PlantResponse
import PlantSearchAdapter
import WeatherRequest
import WeatherResponse
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var plantAdapter: PlantSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<ImageView>(R.id.home_logout).setOnClickListener{
            //apaga key
            SessionManager.clearSession(requireContext())
            // 2. Cria a Intent para a MainActivity
            val intent = Intent(requireContext(), MainActivity::class.java)

            // 3. Limpa a pilha de atividades para que o usuário não consiga voltar
            // para a tela anterior ao apertar o botão "Voltar" do celular
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            // 4. (Opcional) Finaliza a instância atual da atividade
            activity?.finish()
        }
        /**/// Referência para o card de Diagnose
        // Clique no Card Diagnose
        view.findViewById<CardView>(R.id.card_diagnose).setOnClickListener {
            (activity as? MainActivity)?.handleNavigation(R.id.nav_diagnose)
        }//

        // Clique no Card Identity
        view.findViewById<CardView>(R.id.card_identity).setOnClickListener {
            (activity as? MainActivity)?.handleNavigation(R.id.nav_camera)
        }//

        // Clique no Card My Yard
        view.findViewById<CardView>(R.id.card_yard).setOnClickListener {
            (activity as? MainActivity)?.handleNavigation(R.id.nav_yard)
        }// */





        val searchBar = view.findViewById<EditText>(R.id.search_bar)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_search_results)
        plantAdapter = PlantSearchAdapter { selectedPlant ->
            // O que acontece quando clica na planta da lista:
            searchBar.setText(selectedPlant.name) // Preenche a barra com o nome
            recyclerView.visibility = View.GONE  // Esconde a lista flutuante

            // Chame sua função de detalhes ou navegação aqui
            goToPlant(selectedPlant)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = plantAdapter


        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.d("SearchDeubug", "pesquisando")

                val query = s.toString().trim()
                Log.d("SearchDeubug", "query${query}")

                if (query.length >= 3) {
                    performSearch(query)
                    recyclerView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })











        // 1. Inicialize IMEDIATAMENTE ao criar a view
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        checkLocationPermissionAndGetWeather()
    }

    private fun checkLocationPermissionAndGetWeather() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                getLastLocation()
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Aqui chamamos a sua função de clima com os dados reais!
                // Nota: Convertemos para Long se o seu backend exigir,
                // mas normalmente APIs de clima usam Double.
                loadWeatherData(location.latitude.toDouble(), location.longitude.toDouble())
            } else {
                Toast.makeText(context, "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadWeatherData(lat: Double, lon: Double) {
        val request = WeatherRequest(lat, lon)

        RetrofitClient.instance.getWeather(request).enqueue(object : retrofit2.Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: retrofit2.Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()

                    // Preenchendo os campos com os dados da API
                    weather?.let {
                        // 1. Status do Tempo (ex: Sunny)
                        view?.findViewById<TextView>(R.id.tv_weather_status)?.text = it.tempo_principal

                        // 2. Temperatura (adicionando o símbolo de grau)
                        // Usamos String.format para remover casas decimais extras
                        val tempFormatted = "${it.celsius.toInt()}°"
                        view?.findViewById<TextView>(R.id.tv_temperature)?.text = tempFormatted

                        // 3. Data e Localização
                        // Como a API retorna a cidade, mantemos a data atual fixa ou via Calendar
                        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                        val currentDate = sdf.format(Date()).uppercase() // Formata e coloca em maiúsculas

                        val locationText = "$currentDate\n${it.cidade}"
                        view?.findViewById<TextView>(R.id.tv_date_location)?.text = locationText
                    }
                } else {
                    Log.e("WeatherError", "Erro na resposta: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.e("WeatherError", "Falha na rede: ${t.message}")
            }
        })
    }

    private fun performSearch(query: String) {
        val token = SessionManager.fetchAuthToken(requireContext())
        if (token != null) {
            RetrofitClient.instance.searchUserPlants("Bearer $token", query).enqueue(object : Callback<PlantResponse> {
                override fun onResponse(call: Call<PlantResponse>, response: Response<PlantResponse>) {
                    if (response.isSuccessful) {
                        val plants = response.body()?.plants ?: emptyList()

                        // Pegar apenas as primeiras 5 plantas conforme solicitado
                        val topFive = plants.take(5)
                        Log.d("SearchDebug", topFive.toString())
                        // Atualiza a lista no Adapter
                        plantAdapter.setData(topFive)
                    }
                }

                override fun onFailure(call: Call<PlantResponse>, t: Throwable) {
                    Log.e("Search", "Erro na busca: ${t.message}")
                }
            })
        }
    }

    private fun goToPlant(plant: Plant) {
        val bundle = Bundle().apply {
            putInt("EXTRA_ID", plant.id)
            putString("EXTRA_NAME", plant.name)
            putString("EXTRA_SPECIE", plant.specie)
        }

        // 2. Criar a instância do fragmento de destino
        val fragmentDestino = PlantFormFragment()
        fragmentDestino.arguments = bundle

        // 3. Realizar a transação (Trocar de tela)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView3, fragmentDestino) // Certifique-se que o ID é o do container do seu layout principal
            .addToBackStack(null) // Adiciona à pilha para o botão 'voltar' funcionar
            .commit()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic fun newInstance(param1: String, param2: String) =
                HomeFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}