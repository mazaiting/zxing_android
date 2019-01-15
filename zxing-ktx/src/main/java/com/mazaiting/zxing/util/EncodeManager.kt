package com.mazaiting.zxing.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.text.TextUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * 二维码编码管理者
 * @use 使用方法
 *       // 获取输入内容呢
        val content = etContent.text.toString()
        // 获取二维码
        val bitmap = EncodeManager.encodeQrCode(content, 1024, 1024)
        // 添加图标
        val iconBitmap = EncodeManager.addIcon(bitmap!!, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
        
        // 设置图像
        imageView.setImageBitmap(iconBitmap)
 */
object EncodeManager {
  /**
   * 生成二维码
   * @param content 二维码内容
   * @param width 二维码宽高
   * @return Bitmap对象
   */
  fun encodeQrCode(content: String, width: Int): Bitmap? {
    // 检测内容是否为空
    if (TextUtils.isEmpty(content)) {
      return null
    }
    try {
      //1,创建实例化对象
      val writer = QRCodeWriter()
      //2,设置字符集
      val map = HashMap<EncodeHintType, String>()
      map[EncodeHintType.CHARACTER_SET] = "UTF-8"
      //3,通过encode方法将内容写入矩阵对象
      val matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, width, map)
      //4,定义一个二维码像素点的数组，向每个像素点中填充颜色
      val pixels = IntArray(width * width)
      //5,往每一像素点中填充颜色（像素没数据则用黑色填充，没有则用彩色填充，不过一般用白色）
      for (i in 0 until width) {
        for (j in 0 until width) {
          if (matrix.get(j, i)) {
            pixels[i * width + j] = -0x1000000
          } else {
            pixels[i * width + j] = -0x1
          }
        }
      }
      //6,创建一个指定高度和宽度的空白bitmap对象
      val bmQR = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
      //7，将每个像素的颜色填充到bitmap对象
      bmQR.setPixels(pixels, 0, width, 0, 0, width, width)
      
      return bmQR
    } catch (e: WriterException) {
      e.printStackTrace()
    }
    return null
  }
  
  /**
   * 用于向创建的二维码中添加一个login
   * @param bmQr 二维码对象
   * @param bmIcon 图标对象
   * @return bitmap 对象
   */
  fun addIcon(bmQr: Bitmap, bmIcon: Bitmap): Bitmap {
    // 获取图片的宽高
    val bmQrWidth = bmQr.width
    val bmQrHeight = bmQr.height
    val bmIconWidth = bmIcon.width
    val bmIconHeight = bmIcon.height
    
    // 设置icon的大小为二维码整体大小的1/5
    // 获得缩放比
    val scaleIcon = bmQrWidth * 1.0f / 5f / bmIconWidth.toFloat()
    // 获取与二维码相同大小的空白bitmap对象
    var bitmap: Bitmap? = Bitmap.createBitmap(bmQrWidth, bmQrHeight, Bitmap.Config.ARGB_8888)
    
    if (bitmap != null) {
      // 设置画纸
      val canvas = Canvas(bitmap)
      // 绘制图像
      canvas.drawBitmap(bmQr, 0.0f, 0.0f, null)
      // 设置缩放
      canvas.scale(scaleIcon, scaleIcon, (bmQrWidth / 2).toFloat(), (bmQrHeight / 2).toFloat())
      // 绘制Icon
      canvas.drawBitmap(bmIcon, ((bmQrWidth - bmIconWidth) / 2).toFloat(), ((bmQrHeight - bmIconHeight) / 2).toFloat(), null)
      // 保存
      canvas.save()
      // 重置
      canvas.restore()
    } else {
      // 如果画纸为空, 则直接返回当前二维码
      bitmap = bmQr
    }
    return bitmap
  }
}