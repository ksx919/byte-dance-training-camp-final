package com.rednote.ui.main.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rednote.databinding.FragmentMessageBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

class MessageFragment : BaseFragment<FragmentMessageBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMessageBinding {
        return FragmentMessageBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // TODO: 初始化消息页面的UI组件
    }

    override fun initObservers() {
        // TODO: 观察ViewModel的数据变化
    }

    override fun initData() {
        // TODO: 加载消息数据
    }
}