package com.mazaiting.zxing.camera

import android.graphics.Bitmap

import com.google.zxing.LuminanceSource
import kotlin.experimental.and

/**
 * 根据YUV数组返回相机设备, 相机设备包含剪切为矩形的全部数据, 它可能外部包含多余的像素并且加速解码.
 * 它工作在任何的像素格式当Y通道是一个首次的平面,包含${ImageFormat.NV21}和${ImageFormat.NV16}
 *
 * @param yuvData 二进制数据
 * @param dataWidth 数据宽度
 * @param dataHeight 数据高度
 * @param left 左侧边距
 * @param top 右侧边距
 * @param width 宽度
 * @param height 高度
 * @extend 继承自LuminanceSource
 */
class PlanarYUVLuminanceSource(private val yuvData: ByteArray,
                               private val dataWidth: Int,
                               private val dataHeight: Int,
                               private val left: Int,
                               private val top: Int,
                               width: Int,
                               height: Int) : LuminanceSource(width, height) {
  
  init {
    // 判断二维码的大小是否大于数据的大小
    if (left + width > dataWidth || top + height > dataHeight) {
      throw IllegalArgumentException("Crop rectangle does not fit within image data.")
    }
  }
  
  override fun getRow(y: Int, row: ByteArray?): ByteArray {
    // 获取二进制数据
    var dataRow = row
    // 判断行的大小是否超过限制
    if (y < 0 || y >= height) {
      throw IllegalArgumentException("Requested row is outside the image: $y")
    }
//    val width = width
    // 计算二进制数据数组的大小是否为空或小于宽度
    if (dataRow == null || dataRow.size < width) {
      dataRow = ByteArray(width)
    }
    // 计算偏移量
    val offset = (y + top) * dataWidth + left
    // 使用系统类进行拷贝
    System.arraycopy(yuvData, offset, dataRow, 0, width)
    return dataRow
  }
  
  override fun getMatrix(): ByteArray {
    val width = width
    val height = height
    
    // If the caller asks for the entire underlying image, save the copy and give them the
    // original data. The docs specifically warn that result.length must be ignored.
    if (width == dataWidth && height == dataHeight) {
      return yuvData
    }
    
    val area = width * height
    val matrix = ByteArray(area)
    var inputOffset = top * dataWidth + left
    
    // If the width matches the full width of the underlying data, perform a single copy.
    if (width == dataWidth) {
      System.arraycopy(yuvData, inputOffset, matrix, 0, area)
      return matrix
    }
    
    // Otherwise copy one cropped row at a time.
    val yuv = yuvData
    for (y in 0 until height) {
      val outputOffset = y * width
      System.arraycopy(yuv, inputOffset, matrix, outputOffset, width)
      inputOffset += dataWidth
    }
    return matrix
  }
  
  override fun isCropSupported(): Boolean {
    return true
  }
  
  /**
   * 绘制剪切缩放图像
   * @return 位图对象
   */
  fun renderCroppedGreyScaleBitmap(): Bitmap {
    // 赋值宽高
    val width = width
    val height = height
    // 创建整型数组
    val pixels = IntArray(width * height)
    // 赋值二进制数据
    val yuv = yuvData
    // 输入偏移
    var inputOffset = top * dataWidth + left
    // 遍历设置颜色
    for (y in 0 until height) {
      val outputOffset = y * width
      for (x in 0 until width) {
        val grey = yuv[inputOffset + x] and 0xff.toByte()
        // 设置图像
        pixels[outputOffset + x] = -0x1000000 or grey * 0x00010101
      }
      inputOffset += dataWidth
    }
    // 创建图像
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    // 设置图像
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
  }
}
