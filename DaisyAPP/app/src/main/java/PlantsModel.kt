data class PlantResponse(
    val plants: List<Plant> // O nome "plants" deve ser igual ao que aparece no JSON
)
data class Plant(
    val id: Int,
    val name: String,
    val specie: String,
    // Adicione outros campos que a API devolve (ex: image_url, description)
)
