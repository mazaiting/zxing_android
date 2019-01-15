package com.mazaiting.zxing.listener

import android.hardware.Camera
import com.mazaiting.log.L
import com.mazaiting.zxing.camera.CameraConfigurationManager

/**
 * 通知camera预览图像回调
 * 继承自HandlerCallback, 实现Camera.PreviewCallback接口
 */
class PreviewCallback(private val configManager: CameraConfigurationManager) : HandlerCallback(), Camera.PreviewCallback{
//  /** 显示Handler */
//  private var handler: Handler? = null
//  /** 显示消息Message */
//  private var message: Int = 0
//
//  /**
//   * 设置消息处理对象
//   * @param
//   */
//  fun setHandler(handler: Handler?, message: Int) {
//    this.handler = handler
//    this.message = message
//  }
//
  //预览图像
  override fun onPreviewFrame(data: ByteArray, camera: Camera) {
    if (handler != null) {
      // 获取相机结果内容
      val cameraResolution = configManager.cameraResolution
      // 生成消息
      val message = handler!!.obtainMessage(message, cameraResolution!!.x,
              cameraResolution.y, data)
      // 发送
      message.sendToTarget()
      handler = null
    } else {
      L.d("Got preview callback, but no handler for it")
    }
  }
}
