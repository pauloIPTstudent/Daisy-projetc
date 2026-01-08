import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
//classe singleton para gerir armazenamento de imagens de plantas
object ImageStorageManager {

    // Define o nome da pasta onde as imagens serão armazenadas.
    private const val FOLDER_NAME = "plant_images"

    /**
     * Salva o bitmap no armazenamento interno usando o ID da planta como nome.
     */
    fun saveImage(context: Context, plantId: Int, bitmap: Bitmap): Boolean {
        val filename = "plant_$plantId.jpg"
        val directory = getDirectory(context)
        val file = File(directory, filename )

        return try {
            FileOutputStream(file).use { out ->
                // Comprime o Bitmap para o formato JPEG com 90% de qualidade e o escreve no arquivo.
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d("DEBUG_PLANT", "Imagem salva com sucesso: $filename em ${context.filesDir}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Busca a imagem do armazenamento interno pelo ID.
     */
    fun getImage(context: Context, plantId: Int): Bitmap? {
        val directory = getDirectory(context)
        val file = File(directory, "plant_$plantId.jpg")
        Log.d("DEBUG_PLANT", "Tentando carregar: ${file.absolutePath} - Existe? ${file.exists()}")

        return if (file.exists()) {
            // Decodifica o arquivo de imagem em um objeto Bitmap.
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            Log.e("API_DEBUG_ImageStore", "file does not exists")
            null
        }
    }

    /**
     * Deleta a imagem (útil para quando uma planta for excluída).
     */
    fun deleteImage(context: Context, plantId: Int): Boolean {
        val directory = getDirectory(context)
        val file = File(directory, "plant_$plantId.jpg")
        return if (file.exists()) file.delete() else false
    }

    // metodo privado usado pelas outra funções para reutilizar codigo
    private fun getDirectory(context: Context): File {
        // Cria um caminho para a pasta "plant_images" dentro do diretório de arquivos do app.
        val configDir = File(context.filesDir, FOLDER_NAME)
        // Se a pasta não existe, cria a estrutura de diretórios.
        if (!configDir.exists()) configDir.mkdirs()
        return configDir
    }
}