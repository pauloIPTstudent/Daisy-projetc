import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    fun login(@Body request: AuthRequest): Call<AuthResponse>

    @POST("create_account")
    fun register(@Body request: AuthRequest): Call<AuthResponse>
}