data class PlantResponse(
    val plants: List<Plant> // O nome "plants" deve ser igual ao que aparece no JSON
)
data class Plant(
    val id: Int,
    val name: String,
    val specie: String,
    // Adicione outros campos que a API devolve (ex: image_url, description)
)
// O que você envia para a API
data class CreatePlantRequest(
    val name: String,
    val specie: String,
    val description: String
)

// O que a API te devolve (201 Created)
data class CreatePlantResponse(
    val id: Int,
    val success: Boolean
)