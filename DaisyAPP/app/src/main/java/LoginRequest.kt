// O que envias no POST
data class LoginRequest(
    val email: String,
    val password: String
)

// O que recebes (a tua resposta 200 OK)
data class LoginResponse(
    val token: String
)