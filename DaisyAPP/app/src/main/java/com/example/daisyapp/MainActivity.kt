package com.example.daisyapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var icons: List<ImageView>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isLogged()) {
            gotoAuth()
        }
        // Inicializa a lista de ícones
        icons = listOf(
            findViewById(R.id.nav_home),
            findViewById(R.id.nav_diagnose),
            findViewById(R.id.nav_camera),
            findViewById(R.id.nav_yard),
            findViewById(R.id.nav_about)
        )

        // Configura o clique para cada um
        icons.forEach { icon ->
            icon.setOnClickListener {
                handleNavigation(it.id)
            }
        }

        // Carrega o fragmento inicial (Home)
        if (savedInstanceState == null) {
            handleNavigation(R.id.nav_home)
        }
    }

    fun handleNavigation(selectedId: Int) {
        val fragment = when (selectedId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_diagnose -> DiagnoseFragment()
            R.id.nav_camera -> IdentifyFragment()
            R.id.nav_yard -> YardFragment()
            R.id.nav_about -> AboutFragment()
            else -> HomeFragment()
        }

        // Troca o Fragmento
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView3, fragment)
            .commit()

        // Atualiza as cores dos ícones
        updateIconColors(selectedId)
    }

    private fun updateIconColors(selectedId: Int) {
        icons.forEach { icon ->
            if (icon.id == selectedId) {
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#8BC34A")) // Verde
            } else {
                icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#BDBDBD")) // Cinza
            }
        }
    }

    private fun gotoAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish() // impede voltar para a MainActivity
    }
    private fun isLogged(): Boolean {
        return SessionManager.hasToken(this)
    }

}