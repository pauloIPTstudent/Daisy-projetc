import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object ImageStorageManager {

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

    private fun getDirectory(context: Context): File {
        val configDir = File(context.filesDir, FOLDER_NAME)
        if (!configDir.exists()) configDir.mkdirs()
        return configDir
    }
}