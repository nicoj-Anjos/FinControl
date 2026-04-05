package com.example.fincontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.fincontrol.data.SessionManager
import com.example.fincontrol.data.TransactionRepository
import com.example.fincontrol.ui.BudgetBarChartView
import com.example.fincontrol.util.CurrencyUtils

class CarteiraActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var transactionRepository: TransactionRepository
    private var balanceVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carteira)

        sessionManager = SessionManager(this)
        transactionRepository = TransactionRepository(this)

        val userId = sessionManager.getLoggedUserId() ?: run {
            goToLogin()
            return
        }

        transactionRepository.syncRecurringTransactions(userId)

        val tvSaldo = findViewById<TextView>(R.id.tvSaldo)
        val tvReceitasMes = findViewById<TextView>(R.id.tvReceitasMes)
        val tvDespesasMes = findViewById<TextView>(R.id.tvDespesasMes)
        val chartView = findViewById<BudgetBarChartView>(R.id.chartView)

        val summary = transactionRepository.getDashboardSummary(userId)
        val balanceText = CurrencyUtils.formatFromCents(summary.balanceCents)
        tvSaldo.text = "R$ ••••••"
        findViewById<ImageView>(R.id.btnOlho).setOnClickListener {
            balanceVisible = !balanceVisible
            tvSaldo.text = if (balanceVisible) balanceText else "R$ ••••••"
        }

        tvReceitasMes.text = "Receitas do mês: ${CurrencyUtils.formatFromCents(summary.incomeMonthCents)}"
        tvDespesasMes.text = "Despesas do mês: ${CurrencyUtils.formatFromCents(summary.expenseMonthCents)}"
        chartView.submitData(summary.topExpenseCategories, summary.incomeMonthCents)


        findViewById<Button>(R.id.btnAdicionar).setOnClickListener {
            startActivity(Intent(this, LancamentosActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            sessionManager.clear()
            goToLogin()
        }

        findViewById<ImageView>(R.id.btnCarteira).setOnClickListener { }
        findViewById<ImageView>(R.id.btnGrafico).setOnClickListener {
            startActivity(Intent(this, LancamentosActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnExtrato).setOnClickListener {
            startActivity(Intent(this, ExtratoActivity::class.java))
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
