package com.example.fincontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fincontrol.data.SessionManager
import com.example.fincontrol.data.UserRepository

class CadastroActivity : AppCompatActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        userRepository = UserRepository(this)
        sessionManager = SessionManager(this)

        val email = findViewById<EditText>(R.id.email)
        val usuario = findViewById<EditText>(R.id.usuario)
        val senha = findViewById<EditText>(R.id.senha)
        val termos = findViewById<CheckBox>(R.id.checkTermos)
        val cadastrar = findViewById<Button>(R.id.btnCadastrar)

        cadastrar.setOnClickListener {
            val nameValue = usuario.text.toString().trim()
            val emailValue = email.text.toString().trim()
            val passwordValue = senha.text.toString()

            when {
                nameValue.length < 3 -> toast("Informe um nome de usuário com pelo menos 3 caracteres.")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> toast("Informe um e-mail válido.")
                passwordValue.length < 6 -> toast("A senha deve ter pelo menos 6 caracteres.")
                !termos.isChecked -> toast("Aceite os termos para continuar.")
                else -> {
                    val result = userRepository.register(nameValue, emailValue, passwordValue)
                    result.onSuccess { userId ->
                        sessionManager.saveUser(userId)
                        toast("Conta criada com sucesso.")
                        startActivity(Intent(this, CarteiraActivity::class.java))
                        finishAffinity()
                    }.onFailure { error ->
                        toast(error.message ?: "Não foi possível criar a conta.")
                    }
                }
            }
        }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
