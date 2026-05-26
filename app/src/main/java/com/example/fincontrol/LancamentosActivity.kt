package com.example.fincontrol

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fincontrol.data.CategoryRepository
import com.example.fincontrol.data.SessionManager
import com.example.fincontrol.data.TransactionRepository
import com.example.fincontrol.model.Category
import com.example.fincontrol.model.TransactionType
import com.example.fincontrol.util.DateUtils
import java.time.LocalDate

class LancamentosActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var transactionRepository: TransactionRepository

    private var selectedDate: LocalDate = LocalDate.now()
    private var tipoLancamento: TransactionType = TransactionType.EXPENSE
    private var expenseCategories: List<Category> = emptyList()
    private var incomeCategories: List<Category> = emptyList()

    private var lastExpenseCategoryIndex: Int = 0
    private var lastIncomeCategoryIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lancamentos)

        sessionManager = SessionManager(this)
        categoryRepository = CategoryRepository(this)
        transactionRepository = TransactionRepository(this)
        val userId = sessionManager.getLoggedUserId() ?: run {
            goToLogin()
            return@onCreate
        }

        expenseCategories = categoryRepository.getByType(TransactionType.EXPENSE)
        incomeCategories = categoryRepository.getByType(TransactionType.INCOME)

        val tvValor           = findViewById<EditText>(R.id.tvValor)
        val tvDataSelecionada = findViewById<android.widget.TextView>(R.id.tvDataSelecionada)
        val btnMais1          = findViewById<Button>(R.id.btnMais1)
        val btnMais10         = findViewById<Button>(R.id.btnMais10)
        val btnMais50         = findViewById<Button>(R.id.btnMais50)
        val btnMais100        = findViewById<Button>(R.id.btnMais100)
        val btnHoje           = findViewById<Button>(R.id.btnHoje)
        val btnOntem          = findViewById<Button>(R.id.btnOntem)
        val btnPeriodo        = findViewById<Button>(R.id.btnPeriodo)
        val spinnerCategoria  = findViewById<Spinner>(R.id.spinnerCategoria)
        val checkRecorrente   = findViewById<CheckBox>(R.id.checkRecorrente)
        val btnDespesa        = findViewById<Button>(R.id.btnDespesa)
        val btnReceita        = findViewById<Button>(R.id.btnReceita)
        val btnCadastrar      = findViewById<Button>(R.id.btnAdicionar)
        val etDescricao       = findViewById<EditText>(R.id.etDescricao)

        val btnCarteira = findViewById<ImageView>(R.id.btnCarteira)
        val btnGrafico  = findViewById<ImageView>(R.id.btnGrafico)
        val btnExtrato  = findViewById<ImageView>(R.id.btnExtrato)

// Marca lançamentos como ativo
        btnCarteira.setImageResource(R.drawable.carteira_icon)
        btnGrafico.setImageResource(R.drawable.adicionar_icon_azul)
        btnExtrato.setImageResource(R.drawable.extrato_icon)

        // ── Helpers de valor ──────────────────────────────────────
        // ── Helpers de valor ──────────────────────────────────────
        var isFormatting = false  // evita loop infinito no TextWatcher

        fun getValorCentavos(): Long {
            val texto = tvValor.text.toString()
                .replace(".", "").replace(",", "").trim()
            return texto.toLongOrNull() ?: 0L
        }

        fun setValor(centavos: Long) {
            val c = if (centavos < 0) 0L else centavos
            isFormatting = true
            tvValor.setText("%d,%02d".format(c / 100, c % 100))
            tvValor.setSelection(tvValor.text.length)
            isFormatting = false
        }

