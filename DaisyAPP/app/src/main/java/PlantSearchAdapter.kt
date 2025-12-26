import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.daisyapp.R

class PlantSearchAdapter(
    private val onPlantSelected: (Plant) -> Unit
) : RecyclerView.Adapter<PlantSearchAdapter.PlantViewHolder>() {

    private var plants = listOf<Plant>()

    fun setData(newPlants: List<Plant>) {
        this.plants = newPlants
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant_search, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.nameTextView.text = plant.name
        holder.itemView.setOnClickListener { onPlantSelected(plant) }
    }

    override fun getItemCount() = plants.size

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.plant_name)
    }
}