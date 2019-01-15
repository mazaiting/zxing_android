package com.mazaiting.zxing.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * 文件工具类
 */
object FileUtil {
  
  /**
   * 获取解码图片 -- 压缩读取, 防止内存溢出
   * @param path 文件路径
   * @return 位图对象
   */
  fun getDecodeBitmap(path: String): Bitmap? {
    return try {
      // 创建图片配置选项
      val options = BitmapFactory.Options()
      // 只读取图片, 不加载到内存中
      options.inJustDecodeBounds = true
      // 读取出图片的配置
      BitmapFactory.decodeFile(path, options)
      // 获取像素, 所占内存的压缩比
      var sampleSize = options.outHeight / 400
      // 如果高度的小于400, 则值为0
      if (sampleSize <= 0) sampleSize = 1
      // 设置压缩比
      options.inSampleSize = sampleSize
      // 设置加载到内存中
      options.inJustDecodeBounds = false
      // 读取图片
      BitmapFactory.decodeFile(path, options)
    } catch (e: Exception) { null }
  }
  
  /**
   * 获取解码图片 -- 压缩读取, 防止内存溢出
   * @param file 文件
   * @return 位图对象
   */
  fun getDecodeBitmap(file: File): Bitmap? = getDecodeBitmap(file.absolutePath)
  
}
