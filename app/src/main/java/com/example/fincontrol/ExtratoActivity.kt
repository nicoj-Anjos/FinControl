package com.example.fincontrol

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fincontrol.data.CategoryRepository
import com.example.fincontrol.data.SessionManager
import com.example.fincontrol.data.TransactionRepository
import com.example.fincontrol.model.FinanceTransaction
import com.example.fincontrol.model.TransactionType
import com.example.fincontrol.ui.TransactionAdapter

class ExtratoActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var adapter: TransactionAdapter
    private var userId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extrato)

        sessionManager = SessionManager(this)
        transactionRepository = TransactionRepository(this)
        categoryRepository = CategoryRepository(this)
        userId = sessionManager.getLoggedUserId() ?: run {
            goToLogin()
            return
        }

        transactionRepository.syncRecurringTransactions(userId)

        val rv = findViewById<RecyclerView>(R.id.rvExtrato)
        val tvEmpty = findViewById<TextView>(R.id.tvSemLancamentos)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(
            context = this,
            expenseCategories = categoryRepository.getByType(TransactionType.EXPENSE),
            incomeCategories = categoryRepository.getByType(TransactionType.INCOME),
            onUpdate = ::updateTransaction,
            onDelete = ::deleteTransaction
        )
        rv.adapter = adapter
        loadTransactions(tvEmpty)

        findViewById<ImageView>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            sessionManager.clear()
            goToLogin()
        }
        findViewById<ImageView>(R.id.btnCarteira).setOnClickListener { startActivity(Intent(this, CarteiraActivity::class.java)); finish() }
        findViewById<ImageView>(R.id.btnGrafico).setOnClickListener { startActivity(Intent(this, LancamentosActivity::class.java)); finish() }
        findViewById<ImageView>(R.id.btnExtrato).setOnClickListener { }
    }

    private fun loadTransactions(tvEmpty: TextView) {
        val transactions = transactionRepository.getTransactions(userId)
        adapter.submitTransactions(transactions)
        tvEmpty.visibility = if (transactions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateTransaction(transaction: FinanceTransaction) {
        val updated = transactionRepository.updateTransaction(transaction)
        toast(if (updated) "Lançamento atualizado." else "Não foi possível atualizar o lançamento.")
        loadTransactions(findViewById(R.id.tvSemLancamentos))
    }

    private fun deleteTransaction(transaction: FinanceTransaction) {
        AlertDialog.Builder(this)
            .setTitle("Excluir lançamento")
            .setMessage("Deseja realmente excluir este lançamento?")
            .setPositiveButton("Excluir") { _, _ ->
                val deleted = transactionRepository.deleteTransaction(userId, transaction.id)
                toast(if (deleted) "Lançamento excluído." else "Não foi possível excluir o lançamento.")
                loadTransactions(findViewById(R.id.tvSemLancamentos))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
