package com.mazaiting.zxing.listener

import android.os.Handler

/**
 * Handler处理消息基类
 */
open class HandlerCallback {
  /** 显示Handler */
  protected var handler: Handler? = null
  /** 显示消息Message */
  protected var message: Int = 0

  /**
   * 设置消息处理对象
   * @param
   */
  fun setHandler(handler: Handler?, message: Int) {
    this.handler = handler
    this.message = message
  }
}