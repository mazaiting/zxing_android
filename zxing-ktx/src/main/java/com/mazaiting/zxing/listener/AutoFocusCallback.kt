package com.mazaiting.zxing.listener

import android.hardware.Camera
import com.mazaiting.log.L

/**
 * 自动对焦回调
 */
class AutoFocusCallback : HandlerCallback(), Camera.AutoFocusCallback {
  
  companion object {
    /** 自动对焦延时  */
    private const val AUTO_FOCUS_INTERVAL_MS = 1500L
  }
  
  /**
   * 自动对焦函数
   * @param success 是否成功
   * @param camera 照相机设备
   */
  override fun onAutoFocus(success: Boolean, camera: Camera) {
    // 判断消息处理Handler是否为空
    if (handler != null) {
      // 获取消息
      val message = handler!!.obtainMessage(message, success)
      // 发送消息
      handler!!.sendMessageDelayed(message, AUTO_FOCUS_INTERVAL_MS)
      handler = null
    } else {
      L.d("Got auto-focus callback, but no handler for it")
    }
  }
}
