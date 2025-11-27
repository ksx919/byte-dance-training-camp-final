package com.rednote.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rednote.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 返回登录页
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. 注册按钮
        binding.btnRegister.setOnClickListener {
            val nick = binding.etNickname.text.toString()
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (nick.isBlank() || email.isBlank() || pass.isBlank() || confirmPass.isBlank()) {
                Toast.makeText(context, "请完善注册信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 调用 ViewModel 执行注册 API
            performRegisterMock()
        }
    }

    private fun performRegisterMock() {
        Toast.makeText(context, "注册成功", Toast.LENGTH_SHORT).show()
        // 注册成功后，通常返回登录页或者直接登录
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}