package com.rednote.ui.login

sealed interface RegisterUiEvent {
    object RegisterSuccess : RegisterUiEvent
}