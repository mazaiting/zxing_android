package com.mazaiting.zxing.util

import android.graphics.Bitmap
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.mazaiting.log.L
import java.util.*

object DecodeManager {
  /** 解码提示信息 */
  internal val HINTS: Hashtable<DecodeHintType, Any> = Hashtable(3)
  
  init {
    /** 解码格式 */
    val decodeFormat: Vector<BarcodeFormat> = Vector(11)
    decodeFormat.add(BarcodeFormat.UPC_A)
    decodeFormat.add(BarcodeFormat.UPC_E)
    decodeFormat.add(BarcodeFormat.EAN_13)
    decodeFormat.add(BarcodeFormat.EAN_8)
    decodeFormat.add(BarcodeFormat.RSS_14)
    decodeFormat.add(BarcodeFormat.CODE_39)
    decodeFormat.add(BarcodeFormat.CODE_93)
    decodeFormat.add(BarcodeFormat.CODE_128)
    decodeFormat.add(BarcodeFormat.ITF)
    decodeFormat.add(BarcodeFormat.QR_CODE)
    decodeFormat.add(BarcodeFormat.DATA_MATRIX)
    // 设置属性
    HINTS[DecodeHintType.POSSIBLE_FORMATS] = decodeFormat
    // 设置字符集
    HINTS[DecodeHintType.CHARACTER_SET] = "UTF-8"
  }
  
  /**
   * 同步解析本地图片二维码。该方法是耗时操作，请在子线程中调用。
   *
   * @param path 要解析的二维码图片本地路径
   * @return 返回二维码图片里的内容 或 null
   */
  fun syncDecodeQRCode(path: String) = syncDecodeQRCode(FileUtil.getDecodeBitmap(path))
  
  /**
   * 同步解析bitmap二维码。该方法是耗时操作，请在子线程中调用。
   * @param bitmap 要解析的二维码图片
   * @return 返回二维码图片里的内容 或 null
   */
  private fun syncDecodeQRCode(bitmap: Bitmap?): String? {
    // 判断图像是否为空
    if (null == bitmap) return null
    // 亮度源
    var source: RGBLuminanceSource? = null
    return try {
      // 获取图片宽高
      val width = bitmap.width
      val height = bitmap.height
      // 创建图片像素
      val pixels = IntArray(width * height)
      // 获取图片像素点
      bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
      // 创建亮度源
      source = RGBLuminanceSource(width, height, pixels)
      // 获取结果集
      val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)), HINTS)
      result.text
    } catch (e: Exception) {
      L.d(e.message)
      if (source != null) {
        try {
          // 获取结果
          val result = MultiFormatReader().decode(BinaryBitmap(GlobalHistogramBinarizer(source)), HINTS)
          result.text
        } catch (e: Exception) {
          L.d(e.message)
        }
      }
      null
    }
  }
}