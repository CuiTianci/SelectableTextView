package com.ctc.selectabletextview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

/**
 * 文本选择TextView。
 * 选择控件的出现和隐藏不与点击、长按等手势进行绑定，需要手动调用。
 */
class SelectableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1
) :
    AppCompatTextView(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var startCharOffset = 0
    private var endCharOffset = 0
    private val startHandleCenter = Point(INVALID_OFFSET, INVALID_OFFSET)
    private val endHandleCenter = Point(INVALID_OFFSET, INVALID_OFFSET)
    private var isHandlingStart = false
    private var isHandlingEnd = false

    /**
     * 是否处于选择模式。
     *
     * @return true for 正处于选择模式。
     */
    var isInSelectMode = false
        private set
    private val originalPaddings = Rect()
    private var onSelectionChangListener: OnSelectionChangListener? = null

    override fun onDraw(canvas: Canvas) {
        if (!isInSelectMode || visibility == GONE) {
            super.onDraw(canvas)
        } else {
            paint.color = Color.parseColor("#660078FF")
            val startLine = getLineForOffset(startCharOffset)
            val endLine = getLineForOffset(endCharOffset)
            val startFullLine = startLine + 1
            val endFullLine = endLine - 1
            //绘制中间部分（中间部分整行选中）
            if (startFullLine <= endFullLine) {
                val fullAreaTop = getLineTop(startFullLine)
                val fullAreaBottom = getLineBottom(endFullLine)
                canvas.drawRect(
                    paddingLeft.toFloat(),
                    fullAreaTop,
                    (width - paddingRight).toFloat(),
                    fullAreaBottom,
                    paint
                )
            }
            val startPoint = getCoordinateOffset(startCharOffset)
            val endPoint = getCoordinateOffset(endCharOffset)
            //绘制起始行及末尾行。
            if (startLine == endLine) {
                //起始与末尾属同行的情况。
                val top = getLineTop(startLine)
                val bottom = getLineBottom(endLine)
                canvas.drawRect(
                    startPoint.x.toFloat(), top, endPoint.x.toFloat(), bottom,
                    paint
                )
            } else {
                //起始与末尾属不同行的情况。
                val startAreaTop = getLineTop(startLine)
                val startAreaBottom = getLineBottom(startLine)
                canvas.drawRect(
                    startPoint.x.toFloat(),
                    startAreaTop,
                    (width - paddingRight).toFloat(),
                    startAreaBottom,
                    paint
                )
                val endAreaTop = getLineTop(endLine)
                val endAreaBottom = getLineBottom(endLine)
                canvas.drawRect(
                    paddingLeft.toFloat(), endAreaTop, endPoint.x.toFloat(), endAreaBottom,
                    paint
                )
            }
            //上方，绘制选中背景。
            super.onDraw(canvas) //绘制TextView自身内容。
            //下方，绘制选择复制的Handle。
            paint.color = Color.parseColor("#0078FF")
            startHandleCenter.x = startPoint.x
            startHandleCenter.y = startPoint.y + paddingTop
            canvas.drawCircle(
                startHandleCenter.x.toFloat(),
                startHandleCenter.y.toFloat(),
                HANDLE_RADIUS.toFloat(),
                paint
            )
            paint.strokeWidth = BOUND_LINE_WIDTH
            canvas.drawLine(
                startHandleCenter.x.toFloat(),
                startHandleCenter.y.toFloat(),
                startHandleCenter.x.toFloat(),
                startHandleCenter.y + getLineHeight(startLine),
                paint
            )
            endHandleCenter.x = endPoint.x
            endHandleCenter.y = (endPoint.y + paddingTop + getLineHeight(endLine)).toInt()
            canvas.drawCircle(
                endPoint.x.toFloat(), endHandleCenter.y.toFloat(), HANDLE_RADIUS.toFloat(),
                paint
            )
            canvas.drawLine(
                endHandleCenter.x.toFloat(),
                endHandleCenter.y.toFloat(),
                endHandleCenter.x.toFloat(),
                endHandleCenter.y - getLineHeight(endLine),
                paint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isEventInHandle(event, endHandleCenter)) {
                    isHandlingEnd = true
                    //不允许父View拦截事件。
                    parent.requestDisallowInterceptTouchEvent(true)
                    return true
                } else if (isEventInHandle(event, startHandleCenter)) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    isHandlingStart = true
                    return true
                }
                return super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> return when {
                isHandlingEnd -> {
                    val newOffset = getOffsetForPosition(event.x, event.y)
                    //不允许选择内容为空，且仅在选中变化时进行更新。
                    if (startCharOffset != newOffset && endCharOffset != newOffset) {
                        endCharOffset = newOffset
                        exchangeHandleIfShould()
                        invalidate()
                    }
                    true
                }
                isHandlingStart -> {
                    val newOffset = getOffsetForPosition(event.x, event.y)
                    if (endCharOffset != newOffset && startCharOffset != newOffset) {
                        startCharOffset = newOffset
                        exchangeHandleIfShould()
                        invalidate()
                    } else if (endCharOffset == newOffset && endCharOffset == text.length) {
                        //处理endHandle处于最后一个字符时，无法将startHandle拖到右下角的问题。
                        startCharOffset = endCharOffset - 1
                        notifySelectionChanged()
                        invalidate()
                    }
                    true
                }
                else -> {
                    super.onTouchEvent(event)
                }
            }
            MotionEvent.ACTION_UP -> {
                isHandlingStart = false
                isHandlingEnd = false
                return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unselect()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        //记录原始padding。
        originalPaddings.top = top
        originalPaddings.bottom = bottom
        originalPaddings.left = left
        originalPaddings.right = right
    }

    /**
     * 根据行数获取该行的顶部y坐标。
     *
     * @param lineNum 行数。
     * @return 当前行顶部y坐标。
     */
    private fun getLineTop(lineNum: Int): Float {
        return if (layout == null) 0.0f else (layout.getLineTop(lineNum) + paddingTop).toFloat()
    }

    /**
     * 根据行数获取该行底部y坐标。
     *
     * @param lineNum 行数。
     * @return 当前行垂直方向y坐标。
     */
    private fun getLineBottom(lineNum: Int): Float {
        return if (layout == null) 0.0f else (layout.getLineBottom(lineNum) + paddingTop).toFloat()
    }

    /**
     * 根据行数获取行高。
     *
     * @param lineNum 行数。
     * @return 当前行的高度。
     */
    private fun getLineHeight(lineNum: Int): Float {
        return getLineBottom(lineNum) - getLineTop(lineNum)
    }

    /**
     * 通过字符offset获取其所在行数。
     *
     * @param offset 目标字符的offset。
     * @return 该字符所在行数。
     */
    private fun getLineForOffset(offset: Int): Int {
        return if (layout == null) 0 else layout.getLineForOffset(offset)
    }

    /**
     * 获取目标字符的坐标。
     *
     * @param charOffset 目标字符的位置。
     * @return 该字符的坐标。
     */
    private fun getCoordinateOffset(charOffset: Int): Point {
        val line = getLineForOffset(charOffset)
        val x = layout.getPrimaryHorizontal(charOffset).toInt() + paddingLeft
        val y = layout.getLineTop(line)
        return Point(x, y)
    }

    /**
     * 当start大于end时，双方进行交换。
     */
    private fun exchangeHandleIfShould() {
        if (startCharOffset > endCharOffset) {
            val temp = startCharOffset
            startCharOffset = endCharOffset
            endCharOffset = temp
            val tempFlag = isHandlingStart
            isHandlingStart = isHandlingEnd
            isHandlingEnd = tempFlag
        }
        notifySelectionChanged()
    }

    /**
     * 判断当前手势事件是否在Handle范围内。
     *
     * @param event        手势事件。
     * @param handleCenter handle的圆心。
     * @return true for 当前手势落在该Handle中。
     */
    private fun isEventInHandle(event: MotionEvent, handleCenter: Point): Boolean {
        if (!isInSelectMode) return false
        val start = handleCenter.x - HANDLE_TOUCH_RADIUS //这里取了一个虚拟的半径，比实际可见半径大，使触摸范围更大。
        val end = handleCenter.x + HANDLE_TOUCH_RADIUS
        val top = handleCenter.y - HANDLE_TOUCH_RADIUS + paddingTop
        val bottom = handleCenter.y + HANDLE_TOUCH_RADIUS + paddingTop
        return event.x >= start && event.x <= end && event.y >= top && event.y <= bottom
    }

    /**
     * 根据是否处于文本选择状态，调整padding。以保证Handle能够完全可见。
     */
    private fun adjustPaddings() {
        if (isInSelectMode) {
            val top = HANDLE_RADIUS.coerceAtLeast(originalPaddings.top)
            val bottom = HANDLE_RADIUS.coerceAtLeast(originalPaddings.bottom)
            val left = HANDLE_RADIUS.coerceAtLeast(originalPaddings.left)
            val right = HANDLE_RADIUS.coerceAtLeast(originalPaddings.right)
            super.setPadding(left, top, right, bottom)
        } else {
            super.setPadding(
                originalPaddings.left,
                originalPaddings.top,
                originalPaddings.right,
                originalPaddings.bottom
            )
        }
    }

    /**
     * 通知选中文字发生变化。
     */
    private fun notifySelectionChanged() {
        onSelectionChangListener?.apply {
            val selection = selection
            val isAllSelected = TextUtils.equals(selection, text)
            onSelectionChanged(selection, isAllSelected)
        }
    }

    /**
     * 进入文本选择模式并全选。
     */
    fun selectAll() {
        select(0, text.length)
    }

    /**
     * 进入文本选择模式，并选中指定区域。
     *
     * @param start 选中起始位置。
     * @param end   选中结束位置。
     */
    fun select(start: Int, end: Int) {
        isInSelectMode = true
        startCharOffset = 0.coerceAtLeast(start)
        endCharOffset = text.length.coerceAtMost(end)
        adjustPaddings()
        invalidate()
        notifySelectionChanged()
    }

    /**
     * 取消选中，并退出文本选择模式。
     */
    fun unselect() {
        isInSelectMode = false
        startCharOffset = INVALID_OFFSET
        endCharOffset = INVALID_OFFSET
        adjustPaddings()
        invalidate()
        notifySelectionChanged()
    }

    /**
     * 获取当前选中的内容。
     *
     * @return 当前选中内容。
     */
    val selection: CharSequence
        get() = text.subSequence(startCharOffset, endCharOffset)

    /**
     * 设置文字选中变化监听回调。
     */
    fun setOnSelectionChangListener(listener: OnSelectionChangListener?) {
        onSelectionChangListener = listener
    }

    /**
     * 移除文字选中变化监听回调。
     */
    fun removeOnSelectionChangeListener() {
        onSelectionChangListener = null
    }

    interface OnSelectionChangListener {
        /**
         * 选中内容发生变化。
         *
         * @param sequence      当前选中内容。
         * @param isAllSelected 是否全部选中。
         */
        fun onSelectionChanged(sequence: CharSequence?, isAllSelected: Boolean)
    }

    companion object {
        private const val HANDLE_RADIUS = 12
        private const val HANDLE_TOUCH_RADIUS = 40
        private const val INVALID_OFFSET = 0
        private const val BOUND_LINE_WIDTH = 6.0f
    }

    init {
        //原始padding。
        originalPaddings.top = paddingTop
        originalPaddings.bottom = paddingBottom
        originalPaddings.left = paddingLeft
        originalPaddings.right = paddingRight
    }
}