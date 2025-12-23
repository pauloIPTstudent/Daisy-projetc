import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.daisyapp.R
import java.io.File

class PlantAdapter(private var plants: List<Plant>) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.plant_name)
        val nickname: TextView = view.findViewById(R.id.plant_nickname)
        val image: ImageView = view.findViewById(R.id.plant_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant_card, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        val context = holder.itemView.context
        Log.d("DEBUG_PLANT", "Plant Data $plant")

        holder.name.text = plant.name
        holder.nickname.text = plant.specie ?: "Sem apelido" // Trata o null da  API
        // Busca a imagem usando o ID da planta
        val bitmap = ImageStorageManager.getImage(context, plant.id)

        if (bitmap != null) {
            // 2. Se a foto existir, carrega o Bitmap
            holder.image.setImageBitmap(bitmap)
        } else {
            // 3. Se não existir (ou for a primeira vez), carrega a imagem padrão do projeto
            holder.image.setImageResource(R.drawable.home)
        }
    }

    override fun getItemCount() = plants.size


    // Esta é a função que o Fragment chamará
    fun updateData(newPlants: List<Plant>) {
        this.plants = newPlants
        notifyDataSetChanged()
    }
}