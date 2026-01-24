data class PlantResponse( //Getplants
    val plants: List<Plant> // O nome "plants" deve ser igual ao que aparece no JSON
)
// Plant Model
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
// O que você envia para a API \\Edit
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
// O que você envia para a API \\Delete
data class DeletePlantRequest(
    val id:Int
)
// O que a API devolve (200) \\Delete
data class DeletePlatResponse(
    val success: Boolean
)
// O que você envia para a API \\Identify
data class IdentifyPlantResponse(
    val common_name: String,
    val species: String,
    val score: Float
)

// Weather API
data class WeatherRequest(
    val lat: Double,
    val lon: Double
)
// O que a API devolve \\Weather
data class WeatherResponse(
    val celsius: Float,
    val cidade: String,
    val tempo_principal: String
)




