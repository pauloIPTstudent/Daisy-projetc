package com.GoToExpress.daisyapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONObject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ReadingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ReadingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

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
        return inflater.inflate(R.layout.fragment_readings, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Recupera o JSON enviado pelo fragmento anterior
        val jsonString = arguments?.getString("dados_sensor")

        // LOG DE VERIFICAÇÃO INICIAL
        Log.d("READINGS_FRAGMENT", "--- INÍCIO DO ONVIEWCREATED ---")
        Log.d("READINGS_FRAGMENT", "Recebido no Bundle: $jsonString")

        jsonString?.let {
            try {
                val json = JSONObject(it)

                // Usamos requireView() para garantir que a busca seja na View atual do fragmento
                val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                val pbTemp = view.findViewById<ProgressBar>(R.id.tempProgressBar)
                val pbSoil = view.findViewById<ProgressBar>(R.id.soilProgressBar)
                val pbSun = view.findViewById<ProgressBar>(R.id.sunlightProgressBar)

                val resTemp = view.findViewById<TextView>(R.id.resume_temp)
                val resSoil = view.findViewById<TextView>(R.id.resume_soil)
                val resSun = view.findViewById<TextView>(R.id.resume_sunlight)

                // Extração de dados
                val temp = json.optInt("temperature")
                val soil = json.optDouble("soil_ph", 0.0)
                val sun = json.optInt("sunlight")
                val name = json.optString("sensor_name", "Sensor")

                // Aplicando na Interface
                tvTitle?.text = "🪴 $name"

                pbTemp?.progress = temp
                resTemp?.text = gerarResumo(R.string.label_temperature, temp, "°C", 15, 30)

                pbSoil?.progress = soil.toInt()
                resSoil?.text = gerarResumo(R.string.label_soil_ph, soil.toInt(), " pH", 5, 8)

                pbSun?.progress = sun
                resSun?.text = gerarResumo(R.string.label_sunlight, sun, "%", 30, 70)

                Log.i("READINGS_FRAGMENT", "UI Aplicada: Temp=$temp, Soil=$soil, Sun=$sun")
            } catch (e: Exception) {
                // Se houver erro, este Log vai te dizer exatamente o que é
                Log.e("READINGS_FRAGMENT", "Erro ao processar: ${e.message}")
            }
        }
    }

    /**
     * Função de Regra: Define se o nível está baixo, médio ou alto
     * com base em limites (thresholds) customizáveis.
     */
    private fun gerarResumo(nomeResId: Int, valor: Int, unidade: String, limiteBaixo: Int, limiteAlto: Int): String {
        // 1. Define qual ID de string de "nível" usar
        val nivelResId = when {
            valor < limiteBaixo -> R.string.level_low
            valor in limiteBaixo..limiteAlto -> R.string.level_ideal
            else -> R.string.level_high
        }

        // 2. Busca as traduções
        val nomeTraduzido = getString(nomeResId)
        val nivelTraduzido = getString(nivelResId)

        // 3. Monta a frase final usando placeholders (%s para texto, %d para números)
        return getString(R.string.reading_summary, nomeTraduzido, valor, unidade, nivelTraduzido)
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ReadingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ReadingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}