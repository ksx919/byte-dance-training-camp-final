package com.rednote.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rednote.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 去注册点击事件
        binding.tvGoRegister.setOnClickListener {
            (activity as? LoginActivity)?.replaceFragment(RegisterFragment())
        }

        // 2. 关闭页面
        binding.btnClose.setOnClickListener {
            activity?.finish()
        }

        // 3. 登录按钮
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(context, "请输入邮箱和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 调用 ViewModel 执行登录 API
            performLoginMock(email, password)
        }
    }

    private fun performLoginMock(email: String, pass: String) {
        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
        // 登录成功后跳转逻辑
        // startActivity(Intent(activity, MainActivity::class.java))
        // activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}