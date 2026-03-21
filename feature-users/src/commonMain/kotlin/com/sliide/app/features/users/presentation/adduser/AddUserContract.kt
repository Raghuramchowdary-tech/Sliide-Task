package com.sliide.app.features.users.presentation.adduser

data class AddUserState(
    val validationErrors: Map<String, String> = emptyMap(),
    val isSubmitting: Boolean = false,
)

sealed interface AddUserEffect {
    data object Success : AddUserEffect
    data class Error(val message: String) : AddUserEffect
}
