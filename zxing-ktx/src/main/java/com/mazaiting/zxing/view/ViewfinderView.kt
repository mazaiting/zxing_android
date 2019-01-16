package com.mazaiting.zxing.view

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.google.zxing.ResultPoint
import com.mazaiting.zxing.R
import com.mazaiting.zxing.camera.CameraManager
import java.util.HashSet

/**
 * 扫描框预览
 * @param context 上下文
 * @param attrs 属性
 */
class ViewfinderView(context: Context, attrs: AttributeSet) : View(context, attrs) {
  
  companion object {
    /** 扫描透明度 */
    private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
    /** 延时动画 */
    private const val ANIMATION_DELAY = 100L
    /** 不透明 */
    private const val OPAQUE = 0xFF
  }
  
  /** 画笔 */
  private val paint: Paint = Paint()
  /** 扫描结果位图 */
  private var resultBitmap: Bitmap? = null
  /** 标记颜色 */
  private val maskColor: Int
  /** 结果颜色 */
  private val resultColor: Int
  /** 帧颜色 */
  private val frameColor: Int
  /** 激光颜色 */
  private val laserColor: Int
  /** 结果点颜色 */
  private val resultPointColor: Int
  /** 扫描透明度 */
  private var scannerAlpha: Int = 0
  /** 可能的结果点集合 */
  private var possibleResultPoints: MutableCollection<ResultPoint>? = null
  /** 最后可能的结果点集合 */
  private var lastPossibleResultPoints: Collection<ResultPoint>? = null
  
  init {
    // 初始化资源, 绘制的时候使用
    val resources = resources
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      maskColor = resources.getColor(R.color.viewfinder_mask, null)
      resultColor = resources.getColor(R.color.result_view, null)
      frameColor = resources.getColor(R.color.viewfinder_frame, null)
      laserColor = resources.getColor(R.color.viewfinder_laser, null)
      resultPointColor = resources.getColor(R.color.possible_result_points, null)
    } else {
      maskColor = resources.getColor(R.color.viewfinder_mask)
      resultColor = resources.getColor(R.color.result_view)
      frameColor = resources.getColor(R.color.viewfinder_frame)
      laserColor = resources.getColor(R.color.viewfinder_laser)
      resultPointColor = resources.getColor(R.color.possible_result_points)
    }
    // 扫描透明度
    scannerAlpha = 0
    // 可能的结果点集合, 初始化为5个
    possibleResultPoints = HashSet(5)
  }
  
  public override fun onDraw(canvas: Canvas) {
    // 获取相机的帧矩形
    val frame = CameraManager.get().getFramingRect() ?: return
    val width = width
    val height = height
    // 设置画笔颜色
    paint.color = if (resultBitmap != null) resultColor else maskColor
    // 绘制矩形
    canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
    canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
    canvas.drawRect((frame.right + 1).toFloat(), frame.top.toFloat(), width.toFloat(), (frame.bottom + 1).toFloat(), paint)
    canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)
    // 结果位图不为空
    if (resultBitmap != null) {
      // 绘制不透明的结果位图在扫描矩形之上
      paint.alpha = OPAQUE
      // 绘制图片
      canvas.drawBitmap(resultBitmap!!, frame.left.toFloat(), frame.top.toFloat(), paint)
    } else {
      // 绘制两像素的黑色框在帧矩形上      // Draw a two pixel solid black border inside the framing rect
      paint.color = frameColor
      canvas.drawRect(frame.left.toFloat(), frame.top.toFloat(), (frame.right + 1).toFloat(), (frame.top + 2).toFloat(), paint)
      canvas.drawRect(frame.left.toFloat(), (frame.top + 2).toFloat(), (frame.left + 2).toFloat(), (frame.bottom - 1).toFloat(), paint)
      canvas.drawRect((frame.right - 1).toFloat(), frame.top.toFloat(), (frame.right + 1).toFloat(), (frame.bottom - 1).toFloat(), paint)
      canvas.drawRect(frame.left.toFloat(), (frame.bottom - 1).toFloat(), (frame.right + 1).toFloat(), (frame.bottom + 1).toFloat(), paint)
      
      // 绘制红色的激光线在正中间
      // 设置激光线颜色
      paint.color = laserColor
      // 设置透明度
      paint.alpha = SCANNER_ALPHA[scannerAlpha]
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size
      // 位置
      val middle = frame.height() / 2 + frame.top
      paint.strokeWidth = 4.0f
      // 绘制线
//      canvas.drawRect((frame.left + 2).toFloat(), (middle - 1).toFloat(), (frame.right - 1).toFloat(), (middle + 2).toFloat(), paint)
//      canvas.drawLine((frame.left + 2).toFloat(), (middle - 1).toFloat(), (frame.right - 1).toFloat(), (middle + 2).toFloat(), paint)
      canvas.drawLine((frame.left + 2).toFloat(), (middle - 1).toFloat(), (frame.right - 1).toFloat(), (middle + 2).toFloat(), paint)
      // 当前可能点
      val currentPossible = possibleResultPoints
      // 最后可能点
      val currentLast = lastPossibleResultPoints
      // 判断当前可能点集合是否为空
      if (currentPossible!!.isEmpty()) {
        lastPossibleResultPoints = null
      } else {
        // 赋值
        possibleResultPoints = HashSet(5)
        lastPossibleResultPoints = currentPossible
        // 不透明
        paint.alpha = OPAQUE
        paint.color = resultPointColor
        // 遍历点, 绘制点
        for (point in currentPossible) {
          canvas.drawCircle(frame.left + point.x, frame.top + point.y, 6.0f, paint)
        }
      }
      // 判断最后可能点是否为空
      if (currentLast != null) {
        paint.alpha = OPAQUE / 2
        paint.color = resultPointColor
        for (point in currentLast) {
          canvas.drawCircle(frame.left + point.x, frame.top + point.y, 3.0f, paint)
        }
      }
      
      // 请求动画更新
      postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom)
    }
  }
  
  /**
   * 绘制有效内容
   */
  fun drawViewfinder() {
    resultBitmap = null
    invalidate()
  }
  
  /**
   * 绘制结果为位图
   * @param barcode 解码图像
   */
  fun drawResultBitmap(barcode: Bitmap) {
    resultBitmap = barcode
    invalidate()
  }
  
  /**
   * 添加可能点
   * @param point 点的集合
   */
  fun addPossibleResultPoint(point: ResultPoint) {
    possibleResultPoints!!.add(point)
  }
  
}
