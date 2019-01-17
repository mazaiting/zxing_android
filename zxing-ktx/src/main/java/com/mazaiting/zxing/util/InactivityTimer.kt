package com.mazaiting.zxing.util

import android.app.Activity
import com.mazaiting.zxing.listener.FinishListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * Activity的定时器
 * @param activity 活动
 */
class InactivityTimer(private val activity: Activity) {
  companion object {
    /** 延时时长 */
    private const val INACTIVITY_DELAY_SECONDS = 5 * 60
  }
  
  /** 单线程 */
  private val inactivityTimer = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory())
  /** 计划未来控制 */
  private var inactivityFuture: ScheduledFuture<*>? = null
  
  init {
    onActivity()
  }
  
  /**
   * 初始化
   */
  fun onActivity() {
    // 取消
    cancel()
    // 初始化
    inactivityFuture = inactivityTimer.schedule(FinishListener(activity),
            INACTIVITY_DELAY_SECONDS.toLong(),
            TimeUnit.SECONDS)
  }
  
  /**
   * 取消
   */
  private fun cancel() {
    if (inactivityFuture != null) {
      // 取消任务
      inactivityFuture!!.cancel(true)
      inactivityFuture = null
    }
  }
  
  /**
   * 关闭
   */
  fun shutdown() {
    cancel()
    // 关闭
    inactivityTimer.shutdown()
  }
  
  /**
   * 线程工厂
   */
  private class DaemonThreadFactory : ThreadFactory {
    override fun newThread(runnable: Runnable): Thread {
      val thread = Thread(runnable)
      // 设置守护进程为true, 界面关闭时, 线程关闭
      thread.isDaemon = true
      return thread
    }
  }
}
