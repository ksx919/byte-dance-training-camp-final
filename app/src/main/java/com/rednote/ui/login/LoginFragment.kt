package com.rednote.ui.login

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rednote.databinding.FragmentLoginBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginFragment : BaseFragment<FragmentLoginBinding, LoginViewModel>() {

    override val viewModel: LoginViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater,container,false)
    }

    override fun initViews() {
        // --- 输入监听 ---
        binding.etEmail.doAfterTextChanged { text ->
            viewModel.updateEmail(text?.toString() ?: "")
        }

        binding.etPassword.doAfterTextChanged { text ->
            viewModel.updatePassword(text?.toString() ?: "")
        }

        // --- 点击事件 ---
        binding.tvGoRegister.setOnClickListener {
            (activity as? LoginActivity)?.replaceFragment(RegisterFragment(), true)
        }



        binding.btnLogin.setOnClickListener {
            viewModel.login()
        }
    }

    override fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察登录成功跳转事件
                launch {
                    viewModel.loginEvent.collect { event ->
                        when (event) {
                            is LoginUiEvent.NavigateToHome -> {
                                Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(context, MainActivity::class.java))
                                activity?.finish()
                            }
                        }
                    }
                }
            }
        }
    }

    // 6. 重写 Loading UI 逻辑
    // 当 BaseViewModel 的 isLoading 变化时，父类会自动调用这两个方法
    override fun showLoadingUI() {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "登录中..."
    }

    override fun hideLoadingUI() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = "登录"
    }
}