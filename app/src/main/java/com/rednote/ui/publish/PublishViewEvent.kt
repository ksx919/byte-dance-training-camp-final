package com.rednote.ui.publish

// 定义界面事件
sealed interface PublishViewEvent {
    data class ShowToast(val message: String) : PublishViewEvent
    data class OpenGallery(val maxSelection: Int) : PublishViewEvent
    object PrepareCamera : PublishViewEvent
    object Finish : PublishViewEvent
}
