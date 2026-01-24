import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_TOKEN = "auth_token"

    // Função para obter as SharedPreferences encriptadas
    private fun getSharedPreferences(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    /*
     * Função para salvar o token de autenticação
     * @param context Contexto da aplicação
     * @param token Token de autenticação a ser salvo
     */
    fun saveAuthToken(context: Context, token: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    /*
     * Função para verificar se o token de autenticação existe
     * @param context Contexto da aplicação
     * @return true se o token existir, false caso contrário
     */
    fun hasToken(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        val token = prefs.getString(KEY_TOKEN, null)

        // Retorna true se o token existir (não for nulo), false caso contrário
        return token != null
    }
    /*
     * Função para buscar o token de autenticação
     * @param context Contexto da aplicação
     * @return Token de autenticação ou null se não existir
     */
    fun fetchAuthToken(context: Context): String? {
        val prefs = getSharedPreferences(context)
        Log.d("Token_DEBUG", "token : ${prefs.getString(KEY_TOKEN, null)}")
        return prefs.getString(KEY_TOKEN, null)
    }
    /*
     * Função para limpar o token de autenticação
     * @param context Contexto da aplicação
     */
    fun clearSession(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}