package otus.homework.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val MAX_TEXT_SIZE = 20
private const val GRID_SIZE = MAX_TEXT_SIZE shl 2
private const val HORIZONTAL_LINES = 6
class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var percent = 100
        set(value) {
            field = if (value < 0) 0 else { if (value > 100) 100 else value }
            invalidate()
        }

    private val payments = IntArray(24)
    private var drawGraph = false
    private var category: String = ""

    private val gridPaint = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(FloatArray(2) { 5f; 3f}, 0F)
    }
    private val gridText = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
        style = Paint.Style.FILL
//        textSize = GRID_SIZE - 4f
    }
    private val graphPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val d = payments.size - 1

        val w = GRID_SIZE * (d + gridPaint.strokeWidth.toInt()) + (paddingLeft + paddingRight)
        val h = GRID_SIZE * (HORIZONTAL_LINES + gridPaint.strokeWidth.toInt()) + (paddingTop + paddingBottom)

        val rw = resolveSize(w, widthMeasureSpec)
        val rh = resolveSize(h, heightMeasureSpec)

        var ww = max(rw - (paddingLeft + paddingRight), 0)
        var hh = max(rh - (paddingTop + paddingBottom) , 0)

        val g = min(ww / d, hh / HORIZONTAL_LINES)

        gridText.textSize = min(max((g shr 1).toFloat(), 0f), MAX_TEXT_SIZE.toFloat())

        ww = min(g * d + (paddingLeft + paddingRight), rw)
        hh = min(g * HORIZONTAL_LINES + (paddingTop + paddingBottom), rh)

        setMeasuredDimension(ww, hh)

        isVisible = ((ww > 0) && (hh > 0))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (drawGraph) {

            var amount = payments.max()
            var delta = amount / (HORIZONTAL_LINES - 2)

            var cost = 0f
            repeat(HORIZONTAL_LINES - 2) {
                cost = max(cost, gridText.measureText(amount.toString()))
                amount -= delta
            }

            delta = (height - paddingTop - paddingBottom) / HORIZONTAL_LINES
            var fromX = width - paddingRight - cost
            var fromY = (paddingTop + delta).toFloat()

            if (gridText.textSize > 0f)
                canvas.drawText(category, paddingLeft.toFloat(), paddingTop + delta - gridText.strokeWidth, gridText)


            amount = payments.max()
            repeat(HORIZONTAL_LINES - 1) {
                canvas.drawLine(
                    paddingLeft.toFloat(),
                    fromY,
                    (width - paddingRight).toFloat(),
                    fromY,
                    gridPaint
                )

                if (gridText.textSize > 0f)
                    canvas.drawText(
                        when(it) {
                            0 -> amount
                            1 -> amount - (amount shr 2)
                            2 -> amount shr 1
                            3 -> amount shr 2
                            else -> 0
                        }.toString(),
                        fromX,
                        fromY - 1,
                        gridText
                    )
                fromY += delta
            }

            val toY = fromY - delta

            fromY = (paddingTop + delta).toFloat()
            delta = (fromX - paddingLeft).toInt() / (payments.size - 1)
            fromX = paddingLeft.toFloat()

            cost = (toY - fromY) / amount // цена в пикселях одного рубля

            val maxX = (width - paddingLeft - paddingRight) * percent / 100f

            repeat(payments.size) { hour ->

                canvas.drawLine(fromX, fromY, fromX, toY, gridPaint)

                if (gridText.textSize > 0f)
                    canvas.drawText(hour.toString(), max(paddingLeft.toFloat(), fromX - if (hour < 10) 3 else 6), toY + gridText.textSize, gridText)

                if (hour > 0) {

                    val y1 = toY - cost * payments[hour - 1]

                    val x2: Float
                    val y2: Float

                    val maxY = toY - cost * payments[hour]

                    if (if (maxX > fromX) {
                        x2 = fromX
                        y2 = maxY
                        true
                    } else
                        if (maxX in fromX - delta.. fromX) {
                            x2 = maxX
                            y2 = (if (y1 > maxY) y1 - (x2 - (fromX - delta)) * (y1 - maxY) / delta else maxY - (fromX - x2) * (maxY - y1) / delta)
                            true
                        } else
                        {
                            x2 = 0f
                            y2 = 0f
                            false
                        }
                    )
                        canvas.drawLine(fromX - delta, y1, x2, y2, graphPaint)

                }

                fromX += delta
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable {

        return Bundle().apply {
            putString("payments", Json.encodeToString(payments))
            putString("category", category)
            putParcelable("instanceState", super.onSaveInstanceState())
        }

    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle)
            state.apply {
                getString("category")?.also { source ->
                    category = source
                }
                getString("payments")?.also { source ->
                    var i = 0
                    Json.decodeFromString<Array<Int>>(source).forEach { amount ->
                        payments[i] = amount
                        i++
                    }
                }
                drawGraph = true
                percent = 100
                super.onRestoreInstanceState(getParcelable("instanceState"))
            }
    }

    fun setPayments(payments: List<Payment>) {

        val p = payments.sortedBy { it.time }

        drawGraph = p.isNotEmpty()
        category = if (drawGraph) p[0].category else ""

        repeat(this.payments.size) { hour ->
            this.payments[hour] = 0
        }

        val f = SimpleDateFormat("HH", Locale.getDefault())
        p.forEach { payment ->
            this.payments[f.format(payment.time).toInt()] += payment.amount
        }
    }
}
