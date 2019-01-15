package com.mazaiting.zxing.util

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.ReaderException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.mazaiting.log.L
import com.mazaiting.zxing.CaptureActivity
import com.mazaiting.zxing.camera.CameraManager
import com.mazaiting.zxing.constant.DECODE
import com.mazaiting.zxing.constant.DECODE_FAILED
import com.mazaiting.zxing.constant.DECODE_SUCCEEDED
import com.mazaiting.zxing.constant.QUIT

import java.util.Hashtable

/**
 * 解码处理工具
 * @param activity 相机界面
 * @param hints 解码类型
 *
 * @extend 继承自Handler
 */
internal class DecodeHandler(private val activity: CaptureActivity,
                             hints: Hashtable<DecodeHintType, Any>) : Handler() {
  /** 格式化读取器 */
  private val multiFormatReader: MultiFormatReader = MultiFormatReader()
  
  init {
    // 设置提示
    multiFormatReader.setHints(hints)
  }
  
  override fun handleMessage(message: Message) {
    when (message.what) {
      // 收到解码消息
      DECODE -> decode(message.obj as ByteArray, message.arg1, message.arg2)
      // 收到退出消息
      QUIT -> Looper.myLooper()!!.quit()
    }
//    if (message.what == DECODE) {
//      // 收到解码消息
//      // 解码
//      decode(message.obj as ByteArray, message.arg1, message.arg2)
//    } else if (message.what == QUIT) {
//      // 收到退出消息
//      // Looper退出
//      Looper.myLooper()!!.quit()
//    }
  }
  
  /**
   * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency,
   * reuse the same reader objects from one decode to the next.
   * 将ViewFinder中的数据解码,并且记录话费的时间。为了效率，下一次解码重用相同的读取对象
   * @param data   YUV预览帧
   * @param width  预览帧宽度
   * @param height 预览帧高度
   */
  private fun decode(data: ByteArray, width: Int, height: Int) {
//    var width = width
//    var height = height
    // 记录开始时间
    val start = System.currentTimeMillis()
    // 扫描结果
    var rawResult: Result? = null
    
    // 创建二进制字节数组组
    val rotatedData = ByteArray(data.size)
    // 将二进制图像数据旋转90度
    for (y in 0 until height) {
      for (x in 0 until width)
        // 存放数据
        rotatedData[x * height + height - y - 1] = data[x + y * width]
    }
//    val tmp = width // Here we are swapping, that's the difference to #11
//    width = height
//    height = tmp
    // 从相机生成
    val source = CameraManager.get().buildLuminanceSource(rotatedData, height, width)
    // 创建二进制Bitmap, 通过混合二值化器
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    try {
      // 解码工具解码
      rawResult = multiFormatReader.decodeWithState(bitmap)
    } catch (re: ReaderException) {
      if (re.message !=null && !TextUtils.isEmpty(re.message)) L.d(re.message)
    } finally {
      // 重置解码工具
      multiFormatReader.reset()
    }
    // 判断解码结果是否为空
    if (rawResult != null) {
      // 结束时间
      val end = System.currentTimeMillis()
      // 打印计算结果
      L.d("Found barcode (${end - start}} ms):\n $rawResult")
      // 生成消息
//      val message = Message.obtain(activity.getHandler(), R.id.zxing_decode_succeeded, rawResult)
      val message = Message.obtain(activity.getHandler(), DECODE_SUCCEEDED, rawResult)
      // 创建消息存储
      val bundle = Bundle()
      // 存储图片信息
      bundle.putParcelable(DecodeThread.BARCODE_BITMAP, source.renderCroppedGreyScaleBitmap())
      message.data = bundle
      L.d("Sending decode succeeded message...")
      // 发送消息
      message.sendToTarget()
    } else {
//      val message = Message.obtain(activity.getHandler(), R.id.zxing_decode_failed)
      // 创建消息
      val message = Message.obtain(activity.getHandler(), DECODE_FAILED)
      // 发送消息
      message.sendToTarget()
    }
  }
}
