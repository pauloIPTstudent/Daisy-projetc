// O que envias no POST
data class AuthRequest(
    val email: String,
    val password: String
)

// O que recebes (a tua resposta 200 OK)
data class AuthResponse(
    val token: String
)