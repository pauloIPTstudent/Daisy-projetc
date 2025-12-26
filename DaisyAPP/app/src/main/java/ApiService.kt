import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ApiService {
    @POST("login")
    fun login(@Body request: AuthRequest): Call<AuthResponse>

    @POST("create_account")
    fun register(@Body request: AuthRequest): Call<AuthResponse>

    @GET("listuserplants")
    fun getUserPlants(
        @Header("Authorization") token: String
    ): Call<PlantResponse>

    @POST("createplant")
    fun createPlant(
        @Header("Authorization") token: String,
        @Body request: PlantRequest
    ): Call<CreatePlantResponse>

    @PUT("editplant")
    fun editPlant(
        @Header("Authorization") token: String,
        @Body request: EditPlantRequest
    ): Call<EditPlantResponse>


    @Multipart
    @POST("identify")
    fun identifyPlant(
        @Part image: MultipartBody.Part
    ): Call<IdentifyPlantResponse>
}