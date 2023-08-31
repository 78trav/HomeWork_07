package otus.homework.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import kotlinx.serialization.json.Json
import java.util.Random
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.min
import kotlin.math.sqrt
import kotlinx.serialization.encodeToString

private const val MAX_CATEGORIES = 10
private const val STROKE_WIDTH = 4

enum class ChartMode{
    Pie,
    Bar
}

data class Primitive(
    val rectF: RectF = RectF(),
    val paint: Paint = Paint(),
    var start: Float = 0f,
    var end: Float = 0f
)

class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var mode = ChartMode.Bar
        set(value) {
            field = value
            needCalculate = true
            invalidate()
        }

    private val chartCategories = emptyList<Category>().toMutableList()
    private val otherCategories = emptyList<Category>().toMutableList()

    private val primitives = Array(MAX_CATEGORIES) { Primitive() }
    private val colors = Array(MAX_CATEGORIES) { 0 }

    private var callback: ((categoryName: String) -> Unit)? = null

    private var needCalculate = true

    private var primitiveIndex = -1
    private val gradient = GradientDrawable().apply {
        gradientType = GradientDrawable.LINEAR_GRADIENT
        shape = GradientDrawable.RECTANGLE
        orientation = GradientDrawable.Orientation.BOTTOM_TOP
    }
    private val sourceRect = Rect()

    var percent = 100
        set(value) {
            field = if (value < 0) 0 else { if (value > 100) 100 else value }
            invalidate()
            if ((value < 0) || (value > 100)) primitiveIndex = -1
        }

    init {
        val rnd = Random()
        for (i in 0..< MAX_CATEGORIES) {
            while (true) {
                val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
                if (colors.find { it == color } == null ) {
                    colors[i] = color
                    break
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val w = MAX_CATEGORIES * 25 + (paddingLeft + paddingRight)
        val h = MAX_CATEGORIES * 25 + (paddingTop + paddingBottom)

        val rw = resolveSize(w, widthMeasureSpec)
        val rh = resolveSize(h, heightMeasureSpec)

        setMeasuredDimension(rw, rh)

        needCalculate = true
    }

    override fun onSaveInstanceState(): Parcelable {

        return Bundle().apply {
            putBoolean("isBarMode", (mode == ChartMode.Bar))
            putString("chartCategories", Json.encodeToString(chartCategories))
            putString("colors", Json.encodeToString(colors))
            putParcelable("instanceState", super.onSaveInstanceState())
        }

    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle)
            state.apply {
                getString("chartCategories")?.also { source ->
                    chartCategories.clear()
                    chartCategories.addAll(Json.decodeFromString(source))
                }
                getString("colors")?.also { source ->
                    var i = 0
                    Json.decodeFromString<Array<Int>>(source).forEach { color ->
                        colors[i] = color
                        i++
                    }
                }
                mode = if (getBoolean("isBarMode")) ChartMode.Bar else ChartMode.Pie
                super.onRestoreInstanceState(getParcelable("instanceState"))
            }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (needCalculate)
            calculateRectangles()

        primitives.forEach {
            if (!it.rectF.isEmpty) {
                if (mode == ChartMode.Bar) {

                    canvas.drawRect(it.rectF, it.paint)

                    if (primitives.indexOf(it) == primitiveIndex) {

                        val w = it.rectF.right - it.rectF.left
                        val h = it.rectF.bottom - it.rectF.top

                        val d = h / 50f * percent

                        sourceRect.set(0, d.toInt(), w.toInt(), (d + h).toInt())

                        canvas.drawBitmap(
                            gradient.toBitmap(w.toInt(), (h * 3).toInt(), Bitmap.Config.ARGB_8888),
                            sourceRect,
                            it.rectF,
                            null
                        )
                    }
                }
                else {

                    if (!((primitiveIndex == -1) || (primitives.indexOf(it) == primitiveIndex)))
                        it.paint.color = it.paint.color and 0x00FFFFFF or ((255 - 2.55f * percent).toInt() shl 24)

                    canvas.drawArc(it.rectF, it.start, it.end, false, it.paint)

                    if (primitives.indexOf(it) == primitiveIndex) {
                        it.paint.strokeWidth = (min(width, height) shr 3) + percent / 10f
                        canvas.drawArc(it.rectF, it.start, it.end, false, it.paint)
                    }
                }
            }
        }

    }

    private fun calculateRectangles() {

        primitives.forEach { it.rectF.setEmpty() }

        if (chartCategories.isNotEmpty()) {
            var cx = paddingLeft

            if (mode == ChartMode.Bar) {
                val maxAmount = chartCategories[0].amount.toFloat()
                val delta = width / chartCategories.size
                chartCategories.forEach {
                    val cy = height * (1 - it.amount / maxAmount) - paddingTop
                    val index = chartCategories.indexOf(it)
                    primitives[index].apply {
                        rectF.set(
                            cx.toFloat(),
                            cy,
                            (cx + delta - (STROKE_WIDTH shl 1)).toFloat(),
                            (height - paddingBottom).toFloat()
                        )
                        paint.apply {
                            strokeWidth = STROKE_WIDTH.toFloat()
                            style = Paint.Style.FILL
                            color = colors[index]
                        }
                    }
                    cx += delta
                }
            } else {
                var start = 0f
                val totalAmount = chartCategories.sumOf { it.amount }.toFloat()

                val r = min(width, height) shr 1
                val w = r shr 2

                chartCategories.forEach {
                    val index = chartCategories.indexOf(it)
                    primitives[index].apply {
                        rectF.set(
                            (cx + (width shr 1) - r + w).toFloat(),
                            (paddingTop + (height shr 1) - r + w).toFloat(),
                            (cx + (width shr 1) + r - w).toFloat(),
                            (paddingTop + (height shr 1) + r - w).toFloat()
                        )
                        paint.apply {
                            strokeWidth = w.toFloat()
                            style = Paint.Style.STROKE
                            color = colors[index]
                        }
                        this.start = start
                        end = it.amount / totalAmount * 360 - 1

                        start += end + 1
                    }
                }

            }
        }
        needCalculate = false
    }

    private fun onClick(index: Int) {
        if ((callback != null) && (primitiveIndex == -1)) {

            primitiveIndex = index

            gradient.colors = intArrayOf(
                colors[index],
                Color.WHITE,
                colors[index]
            )

            callback!!.invoke(chartCategories[index].name)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (callback != null)
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (mode == ChartMode.Bar)
                    primitives.find { it.rectF.contains(event.x, event.y) }?.also {
                        onClick(primitives.indexOf(it))
                    }
                else {
                    val halfWidth = width shr 1
                    val halfHeight = height shr 1
                    val delta: Float
                    val a: Float
                    val b: Float

                    if (event.y > halfHeight) {
                        if (event.x > halfWidth) {
                            a = event.y - halfHeight
                            b = event.x - halfWidth
                            delta = 0f
                        } else {
                            a = halfWidth - event.x
                            b = event.y - halfHeight
                            delta = 90f
                        }
                    }
                    else {
                        if (event.x > halfWidth) {
                            a = event.x - halfWidth
                            b = halfHeight - event.y
                            delta = 270f
                        } else {
                            a = halfHeight - event.y
                            b = halfWidth - event.x
                            delta = 180f
                        }
                    }
                    val angle = asin(a / sqrt((a * a + b * b).toDouble())) * 180 / PI + delta
                    primitives.find { (angle >= it.start) && (angle <= it.start + it.end) }?.also {
                        onClick(primitives.indexOf(it))
                    }
                }
            }

        return super.onTouchEvent(event)
    }

    fun setCategories(payments: List<Payment>) {

        chartCategories.clear()
        otherCategories.clear()

        var count = MAX_CATEGORIES - 1

        payments
            .groupingBy { it.category.lowercase() }
            .aggregate { _, amount: Int?, payment: Payment, _ ->
                (amount ?: 0) + payment.amount
            }
            .toList()
            .sortedByDescending { it.second }
            .forEach { pair ->
                (if (count > 0) chartCategories else otherCategories).add(Category(pair.first.replaceFirstChar { it.uppercase() }, pair.second))
                count--
            }

        if (otherCategories.size > 0)
            chartCategories.add(Category("Другое", otherCategories.sumOf { it.amount }))

        needCalculate = true

        invalidate()
    }

    fun setCallback(callback: ((categoryName: String) -> Unit)?) {
        this.callback = callback
    }

}
