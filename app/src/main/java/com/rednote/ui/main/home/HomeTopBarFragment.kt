package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.rednote.databinding.FragmentHomeTopBarBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator

class HomeTopBarFragment : BaseFragment<FragmentHomeTopBarBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentHomeTopBarBinding {
        return FragmentHomeTopBarBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        val titles = listOf("关注", "发现", "同城")
        
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = titles.size

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    1 -> ContentFragment() // 发现页
                    else -> PlaceholderFragment() // 关注、同城页
                }
            }
        }

        // 禁用预加载，或者根据需要设置
        binding.viewPager.offscreenPageLimit = 1

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        // 默认选中第二个Tab（发现），因为它是主要的Feed流
        // 注意：TabLayoutMediator attach后，ViewPager会自动同步Tab，所以设置ViewPager的currentItem即可
        binding.viewPager.setCurrentItem(1, false)
    }
}