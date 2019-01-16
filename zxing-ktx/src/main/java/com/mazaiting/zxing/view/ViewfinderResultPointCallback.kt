package com.mazaiting.zxing.view

import com.google.zxing.ResultPoint
import com.google.zxing.ResultPointCallback

/**
 * 扫描框结果返回回调类
 * @param viewfinderView 扫描区域
 */
class ViewfinderResultPointCallback(private val viewfinderView: ViewfinderView) : ResultPointCallback {
  
  override fun foundPossibleResultPoint(point: ResultPoint) {
    viewfinderView.addPossibleResultPoint(point)
  }
  
}
