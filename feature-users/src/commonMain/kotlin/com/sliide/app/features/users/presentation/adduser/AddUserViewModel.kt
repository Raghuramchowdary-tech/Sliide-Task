package com.sliide.app.features.users.presentation.adduser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.app.core.common.DomainError
import com.sliide.app.core.common.DomainResult
import com.sliide.app.features.users.domain.model.CreateUserRequest
import com.sliide.app.features.users.domain.model.Gender
import com.sliide.app.features.users.domain.usecase.AddUserUseCase
import com.sliide.app.features.users.domain.usecase.ValidateUserInputUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddUserViewModel(
    private val addUser: AddUserUseCase,
    private val validateInput: ValidateUserInputUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AddUserState())
    val state: StateFlow<AddUserState> = _state.asStateFlow()

    private val _effects = Channel<AddUserEffect>(Channel.BUFFERED)
    val effects: Flow<AddUserEffect> = _effects.receiveAsFlow()

    fun submit(name: String, email: String, gender: Gender) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        when (val validation = validateInput(trimmedName, trimmedEmail)) {
            is DomainResult.Failure -> {
                val error = validation.error
                if (error is DomainError.ValidationFailed) {
                    _state.update { it.copy(
                        validationErrors = error.violations.associate { v -> v.field to v.message },
                    )}
                }
                return
            }
            is DomainResult.Success -> {}
        }

        _state.update { it.copy(isSubmitting = true, validationErrors = emptyMap()) }
        viewModelScope.launch {
            val request = CreateUserRequest(name = trimmedName, email = trimmedEmail, gender = gender)
            when (val result = addUser(request)) {
                is DomainResult.Success -> _effects.send(AddUserEffect.Success)
                is DomainResult.Failure -> {
                    _state.update { it.copy(isSubmitting = false) }
                    if (result.error is DomainError.ValidationFailed) {
                        _state.update { it.copy(
                            validationErrors = (result.error as DomainError.ValidationFailed)
                                .violations.associate { v -> v.field to v.message },
                        )}
                    } else {
                        _effects.send(AddUserEffect.Error("Failed to add user"))
                    }
                }
            }
        }
    }
}
