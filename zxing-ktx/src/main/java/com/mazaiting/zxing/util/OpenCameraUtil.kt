package com.mazaiting.zxing.util

import android.hardware.Camera
import com.mazaiting.log.L

/**
 * 开启相机接工具
 */
object OpenCameraUtil {
  
  /**
   * 开启后置摄像头
   * @return 已开启的照相机
   */
  fun open(): Camera? {
    return open(-1)
  }
  
  /**
   * 打开指定的摄像头
   * @param cameraId 照相机ID
   * @return 已开启的照相机
   */
  private fun open(cameraId: Int): Camera? {
    // 赋值相机ID
    var cId = cameraId
    // 获取照相机个数
    val numCameras = Camera.getNumberOfCameras()
    // 如果为0, 则说明没有照相机
    if (numCameras == 0) {
      L.d("No cameras!")
      return null
    }
    // 判断是否需要选择摄像头
    val isRequest = cId >= 0
    // 传入的相机ID小于0
    if (!isRequest) {
      // 循环遍历 - 选择后置相机
      for (i in 0..numCameras) {
        // 获取相机信息
        val cameraInfo = Camera.CameraInfo()
        // 根据id获取信息
        Camera.getCameraInfo(i, cameraInfo)
        // 判断是否为后置摄像头
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
          cId = i
          break
        }
      }
      
//      // 选择后置相机
//      var index = 0
//      while (index < numCameras) {
//        val cameraInfo = Camera.CameraInfo()
//        Camera.getCameraInfo(index, cameraInfo)
//        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//          break
//        }
//        index++
//      }
    }
    // 定义相机
    val camera: Camera?
    // 获取相机
    camera = if (cId < numCameras) {
      L.d("Opening camera #$cId")
      Camera.open(cId)
    } else {
      if (isRequest) {
        L.d("Requested camera does not exist: $cId")
        null
      } else {
        L.d("No camera facing back; returning camera #0")
        Camera.open(0)
      }
    }
    // 返回相机
    return camera
  }
  
}
