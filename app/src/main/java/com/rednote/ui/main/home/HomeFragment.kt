package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rednote.databinding.FragmentHomeBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

class HomeFragment : BaseFragment<FragmentHomeBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
    }

    override fun initObservers() {
    }

    override fun initData() {
    }
}