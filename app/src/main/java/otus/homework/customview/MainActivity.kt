package otus.homework.customview

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.Toast
import kotlinx.serialization.json.Json

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val text = resources.openRawResource(R.raw.payload)
            .bufferedReader().use { it.readText() }

        val data = Json.decodeFromString<Array<Payment>>(text)

        val graph = findViewById<GraphView>(R.id.graphView)

        val graphAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            addUpdateListener {
                graph.percent = it.animatedValue as Int
            }
        }

        val bar = findViewById<ChartView>(R.id.chartViewBar)
        bar.setCategories(data.asList())
        bar.setCallback { categoryName ->

            graph.setPayments(data.filter { it.category == categoryName }.toList())

            val barAnimator = ValueAnimator.ofInt(0, 100, -1).apply {
                duration = 2000
                interpolator = LinearInterpolator()
                addUpdateListener {
                    bar.percent = it.animatedValue as Int
                }
            }

            AnimatorSet().apply {
                playTogether(graphAnimator, barAnimator)
                start()
            }

            //Toast.makeText(context, categoryName, Toast.LENGTH_SHORT).show()
        }

        val pie = findViewById<ChartView>(R.id.chartViewPie)
        with (pie) {
            mode = ChartMode.Pie
            setCategories(data.asList())

            setCallback { categoryName ->
                Toast.makeText(context, categoryName, Toast.LENGTH_SHORT).show()

                graph.setPayments(data.filter { it.category == categoryName }.toList())

                val pieAnimator = ValueAnimator.ofInt(0, 100, -1).apply {
                    duration = 2000
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        pie.percent = it.animatedValue as Int
                    }
                }

                AnimatorSet().apply {
                    playTogether(graphAnimator, pieAnimator)
                    start()
                }

            }

        }

    }
}
