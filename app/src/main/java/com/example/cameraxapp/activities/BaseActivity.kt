package com.example.cameraxapp.activities

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    val context: Context
        get() = this

    protected fun init() {
        initArguments()
        initViews()
        setupListener()
        loadData()
    }

    protected interface OptionClickedListener {
        fun onBackBtnPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    fun showToast(msg: String?) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    abstract fun initArguments()
    abstract fun initViews()
    abstract fun setupListener()
    abstract fun loadData()






}