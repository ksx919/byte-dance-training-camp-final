package com.rednote.ui.base

/**
 * 空ViewModel，用于不需要复杂业务逻辑的Fragment
 * 继承BaseViewModel以保持架构一致性
 */
class EmptyViewModel : BaseViewModel() {
    // 空实现，仅用于满足 BaseFragment 的泛型要求
}
