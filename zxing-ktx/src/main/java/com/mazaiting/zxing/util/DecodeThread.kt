package com.mazaiting.zxing.util

import android.os.Handler
import android.os.Looper
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPointCallback
import com.mazaiting.zxing.CaptureActivity
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * 这个线程执行所有图片解码事务
 * @param activity 解码界面
 * @param resultPointCallback 结果回调
 *
 * @extend 继承自线程
 */
class DecodeThread(private val activity: CaptureActivity,
                   resultPointCallback: ResultPointCallback) : Thread() {
  
  companion object {
    /** 二维码图像 */
    const val BARCODE_BITMAP = "barcode_bitmap"
//    const val BARCODE_SCALED_FACTOR = "barcode_scaled_factor"
  }
  
  /** 解码提示信息 */
  private val hints: Hashtable<DecodeHintType, Any> = Hashtable(3)
  /** 消息处理独享 */
  internal var handler: Handler? = null
    private set
  /** 线程同步计数器 */
  private val handlerInitLatch: CountDownLatch = CountDownLatch(1)
  
  init {
    hints.putAll(DecodeManager.HINTS)
    // 设置结果回调
    hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] = resultPointCallback
  }
  
//  fun getHandler(): Handler? {
//    try {
//      handlerInitLatch.await()
//    } catch (ie: InterruptedException) {
//      // continue?
//    }
//
//    return handler
//  }
  
  override fun run() {
    // Looper准备
    Looper.prepare()
    // 创建新的解码处理器
    handler = DecodeHandler(activity, hints)
    // 计数
    handlerInitLatch.countDown()
    // Looper循环
    Looper.loop()
  }
}
