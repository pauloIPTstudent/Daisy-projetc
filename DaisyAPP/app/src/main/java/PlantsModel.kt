data class PlantResponse( //Getplants
    val plants: List<Plant> // O nome "plants" deve ser igual ao que aparece no JSON
)
data class Plant(
    val id: Int,
    val name: String,
    val specie: String,
    // Adicione outros campos que a API devolve (ex: image_url, description)
)
// O que você envia para a API \\Create
data class PlantRequest(
    val name: String,
    val specie: String,
    val description: String
)

// O que a API te devolve (201 Created) \\Create
data class CreatePlantResponse(
    val id: Int,
    val success: Boolean
)
//
data class EditPlantRequest(
    val id: Int,
    val name: String,
    val specie: String,
    val description: String
)
// O que a API devolve (201) \\Edit
data class EditPlantResponse(
    val success: Boolean
)