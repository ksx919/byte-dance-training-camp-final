package com.rednote.ui.login

sealed interface LoginUiEvent {
    object NavigateToHome : LoginUiEvent
}