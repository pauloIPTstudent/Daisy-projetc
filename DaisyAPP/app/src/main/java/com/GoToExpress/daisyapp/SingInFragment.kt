package com.GoToExpress.daisyapp

import AuthRequest
import AuthResponse
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback
import RetrofitClient
import android.content.Intent

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SingInFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SingInFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_sing_in, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Associa ação ao botão btnSingIn
        val buttonSignIn = view.findViewById<Button>(R.id.btnSignIn)
        buttonSignIn.setOnClickListener {

            // forma o 'request' com os dados dos campos de email e password
            val email = view.findViewById<TextView>(R.id.etEmail).text.toString()
            val password = view.findViewById<TextView>(R.id.etPassword).text.toString()
            val request = AuthRequest(email, password)

            // faz um pedido de login com os parametros recolhidos anteriormente
            RetrofitClient.instance.login(request).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val token = response.body()?.token
                        // SUCESSO: Guarda o token e muda de ecrã
                        //Log.d("API_DEBUG", "Token recebido: $token")
                        if (token != null) {
                            // Guardar o token no nosso Singleton de sessão
                            SessionManager.saveAuthToken(requireContext(), token)
                            //Log.e("API_DEBUG", "token: ${token}")
                            Toast.makeText(context, R.string.api_login_success, Toast.LENGTH_SHORT).show()

                            // Ir para a MainActivity
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    } else {
                        // ERRO: Credenciais inválidas (ex: 401)
                        Toast.makeText(context, R.string.api_login_error400, Toast.LENGTH_SHORT).show()
                        val errorBody = response.errorBody()?.string()
                        Log.e("API_ERROR", "Código: ${response.code()} - Erro: $errorBody")                    }
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    // ERRO DE REDE: Sem internet ou servidor offline
                    Toast.makeText(context, R.string.api_login_conectivity_error, Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "Erro: ${t.message}")
                }
            })
        }
        //goto singup
        val buttonSignUp = view.findViewById<TextView>(R.id.tvSignUp)
        buttonSignUp.setOnClickListener {
            //troca o fragemento no fragmentConteinerView (AuthActivityview) para a pagina/fragmento de update
            parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, SingUpFragment()) // R.id.fragment_container é o ID do container na AuthActivity
            //.addToBackStack(null) // Permite que o utilizador volte ao Login ao carregar no botão "Retroceder"
            .commit()

        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SingInFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SingInFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }

            }
    }
}