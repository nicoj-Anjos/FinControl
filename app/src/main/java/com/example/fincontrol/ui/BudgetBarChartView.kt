package com.example.fincontrol.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.fincontrol.model.CategoryExpenseSummary
import com.example.fincontrol.util.CurrencyUtils
import kotlin.math.max

class BudgetBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2F3650")
        textSize = 56f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4A4A4A")
        textSize = 48f
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5A7FC4")
    }
    private val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D9E1F2")
    }

    private var items: List<CategoryExpenseSummary> = emptyList()
    private var totalIncomeCents: Long = 0L

    // Recebe os dados E a receita total do mês como referência
    fun submitData(data: List<CategoryExpenseSummary>, incomeCents: Long) {
        items = data
        totalIncomeCents = incomeCents
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = paddingTop + paddingBottom + max(items.size, 1) * 190
        val resolvedWidth = MeasureSpec.getSize(widthMeasureSpec)
        // EXACTLY força a view a ocupar exatamente o espaço calculado
        // independente do que o pai sugerir
        setMeasuredDimension(resolvedWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (items.isEmpty()) {
            labelPaint.textSize = 34f
            canvas.drawText("Sem despesas neste mês", paddingLeft.toFloat(), (height / 2).toFloat(), labelPaint)
            return
        }

        // Usa a receita do mês como 100% da barra
        // Se não houver receita, usa o maior gasto como fallback
        val maxValue = if (totalIncomeCents > 0) {
            totalIncomeCents.toFloat()
        } else {
            items.maxOf { max(it.totalCents, 1L) }.toFloat()
        }

        var top = paddingTop.toFloat() + 10f
        val left = paddingLeft.toFloat()
        val barLeft = left + 10f
        val barRight = width - paddingRight.toFloat() - 10f

        items.forEach { item ->
            canvas.drawText(item.categoryName, left, top + 30f, labelPaint)
            val valueText = CurrencyUtils.formatFromCents(item.totalCents)
            canvas.drawText(valueText, left, top + 78f, valuePaint)

            val barTop = top + 90f
            val barBottom = barTop + 28f

            // Fundo cinza (100%)
            canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, 16f, 16f, barBgPaint)

            // Preenchimento: limitado a 100% mesmo se o gasto ultrapassar a receita
            val ratio = (item.totalCents.toFloat() / maxValue).coerceAtMost(1f)
            val progressRight = barLeft + (ratio * (barRight - barLeft))
            canvas.drawRoundRect(barLeft, barTop, progressRight, barBottom, 16f, 16f, barPaint)

            top += 200f
        }
    }
}