package com.rednote.ui.main

import android.content.Intent
import android.graphics.Typeface
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.amap.api.location.AMapLocationClient
import com.rednote.R
import com.rednote.databinding.ActivityMainBinding
import com.rednote.ui.base.BaseActivity
import com.rednote.ui.main.mine.MeFragment
import com.rednote.ui.main.message.MessageFragment
import com.rednote.ui.main.weather.WeatherFragment
import com.rednote.ui.main.home.HomeFragment
import com.rednote.ui.publish.AddActivity

class MainActivity : BaseActivity<ActivityMainBinding>() {
    
    private var currentFragment: Fragment? = null

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun initView() {
        // 初始化高德地图隐私合规
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        // 设置Edge-to-Edge显示
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation()
    }

    override fun initData() {
        // 默认显示首页
        switchToFragment(HomeFragment(), "home")
        updateBottomNavigationState("home")
    }

    private fun updateBottomNavigationState(selectTab: String) {
        // 重置所有按钮状态
        binding.tvHome.setTextColor(getColor(R.color.gray))
        binding.tvWeather.setTextColor(getColor(R.color.gray))
        binding.tvMessage.setTextColor(getColor(R.color.gray))
        binding.tvMe.setTextColor(getColor(R.color.gray))

        binding.tvHome.typeface = Typeface.DEFAULT
        binding.tvWeather.typeface = Typeface.DEFAULT
        binding.tvMessage.typeface = Typeface.DEFAULT
        binding.tvMe.typeface = Typeface.DEFAULT

        // 设置选中状态
        when (selectTab) {
            "home" -> {
                binding.tvHome.setTextColor(getColor(R.color.black))
                binding.tvHome.typeface = Typeface.DEFAULT_BOLD
            }
            "weather" -> {
                binding.tvWeather.setTextColor(getColor(R.color.black))
                binding.tvWeather.typeface = Typeface.DEFAULT_BOLD
            }
            "message" -> {
                binding.tvMessage.setTextColor(getColor(R.color.black))
                binding.tvMessage.typeface = Typeface.DEFAULT_BOLD
            }
            "me" -> {
                binding.tvMe.setTextColor(getColor(R.color.black))
                binding.tvMe.typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.tvHome.setOnClickListener {
            switchToFragment(HomeFragment(), "home")
            updateBottomNavigationState("home")
        }
        binding.tvWeather.setOnClickListener {
            switchToFragment(WeatherFragment(), "weather")
            updateBottomNavigationState("weather")
        }
        binding.tvMessage.setOnClickListener {
            switchToFragment(MessageFragment(), "message")
            updateBottomNavigationState("message")
        }
        binding.tvMe.setOnClickListener {
            switchToFragment(MeFragment(), "me")
            updateBottomNavigationState("me")
        }
        binding.ivAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }
    }

    private fun switchToFragment(targetFragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // 1. 查找目标 Fragment 是否已存在
        var fragment = fragmentManager.findFragmentByTag(tag)

        // 2. 如果当前有显示的 Fragment，先隐藏
        if (currentFragment != null && currentFragment != fragment) {
            transaction.hide(currentFragment!!)
        }

        if (fragment == null) {
            // 3. 如果目标 Fragment 不存在，则添加
            fragment = targetFragment
            transaction.add(R.id.fragment_container, fragment, tag)
        } else {
            // 4. 如果目标 Fragment 已存在，则显示
            transaction.show(fragment)
        }

        transaction.commit()
        currentFragment = fragment
    }
}