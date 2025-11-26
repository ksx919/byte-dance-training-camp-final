package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rednote.databinding.FragmentHomeTopBarBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

class HomeTopBarFragment : BaseFragment<FragmentHomeTopBarBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeTopBarBinding {
        return FragmentHomeTopBarBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 默认选中第二个Tab（关注）
        binding.tabLayout.getTabAt(1)?.select()
        
        // TODO: 添加Tab选择监听
    }

    override fun initData() {
        // TODO: 根据需要加载数据
    }
}