package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rednote.databinding.FragmentPlaceholderBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

class PlaceholderFragment : BaseFragment<FragmentPlaceholderBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentPlaceholderBinding {
        return FragmentPlaceholderBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 默认显示 layout 中的文本
    }

    override fun initData() {
    }
}
