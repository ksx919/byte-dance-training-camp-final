package com.rednote.ui.publish

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rednote.databinding.ActivityAddBinding
import com.rednote.ui.base.BaseActivity

class AddActivity : BaseActivity<ActivityAddBinding>() {
    
    override fun getViewBinding(): ActivityAddBinding {
        return ActivityAddBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 设置Edge-to-Edge显示
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // TODO: 添加发布页面的UI初始化逻辑
    }

    override fun initData() {
        // TODO: 加载发布页面需要的数据
    }
}