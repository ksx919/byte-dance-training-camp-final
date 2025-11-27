package com.rednote.ui.main.mine

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rednote.databinding.FragmentMeBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

class MeFragment : BaseFragment<FragmentMeBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMeBinding {
        return FragmentMeBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // TODO: 初始化个人中心的UI组件
    }

    override fun initObservers() {
        // TODO: 观察ViewModel的数据变化
    }

    override fun initData() {
        // TODO: 加载用户数据
    }
}