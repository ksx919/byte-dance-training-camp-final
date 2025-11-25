package com.rednote

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.rednote.activity.AddActivity
import com.rednote.fragment.HomeFragment
import com.rednote.fragment.MeFragment
import com.rednote.fragment.MessageFragment
import com.rednote.fragment.ShopFragment

class MainActivity : AppCompatActivity() {
    private lateinit var tvHome: TextView

    private lateinit var tvShop: TextView

    private lateinit var tvMessage: TextView

    private lateinit var tvMe: TextView

    private lateinit var ivAdd: ImageView

    private var currentFragment: Fragment?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupBottomNavigation()

        switchToFragment(HomeFragment(), "home")
        updateBottomNavigationState("home")
    }

    private fun updateBottomNavigationState(selectTab: String) {

        tvHome.setTextColor(getColor(R.color.gray))
        tvShop.setTextColor(getColor(R.color.gray))
        tvMessage.setTextColor(getColor(R.color.gray))
        tvMe.setTextColor(getColor(R.color.gray))

        when (selectTab) {
            "home" -> {
                tvHome.setTextColor(getColor(R.color.black))
            }
            "shop" -> {
                tvShop.setTextColor(getColor(R.color.black))
            }
            "message" -> {
                tvMessage.setTextColor(getColor(R.color.black))
            }
            "me" -> {
                tvMe.setTextColor(getColor(R.color.black))
            }
        }
    }

    private fun initViews() {
        tvHome = findViewById(R.id.tv_home)
        tvShop = findViewById(R.id.tv_shop)
        tvMessage = findViewById(R.id.tv_message)
        tvMe = findViewById(R.id.tv_me)
        ivAdd = findViewById(R.id.iv_add)
    }

    private fun setupBottomNavigation() {
        tvHome.setOnClickListener {
            switchToFragment(HomeFragment(), "home")
        }
        tvShop.setOnClickListener {
            switchToFragment(ShopFragment(), "shop")
        }
        tvMessage.setOnClickListener {
            switchToFragment(MessageFragment(), "message")
        }
        tvMe.setOnClickListener {
            switchToFragment(MeFragment(), "me")
        }
        ivAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
        }
    }

    private fun switchToFragment(fragment: Fragment, tag: String) {
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        val existingFragment = fragmentManager.findFragmentByTag(tag)
        if (existingFragment != null) {
            transaction.replace(R.id.fragment_container, existingFragment, tag)
        } else {
            transaction.replace(R.id.fragment_container, fragment, tag)
        }

        transaction.commit()
        currentFragment = fragment
    }
}