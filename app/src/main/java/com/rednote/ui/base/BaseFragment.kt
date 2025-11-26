package com.rednote.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    protected abstract val viewModel: VM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 初始化 UI
        initViews()

        // 2. 注册观察者
        initObservers()

        // 3. 自动监听 BaseViewModel 的通用状态 (Loading, Toast)
        observeBaseEvents()

        // 4. 加载数据 (可选)
        initData()
    }

    // 核心：在 onDestroyView 中置空 binding，防止内存泄漏 (Android 面试必问点)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- 抽象方法，子类必须实现 ---
    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB
    abstract fun initViews()

    // --- 可选重写的方法 ---
    open fun initObservers() {}
    open fun initData() {}

    // --- 私有方法：统一处理 Loading 和 Toast ---
    private fun observeBaseEvents() {
        // 使用 repeatOnLifecycle 确保只在 Fragment 可见时收集 Flow (省电、安全)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 监听 Loading
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) showLoadingUI() else hideLoadingUI()
                    }
                }
                // 监听 Toast
                launch {
                    viewModel.toastEvent.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // 具体的 Loading UI 实现，子类可以重写来自定义 (比如显示一个 ProgressBar 弹窗)
    open fun showLoadingUI() {
        // 默认实现：如果有通用的 LoadingDialog 可以在这里 show
    }

    open fun hideLoadingUI() {
        // 默认实现：关闭 LoadingDialog
    }
}
