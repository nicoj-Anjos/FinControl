package com.example.fincontrol.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fincontrol.R
import com.example.fincontrol.model.Category
import com.example.fincontrol.model.FinanceTransaction
import com.example.fincontrol.model.TransactionType
import com.example.fincontrol.util.CurrencyUtils
import com.example.fincontrol.util.DateUtils
import java.time.LocalDate

class TransactionAdapter(
    private val context: Context,
    private val expenseCategories: List<Category>,
    private val incomeCategories: List<Category>,
    private val onUpdate: (FinanceTransaction) -> Unit,
    private val onDelete: (FinanceTransaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed interface Item {
        data class Header(val title: String) : Item
        data class Entry(val transaction: FinanceTransaction) : Item
    }

    private val items = mutableListOf<Item>()

    fun submitTransactions(transactions: List<FinanceTransaction>) {
        items.clear()
        var lastHeader: String? = null
        transactions.forEach { transaction ->
            val header = DateUtils.formatHeader(transaction.transactionDate)
            if (header != lastHeader) {
                items += Item.Header(header)
                lastHeader = header
            }
            items += Item.Entry(transaction)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is Item.Header -> 0
        is Item.Entry -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_extrato_header, parent, false)
            HeaderVH(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_extrato, parent, false)
            EntryVH(view)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Header -> (holder as HeaderVH).bind(item)
            is Item.Entry -> (holder as EntryVH).bind(item.transaction)
        }
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHeader: TextView = view.findViewById(R.id.tvHeaderDate)
        fun bind(item: Item.Header) {
            tvHeader.text = item.title.replaceFirstChar { it.uppercase() }
        }
    }

    inner class EntryVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSeta: TextView = view.findViewById(R.id.tvSeta)
        private val tvDescricao: TextView = view.findViewById(R.id.tvDescricao)
        private val tvCategoria: TextView = view.findViewById(R.id.tvCategoria)
        private val btnEditar: ImageView = view.findViewById(R.id.btnEditar)
        private val btnExcluir: ImageView = view.findViewById(R.id.btnExcluir)

        fun bind(transaction: FinanceTransaction) {
            val isExpense = transaction.type == TransactionType.EXPENSE
            tvSeta.text = if (isExpense) "↓" else "↑"
            tvSeta.setTextColor(Color.parseColor(if (isExpense) "#EA4335" else "#34A853"))
            tvDescricao.text = CurrencyUtils.formatFromCents(transaction.amountCents)
            tvCategoria.text = buildString {
                append(transaction.categoryName)
                if (!transaction.description.isNullOrBlank()) {
                    append(" • ")
                    append(transaction.description)
                }
            }
            btnEditar.setOnClickListener { showEditDialog(transaction) }
            btnExcluir.setOnClickListener { onDelete(transaction) }
        }

        private fun showEditDialog(transaction: FinanceTransaction) {
            val root = LayoutInflater.from(context).inflate(R.layout.dialog_edit_transaction, null)
            val etValue = root.findViewById<EditText>(R.id.etEditValue)
            val etDescription = root.findViewById<EditText>(R.id.etEditDescription)
            val spinnerType = root.findViewById<Spinner>(R.id.spinnerEditType)
            val spinnerCategory = root.findViewById<Spinner>(R.id.spinnerEditCategory)
            val tvDate = root.findViewById<TextView>(R.id.tvEditDate)

            etValue.setText(CurrencyUtils.formatFromCents(transaction.amountCents).replace("R$", "").trim())
            etDescription.setText(transaction.description.orEmpty())
            var selectedDate = transaction.transactionDate
            tvDate.text = DateUtils.format(selectedDate)

            val typeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listOf("Despesa", "Receita"))
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerType.adapter = typeAdapter
            spinnerType.setSelection(if (transaction.type == TransactionType.EXPENSE) 0 else 1)

            fun currentCategories(type: TransactionType): List<Category> = if (type == TransactionType.EXPENSE) expenseCategories else incomeCategories

            fun bindCategories(type: TransactionType, selectedCategoryName: String) {
                val list = currentCategories(type)
                val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, list.map { it.name })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = adapter
                val index = list.indexOfFirst { it.name == selectedCategoryName }.coerceAtLeast(0)
                spinnerCategory.setSelection(index)
            }

            bindCategories(transaction.type, transaction.categoryName)
            spinnerType.setOnItemSelectedListener(SimpleItemSelectedListener {
                val selectedType = if (spinnerType.selectedItemPosition == 0) TransactionType.EXPENSE else TransactionType.INCOME
                bindCategories(selectedType, transaction.categoryName)
            })

            tvDate.setOnClickListener {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        selectedDate = LocalDate.of(year, month + 1, day)
                        tvDate.text = DateUtils.format(selectedDate)
                    },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth
                ).show()
            }

            AlertDialog.Builder(context)
                .setTitle("Editar lançamento")
                .setView(root)
                .setPositiveButton("Salvar") { _, _ ->
                    val cents = CurrencyUtils.parseToCents(etValue.text.toString()) ?: return@setPositiveButton
                    val selectedType = if (spinnerType.selectedItemPosition == 0) TransactionType.EXPENSE else TransactionType.INCOME
                    val category = currentCategories(selectedType)[spinnerCategory.selectedItemPosition]
                    onUpdate(
                        transaction.copy(
                            amountCents = cents,
                            type = selectedType,
                            categoryId = category.id,
                            categoryName = category.name,
                            description = etDescription.text.toString().trim().ifBlank { null },
                            transactionDate = selectedDate
                        )
                    )
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}

private class SimpleItemSelectedListener(
    private val onSelected: () -> Unit
) : android.widget.AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) = onSelected()
    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
}
