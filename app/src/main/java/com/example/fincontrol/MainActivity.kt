package com.example.fincontrol

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fincontrol.data.SessionManager
import com.example.fincontrol.data.TransactionRepository
import com.example.fincontrol.data.UserRepository

class MainActivity : AppCompatActivity() {
    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var transactionRepository: TransactionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        userRepository = UserRepository(this)
        transactionRepository = TransactionRepository(this)

        val userId = sessionManager.getLoggedUserId()
        if (userId != null) {
            transactionRepository.syncRecurringTransactions(userId)
            openDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        val nome = findViewById<EditText>(R.id.nome)
        val senha = findViewById<EditText>(R.id.senha)
        val btnEntrar = findViewById<Button>(R.id.btnEntrar)

        findViewById<TextView>(R.id.tvEsqueceuSenha).setOnClickListener { showRecoverPasswordDialog() }
        findViewById<TextView>(R.id.tvCriarConta).setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }

        btnEntrar.setOnClickListener {
            val identifier = nome.text.toString().trim()
            val password = senha.text.toString()
            if (identifier.isBlank() || password.isBlank()) {
                toast("Preencha usuário/e-mail e senha.")
                return@setOnClickListener
            }
            val user = userRepository.login(identifier, password)
            if (user == null) {
                toast("Credenciais inválidas.")
                return@setOnClickListener
            }
            sessionManager.saveUser(user.id)
            transactionRepository.syncRecurringTransactions(user.id)
            openDashboard()
        }
    }

    private fun showRecoverPasswordDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_recover_password, null)
        val email = view.findViewById<EditText>(R.id.etRecoverEmail)
        val user = view.findViewById<EditText>(R.id.etRecoverUser)
        val newPassword = view.findViewById<EditText>(R.id.etRecoverPassword)

        AlertDialog.Builder(this)
            .setTitle("Recuperar senha")
            .setView(view)
            .setPositiveButton("Atualizar") { _, _ ->
                if (email.text.isNullOrBlank() || user.text.isNullOrBlank() || newPassword.text.isNullOrBlank()) {
                    toast("Preencha todos os campos para redefinir a senha.")
                    return@setPositiveButton
                }
                val success = userRepository.resetPassword(
                    email.text.toString(),
                    user.text.toString(),
                    newPassword.text.toString()
                )
                toast(if (success) "Senha atualizada com sucesso." else "Não encontramos uma conta com os dados informados.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openDashboard() {
        startActivity(Intent(this, CarteiraActivity::class.java))
        finish()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