// Formata automaticamente enquanto o usuário digita
// mantendo sempre o padrão 0,00 como uma maquininha
        tvValor.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                isFormatting = true

                // Pega só os dígitos do que foi digitado
                val digits = s.toString().replace(".", "").replace(",", "").trimStart('0')
                val centavos = digits.toLongOrNull() ?: 0L

                val formatted = "%d,%02d".format(centavos / 100, centavos % 100)
                tvValor.setText(formatted)
                tvValor.setSelection(formatted.length)

                isFormatting = false
            }
        })

        // ── Categorias ────────────────────────────────────────────
        fun currentCategories(): List<Category> =
            if (tipoLancamento == TransactionType.EXPENSE) expenseCategories else incomeCategories

        fun bindCategories() {
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                currentCategories().map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapter

            // Restaura a última categoria selecionada para esse tipo
            val lastIndex = if (tipoLancamento == TransactionType.EXPENSE)
                lastExpenseCategoryIndex else lastIncomeCategoryIndex
            spinnerCategoria.setSelection(lastIndex)

            spinnerCategoria.post {
                val tv = spinnerCategoria.selectedView as? android.widget.TextView
                tv?.setTextColor(android.graphics.Color.parseColor("#252525"))
            }
        }

        spinnerCategoria.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, pos: Int, id: Long) {
                (view as? android.widget.TextView)?.setTextColor(android.graphics.Color.parseColor("#252525"))

                // Salva a posição para o tipo atual
                if (tipoLancamento == TransactionType.EXPENSE) {
                    lastExpenseCategoryIndex = pos
                } else {
                    lastIncomeCategoryIndex = pos
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        // ── Data ──────────────────────────────────────────────────
        fun setDate(date: LocalDate) {
            selectedDate = date
            tvDataSelecionada.text = "Data selecionada: ${DateUtils.format(date)}"
        }

        // ── Tipo (Despesa / Receita) ───────────────────────────────
        fun configureTypeButtons() {
            val despesaAtiva = tipoLancamento == TransactionType.EXPENSE
            btnDespesa.alpha = if (despesaAtiva) 1f else 0.6f
            btnReceita.alpha = if (despesaAtiva) 0.6f else 1f
        }

        // ── Inicialização ─────────────────────────────────────────
        setDate(LocalDate.now())
        bindCategories()
        configureTypeButtons()
        setValor(0L)

        // ── Botões de incremento ──────────────────────────────────
        btnMais1.setOnClickListener   { setValor(getValorCentavos() + 100) }
        btnMais10.setOnClickListener  { setValor(getValorCentavos() + 1000) }
        btnMais50.setOnClickListener  { setValor(getValorCentavos() + 5000) }
        btnMais100.setOnClickListener { setValor(getValorCentavos() + 10000) }

        // ── Data ──────────────────────────────────────────────────
        btnHoje.setOnClickListener  { setDate(LocalDate.now()) }
        btnOntem.setOnClickListener { setDate(LocalDate.now().minusDays(1)) }
        btnPeriodo.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day -> setDate(LocalDate.of(year, month + 1, day)) },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        // ── Tipo ──────────────────────────────────────────────────
        btnDespesa.setOnClickListener {
            tipoLancamento = TransactionType.EXPENSE
            configureTypeButtons()
            bindCategories()
        }
        btnReceita.setOnClickListener {
            tipoLancamento = TransactionType.INCOME
            configureTypeButtons()
            bindCategories()
        }

        // ── Cadastrar ─────────────────────────────────────────────
        btnCadastrar.setOnClickListener {
            val cents = getValorCentavos()
            if (cents <= 0) {
                toast("Informe um valor válido.")
                return@setOnClickListener
            }
            val categories = currentCategories()
            if (categories.isEmpty()) {
                toast("Nenhuma categoria disponível para esse tipo.")
                return@setOnClickListener
            }
            val category = categories[spinnerCategoria.selectedItemPosition]

            val tipo = if (tipoLancamento == TransactionType.EXPENSE) "Despesa" else "Receita"
            val valorFormatado = "R$ %,.2f".format(cents / 100.0)
                .replace(",", "X").replace(".", ",").replace("X", ".")
            val dataFormatada = DateUtils.format(selectedDate)
            val descricao = etDescricao.text.toString().trim()
            val recorrente = if (checkRecorrente.isChecked) "\nRecorrente: Sim" else ""
            val descricaoLinha = if (descricao.isNotBlank()) "\nDescrição: $descricao" else ""

            val mensagem = "Confirma o lançamento?\n\n" +
                    "Tipo: $tipo\n" +
                    "Valor: $valorFormatado\n" +
                    "Categoria: ${category.name}\n" +
                    "Data: $dataFormatada" +
                    descricaoLinha +
                    recorrente

            val dialogView = layoutInflater.inflate(R.layout.dialog_confirmacao, null)
            dialogView.findViewById<android.widget.TextView>(R.id.tvMensagem).text = mensagem

            val dialog = android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
                dialog.dismiss()
            }

            dialogView.findViewById<Button>(R.id.btnConfirmar).setOnClickListener {
                dialog.dismiss()
                val result = transactionRepository.saveTransaction(
                    TransactionRepository.SaveTransactionRequest(
                        userId = userId,
                        categoryId = category.id,
                        amountCents = cents,
                        type = tipoLancamento,
                        date = selectedDate,
                        description = descricao.ifBlank { null },
                        recurring = checkRecorrente.isChecked
                    )
                )
                result.onSuccess {
                    toast(if (checkRecorrente.isChecked) "Lançamento recorrente salvo." else "Lançamento salvo.")
                    startActivity(Intent(this@LancamentosActivity, ExtratoActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                }.onFailure {
                    toast(it.message ?: "Erro ao salvar lançamento.")
                }
            }

            dialog.show()
        } // ← fecha o btnCadastrar.setOnClickListener

        // ── Navegação ─────────────────────────────────────────────
        findViewById<ImageView>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            sessionManager.clear()
            goToLogin()
        }
        findViewById<ImageView>(R.id.btnCarteira).setOnClickListener {
            startActivity(Intent(this, CarteiraActivity::class.java));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
        findViewById<ImageView>(R.id.btnGrafico).setOnClickListener { }
        findViewById<ImageView>(R.id.btnExtrato).setOnClickListener {
            startActivity(Intent(this, ExtratoActivity::class.java));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    } // ← fecha o onCreate

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun goToLogin() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        )
        finish()
    }
}