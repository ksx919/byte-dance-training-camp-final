package com.rednote.ui.login

import com.rednote.databinding.ActivityLoginBinding
import com.rednote.ui.base.BaseActivity

class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override fun getViewBinding(): ActivityLoginBinding {
        return ActivityLoginBinding.inflate(layoutInflater)
    }
}