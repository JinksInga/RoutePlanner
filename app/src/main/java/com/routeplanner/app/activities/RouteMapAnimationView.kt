package com.routeplanner.app.activities

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.DashPathEffect
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator

class RouteMapAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var startProgress = 0f
    private var destinationProgress = 0f
    private var routeProgress = 0f

    private val routePath = Path().apply {
        moveTo(35f, 125f)
        quadTo(70f, 85f, 100f, 120f)
        quadTo(135f, 155f, 165f, 70f)
    }

    private val routeMeasure = PathMeasure(routePath, false)

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun startAnimation() {

        val startAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator()

            addUpdateListener {
                startProgress = it.animatedValue as Float
                invalidate()
            }
        }

        val destinationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator()

            addUpdateListener {
                destinationProgress = it.animatedValue as Float
                invalidate()
            }
        }

        val routeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100

            addUpdateListener {
                routeProgress = it.animatedValue as Float
                invalidate()
            }
        }

        AnimatorSet().apply {
            playSequentially(
                startAnimator,
                destinationAnimator,
                routeAnimator
            )
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        val scale = size / 200f

        val saveCount = canvas.save()

        canvas.translate(
            (width - size) / 2f,
            (height - size) / 2f
        )

        canvas.scale(scale, scale)

        drawMap(canvas)
        drawRoute(canvas)

        if (startProgress > 0f) {
            drawPin(
                canvas,
                35f,
                95f,
                "#1565C0",
                startProgress
            )
        }

        if (destinationProgress > 0f) {
            drawPin(
                canvas,
                165f,
                40f,
                "#EF4444",
                destinationProgress
            )
        }

        canvas.restoreToCount(saveCount)
    }

    private fun drawMap(canvas: Canvas) {

        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#2196F3")

        val map = Path().apply {
            moveTo(20f, 65f)
            lineTo(75f, 40f)
            lineTo(130f, 60f)
            lineTo(180f, 35f)
            lineTo(180f, 145f)
            lineTo(125f, 170f)
            lineTo(70f, 150f)
            lineTo(20f, 175f)
            close()
        }

        canvas.drawPath(map, paint)

        paint.color = android.graphics.Color.parseColor("#43A047")

        val fold = Path().apply {
            moveTo(75f, 40f)
            lineTo(75f, 150f)
            lineTo(125f, 170f)
            lineTo(130f, 60f)
            close()
        }

        canvas.drawPath(fold, paint)

        paint.color = android.graphics.Color.parseColor("#64B5F6")

        val leftFold = Path().apply {
            moveTo(20f, 65f)
            lineTo(75f, 40f)
            lineTo(75f, 150f)
            lineTo(20f, 175f)
            close()
        }

        canvas.drawPath(leftFold, paint)
    }

    private fun drawRoute(canvas: Canvas) {

        if (routeProgress <= 0f) return

        val partialPath = Path()

        routeMeasure.getSegment(
            0f,
            routeMeasure.length * routeProgress,
            partialPath,
            true
        )

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 7f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = android.graphics.Color.WHITE

        paint.pathEffect = DashPathEffect(
            floatArrayOf(8f, 7f),
            0f
        )

        canvas.drawPath(partialPath, paint)

        paint.pathEffect = null
        paint.style = Paint.Style.FILL
    }

    private fun drawPin(
        canvas: Canvas,
        x: Float,
        y: Float,
        color: String,
        progress: Float
    ) {

        val saveCount = canvas.save()

        canvas.translate(x, y)

        canvas.scale(progress, progress)

        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor(color)

        val pin = Path().apply {
            moveTo(0f, -25f)
            cubicTo(-18f, -25f, -23f, -9f, -17f, 5f)
            cubicTo(-12f, 17f, 0f, 32f, 0f, 32f)
            cubicTo(0f, 32f, 12f, 17f, 17f, 5f)
            cubicTo(23f, -9f, 18f, -25f, 0f, -25f)
            close()
        }

        canvas.drawPath(pin, paint)

        paint.color = android.graphics.Color.WHITE

        canvas.drawCircle(0f, -8f, 8f, paint)

        canvas.restoreToCount(saveCount)
    }
}