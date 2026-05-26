package com.example.fincontrol

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.fincontrol.data.SessionManager
import com.example.fincontrol.data.TransactionRepository
import com.example.fincontrol.ui.BudgetBarChartView
import com.example.fincontrol.util.CurrencyUtils

class CarteiraActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var transactionRepository: TransactionRepository
    private var balanceVisible = false

    private fun getStatusBarHeight(): Int {
        val insets = WindowInsetsCompat.toWindowInsetsCompat(
            window.decorView.rootWindowInsets ?: return 0
        )
        return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carteira)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#2F3650"))
        val rootView = findViewById<android.view.View>(R.id.root_layout)
        rootView.setPadding(0, getStatusBarHeight(), 0, 0)

        sessionManager = SessionManager(this)
        transactionRepository = TransactionRepository(this)

        val userId = sessionManager.getLoggedUserId() ?: run {
            goToLogin()
            return
        }

        transactionRepository.syncRecurringTransactions(userId)

        val tvSaldo       = findViewById<TextView>(R.id.tvSaldo)
        val tvReceitasMes = findViewById<TextView>(R.id.tvReceitasMes)
        val tvDespesasMes = findViewById<TextView>(R.id.tvDespesasMes)
        val chartView     = findViewById<BudgetBarChartView>(R.id.chartView)
        val btnOlho       = findViewById<ImageView>(R.id.btnOlho)
        val btnCarteira   = findViewById<ImageView>(R.id.btnCarteira)
        val btnGrafico    = findViewById<ImageView>(R.id.btnGrafico)
        val btnExtrato    = findViewById<ImageView>(R.id.btnExtrato)

        // ── Dados do dashboard ────────────────────────────────────────
        val summary     = transactionRepository.getDashboardSummary(userId)
        val balanceText = CurrencyUtils.formatFromCents(summary.balanceCents)

        tvSaldo.text = "R$ ••••••"
        tvReceitasMes.text = "Receitas do mês: ${CurrencyUtils.formatFromCents(summary.incomeMonthCents)}"
        tvDespesasMes.text = "Despesas do mês: ${CurrencyUtils.formatFromCents(summary.expenseMonthCents)}"
        chartView.submitData(summary.topExpenseCategories, summary.incomeMonthCents)

        // ── Ícone do olho ─────────────────────────────────────────────
        btnOlho.setOnClickListener {
            balanceVisible = !balanceVisible
            tvSaldo.text = if (balanceVisible) balanceText else "R$ ••••••"
            btnOlho.isSelected = balanceVisible
        }

        // ── Navbar: marca carteira como ativa ─────────────────────────
        btnCarteira.setImageResource(R.drawable.carteira_icon_azul)
        btnGrafico.setImageResource(R.drawable.adicionar_icon)
        btnExtrato.setImageResource(R.drawable.extrato_icon)

        // ── Navegação ─────────────────────────────────────────────────
        findViewById<Button>(R.id.btnAdicionar).setOnClickListener {
            startActivity(Intent(this, LancamentosActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        btnCarteira.setOnClickListener { }

        btnGrafico.setOnClickListener {
            startActivity(Intent(this, LancamentosActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        btnExtrato.setOnClickListener {
            startActivity(Intent(this, ExtratoActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            sessionManager.clear()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}