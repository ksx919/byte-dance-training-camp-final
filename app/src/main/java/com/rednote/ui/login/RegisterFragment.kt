package com.rednote.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rednote.databinding.FragmentRegisterBinding

import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        // 1. 返回登录页
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. 输入监听
        binding.etNickname.doAfterTextChanged { viewModel.updateNickname(it.toString()) }
        binding.etEmail.doAfterTextChanged { viewModel.updateEmail(it.toString()) }
        binding.etPassword.doAfterTextChanged { viewModel.updatePassword(it.toString()) }

        // 3. 注册按钮
        binding.btnRegister.setOnClickListener {
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            if (pass != confirmPass) {
                Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.register()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 监听 Toast
                launch {
                    viewModel.toastEvent.collect { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }

                // 监听注册成功事件
                launch {
                    viewModel.registerEvent.collect { event ->
                        when (event) {
                            is RegisterUiEvent.RegisterSuccess -> {
                                // 注册成功后返回登录页
                                parentFragmentManager.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}