package pl.fotopuzzle

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class PuzzleView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {
    var onMove: ((Int) -> Unit)? = null
    var onSolved: (() -> Unit)? = null
    private var bitmap: Bitmap? = null
    private var n = 3
    var order = IntArray(0); private set   // order[position] = tile index
    var moves = 0; private set
    var solved = false; private set
    private var selected = -1
    private var dragFrom = -1
    private var dragX = 0f; private var dragY = 0f
    private var downX = 0f; private var downY = 0f
    private var dragging = false
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val line = Paint().apply { color = Color.WHITE; strokeWidth = 2f; style = Paint.Style.STROKE }
    private val sel = Paint().apply { color = Color.parseColor("#FFD0BCFF"); strokeWidth = 8f; style = Paint.Style.STROKE }
    private val src = Rect(); private val dst = RectF()

    fun hasGame() = bitmap != null

    fun start(bmp: Bitmap, size: Int) {
        bitmap = bmp; n = size; moves = 0; solved = false; selected = -1
        order = IntArray(n * n) { it }
        do { order.shuffle() } while (isSolved())
        onMove?.invoke(0); invalidate()
    }

    fun restore(bmp: Bitmap, size: Int, ord: IntArray, mv: Int) {
        bitmap = bmp; n = size; order = ord; moves = mv; solved = isSolved(); invalidate()
    }

    private fun isSolved() = order.indices.all { order[it] == it }
    private fun side() = min(width, height).toFloat()
    private fun left() = (width - side()) / 2f
    private fun top() = (height - side()) / 2f

    private fun cellAt(x: Float, y: Float): Int {
        val s = side(); val cx = x - left(); val cy = y - top()
        if (cx < 0 || cy < 0 || cx >= s || cy >= s) return -1
        return (cy / (s / n)).toInt() * n + (cx / (s / n)).toInt()
    }

    private fun drawTile(c: Canvas, tile: Int, l: Float, t: Float, cell: Float) {
        val b = bitmap ?: return
        val ts = b.width / n
        src.set((tile % n) * ts, (tile / n) * ts, (tile % n + 1) * ts, (tile / n + 1) * ts)
        dst.set(l, t, l + cell, t + cell)
        c.drawBitmap(b, src, dst, paint)
        if (!solved) c.drawRect(dst, line)
    }

    override fun onDraw(c: Canvas) {
        if (bitmap == null) return
        val cell = side() / n
        for (p in order.indices) {
            if (dragging && p == dragFrom) continue
            val l = left() + (p % n) * cell; val t = top() + (p / n) * cell
            drawTile(c, order[p], l, t, cell)
            if (p == selected) c.drawRect(l + 4, t + 4, l + cell - 4, t + cell - 4, sel)
        }
        if (dragging && dragFrom >= 0) {
            paint.alpha = 220
            drawTile(c, order[dragFrom], dragX - cell / 2, dragY - cell / 2, cell)
            paint.alpha = 255
            c.drawRect(dst, sel)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (bitmap == null || solved) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { dragFrom = cellAt(e.x, e.y); downX = e.x; downY = e.y; dragX = e.x; dragY = e.y; dragging = false; return dragFrom >= 0 }
            MotionEvent.ACTION_MOVE -> {
                dragX = e.x; dragY = e.y
                if (!dragging && Math.hypot((e.x - downX).toDouble(), (e.y - downY).toDouble()) > side() / n / 4) { dragging = true; selected = -1 }
                if (dragging) invalidate()
            }
            MotionEvent.ACTION_UP -> {
                val to = cellAt(e.x, e.y)
                if (dragging) { dragging = false; if (to >= 0 && to != dragFrom) swap(dragFrom, to) }
                else if (to >= 0) {
                    when { selected < 0 -> selected = to; selected == to -> selected = -1; else -> { val a = selected; selected = -1; swap(a, to) } }
                }
                invalidate(); performClick()
            }
            MotionEvent.ACTION_CANCEL -> { dragging = false; invalidate() }
        }
        return true
    }

    override fun performClick(): Boolean { super.performClick(); return true }

    private fun swap(a: Int, b: Int) {
        val t = order[a]; order[a] = order[b]; order[b] = t
        moves++; onMove?.invoke(moves)
        if (isSolved()) { solved = true; invalidate(); onSolved?.invoke() }
    }
}
