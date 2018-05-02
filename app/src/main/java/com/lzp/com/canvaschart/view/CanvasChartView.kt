package com.lzp.com.canvaschart.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


/**
 * Created by li.zhipeng on 2018/5/2.
 */
class CanvasChartView(context: Context, attributes: AttributeSet?, defStyleAttr: Int)
    : View(context, attributes, defStyleAttr) {

    constructor(context: Context, attributes: AttributeSet?) : this(context, attributes, 0)

    constructor(context: Context) : this(context, null)

    /**
     * 画笔
     *
     * 设置抗锯齿和防抖动
     * */
    private val paint: Paint by lazy {
        val field = Paint()
        field.isAntiAlias = true
        field.isDither = true
        field
    }

    /**
     * 绘制X轴和Y轴的颜色
     *
     *  默认是系统自带的蓝色
     * */
    var lineColor: Int = Color.BLUE

    /**
     * 绘制X轴和Y轴的宽度
     * */
    var lineWidth = 5f

    /**
     * 图表的颜色
     * */
    var chartLineColor: Int = Color.RED

    /**
     * 图表的宽度
     * */
    var chartLineWidth: Float = 3f

    /**
     * 圆点的宽度
     * */
    var dotWidth = 15f

    /**
     * 圆点的颜色
     * */
    var dotColor: Int = Color.BLACK

    /**
     * 虚线的颜色
     * */
    var dashLineColor: Int = Color.GRAY

    /**
     * 虚线的颜色
     * */
    var dashLineWidth: Float = 2f

    /**
     * x轴的刻度间隔
     *
     * 因为x周是可以滑动的，所以只有刻度的数量这一个属性
     * */
    var xLineMarkCount: Int = 5

    /**
     * y轴的最大刻度
     * */
    var yLineMax: Int = 100

    /**
     * 绘制文字的大小
     * */
    var textSize: Float = 40f

    /**
     * 绘制文字的颜色
     * */
    var textColor: Int = Color.BLACK

    /**
     * 文字和圆点之间的间距
     * */
    var textSpace: Int = 10

    /**
     * 数据适配器
     * */
    var adapter: BaseDataAdapter? = null
        set(value) {
            field = value
            invalidate()
            value?.addObserver { _, _ ->
                // 当数据发生改变的时候，立刻重绘
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制X轴和Y轴
        drawXYLine(canvas)
        // 绘制数据
        drawData(canvas)
    }

    /**
     * 绘制X轴和Y轴
     *
     * x轴位于中心位置，值为0
     * y轴位于最最左边，与x轴交叉，交叉点为0
     * */
    private fun drawXYLine(canvas: Canvas) {
        // 设置颜色和宽度
        paint.color = lineColor
        paint.strokeWidth = lineWidth
        paint.style = Paint.Style.STROKE
        drawXLine(canvas)
        drawYLine(canvas)
    }

    /**
     * 画X轴
     * */
    private fun drawXLine(canvas: Canvas) {
        val width = width.toFloat()
        // 计算y方向上的中心位置
        val yCenter = (height - lineWidth) / 2
        // 绘制X轴
        canvas.drawLine(0f, yCenter, width, yCenter, paint)
    }

    /**
     * 画Y轴
     * */
    private fun drawYLine(canvas: Canvas) {
        // 计算一下Y轴的偏移值
        val offsetY = lineWidth / 2
        // 绘制Y轴
        canvas.drawLine(offsetY, 0f, offsetY, height.toFloat(), paint)
        // 绘制每一条数据之间的间隔虚线
        drawDashLine(canvas)
    }

    /**
     * 绘制数据之间
     * */
    private fun drawDashLine(canvas: Canvas) {
        // 画条目之间的间隔虚线
        var index = 1
        // 通过x轴的刻度间隔，计算x轴坐标
        val xItemSpace = width / xLineMarkCount.toFloat()
        paint.color = dashLineColor
        paint.strokeWidth = dashLineWidth
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 1f)
        while (index < xLineMarkCount) {
            val startY = xItemSpace * index
            val path = Path()
            path.moveTo(startY, 0f)
            path.lineTo(startY, height.toFloat())
            canvas.drawPath(path, paint)
            index++
        }
    }

    /**
     * 绘制数据曲线
     * */
    private fun drawData(canvas: Canvas) {
        // 设置画笔样式
        paint.pathEffect = null
        // 得到数据列表, 如果是null，取消绘制
        val dataList = adapter?.getData() ?: return
        // 绘制每一条数据列表
        for (item in dataList) {
            drawItemData(canvas, item)
        }
    }

    /**
     * 绘制一条数据曲线
     * */
    private fun drawItemData(canvas: Canvas, data: List<Int>) {
        // 通过x轴的刻度间隔，计算x轴坐标
        val xItemSpace = width / xLineMarkCount
        val path = Path()
        val dotPath = Path()
        for ((index, item) in data.withIndex()) {
            // 计算每一个点的位置f
            val xPos = (xItemSpace / 2 + index * xItemSpace).toFloat()
            val yPos = calculateYPosition(item)
            if (index == 0) {
                path.moveTo(xPos, yPos)
            } else {
                path.lineTo(xPos, yPos)
            }
            dotPath.addCircle(xPos, yPos, dotWidth, Path.Direction.CW)
            // 绘制文字
            drawText(canvas, item, xPos, yPos)
        }
        // 绘制曲线
        paint.style = Paint.Style.STROKE
        paint.color = chartLineColor
        paint.strokeWidth = chartLineWidth
        canvas.drawPath(path, paint)
        // 绘制圆点
        paint.color = dotColor
        paint.style = Paint.Style.FILL
        canvas.drawPath(dotPath, paint)
    }

    /**
     * 计算每一个数据点在Y轴上的坐标
     * */
    private fun calculateYPosition(value: Int): Float {
        // 计算比例
        val scale = value.toFloat() / yLineMax
        // 计算y方向上的中心位置
        val yCenter = (height - lineWidth) / 2
        // 如果小于0
        return yCenter - yCenter * scale
    }

    /**
     * 绘制文字
     * */
    private fun drawText(canvas: Canvas, item: Int, xPos: Float, yPos: Float) {
        val text = item.toString()
        paint.textSize = textSize
        paint.color = textColor
        paint.style = Paint.Style.FILL
        val textWidth = paint.measureText(text)
        // 文字自带的间距，不理解的可以查一下：如何绘制文字居中
        val offset = Math.abs(paint.ascent()) - paint.descent()
        if (item > 0) {
            // 要把文字自带的间距减去，统一和圆点之间的间距
            canvas.drawText(text, xPos - textWidth / 2, yPos - textSize + offset - textSpace, paint)
        }
        else{
            // 要把文字自带的间距减去，统一和圆点之间的间距
            canvas.drawText(text, xPos - textWidth / 2, yPos + dotWidth + offset + textSpace, paint)
        }
    }

}