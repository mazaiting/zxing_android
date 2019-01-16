package com.mazaiting.zxing.camera

import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.view.WindowManager
import com.mazaiting.log.L
import java.util.regex.Pattern

/**
 * 照相机配置管理类
 * @param context 上下文
 */
class CameraConfigurationManager internal constructor(private val context: Context) {
  /** 屏幕分辨率 */
  internal var screenResolution: Point? = null
    private set
  
  /** 摄像机分辨率 -- set方法私有化, get方法公有 */
  internal var cameraResolution: Point? = null
    private set
  /** 预览格式 */
  internal var previewFormat: Int = 0
    private set
  /** 预览格式字符串 */
  internal var previewFormatString: String? = null
    private set
  
  /**
   * 初始化相机参数
   * @param camera 参数
   */
  internal fun initFromCameraParameters(camera: Camera) {
    // 获取相机参数
    val parameters = camera.parameters
    // 获取预览格式
    previewFormat = parameters.previewFormat
    // 获取预览格式字符串
    previewFormatString = parameters.get("preview-format")
    L.d("Default preview format: $previewFormat/$previewFormatString")
    // 获取窗口管理者
    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    // 获取屏幕display 宽高
    val display = manager.defaultDisplay
    // 创建屏幕像素点
    screenResolution = Point()
    // 获取屏幕大小
    display.getSize(screenResolution)
    L.d("Screen resolution: $screenResolution")
    // 为相机创建屏幕分辨率, 这里不能直接将screeResolution赋值, 需要重新创建Point对象
    val screenResolutionForCamera = Point()
    // 赋值
    screenResolutionForCamera.x = screenResolution!!.x
    screenResolutionForCamera.y = screenResolution!!.y

    // 预览尺寸总是类似480*320, other 320*480, 如果y大于x,则交换数据
    if (screenResolution!!.x < screenResolution!!.y) {
      screenResolutionForCamera.x = screenResolution!!.y
      screenResolutionForCamera.y = screenResolution!!.x
    }
    // 获取相机区域
    cameraResolution = getCameraResolution(parameters, screenResolutionForCamera)
    
    L.d("Camera resolution: $screenResolution")
  }
    
  /**
   * 设置相机的期望参数
   * 设置用于预览的姐的相机图像, 我们在这里检测预览帧为了buildLuminanceSource()能够创建一个适当的LuminanceSource
   * 子类. 以后我们可能想去强制使用更小的YUV420SP, 并且Y平面被用于扫描码.
   */
  internal fun setDesiredCameraParameters(camera: Camera) {
    // 获取相机参数
    val parameters = camera.parameters
    // 判断参数是否为空
    if (parameters == null) {
      L.d("Device error: no camera parameters are available. Proceeding without configuration.")
      return
    }
    // 打印参数
    L.d("Initial camera parameters: " + parameters.flatten())
    
    //if (safeMode) {
    //  Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    //}
    // 设置预览大小
    parameters.setPreviewSize(cameraResolution!!.x, cameraResolution!!.y)
    camera.parameters = parameters
    // 获取参数
    val afterParameters = camera.parameters
    /*
    Log.d(TAG, "Setting preview size: " + cameraResolution);
    parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
    setFlash(parameters);
    setZoom(parameters);
    //setSharpness(parameters);
    //modify here
    camera.setDisplayOrientation(90);
    camera.setParameters(parameters);*/
    // 获取预览大小
    val afterSize = afterParameters.previewSize
    if (afterSize != null && (cameraResolution!!.x != afterSize.width || cameraResolution!!.y != afterSize
                    .height)) {
      L.d("Camera said it supported preview size " + cameraResolution!!.x + 'x'.toString() +
              cameraResolution!!.y + ", but after setting it, preview size is " + afterSize.width + 'x'.toString()
              + afterSize.height)
      cameraResolution!!.x = afterSize.width
      cameraResolution!!.y = afterSize.height
    }
    
    // 设置相机预览为竖屏
    camera.setDisplayOrientation(90)
  }
  
  /**
   * 设置闪光灯
   * @param parameters 相机参数
   */
  private fun setFlash(parameters: Camera.Parameters) {
    // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
    // And this is a hack-hack to work around a different value on the Behold II
    // Restrict Behold II check to Cupcake, per Samsung's advice
    //if (Build.MODEL.contains("Behold II") &&
    //    CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
    // 设置闪光值
    parameters.set("flash-value", 2)
    // This is the standard setting to turn the flash off that all devices should honor.
    //  设置模式
    parameters.set("flash-mode", "off")
  }
  
  /**
   * 设置缩放
   * @param parameters 相机参数
   */
  private fun setZoom(parameters: Camera.Parameters) {
    // 获取缩放所支持的字符串
    val zoomSupportedString: String? = parameters.get("zoom-supported")
    // 判断是否为空
    if (zoomSupportedString != null && !zoomSupportedString.toBoolean()) {
      return
    }
    // 初始化期望值
    var tenDesiredZoom = TEN_DESIRED_ZOOM
    // 获取最大缩放
    val maxZoomString = parameters.get("max-zoom")
    if (maxZoomString != null) {
      try {
        // 获取缩放值
        val tenMaxZoom = (10.0 * maxZoomString.toDouble()).toInt()
        // 判断是否小于期望值
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom
        }
      } catch (nfe: NumberFormatException) {
        L.d("Bad max-zoom: $maxZoomString")
      }
      
    }
    // 获取拍照缩放最大值
    val takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max")
    if (takingPictureZoomMaxString != null) {
      try {
        // 将字符串转为整型
        val tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString)
        // 判断是否小于期望值
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom
        }
      } catch (nfe: NumberFormatException) {
        L.d("Bad taking-picture-zoom-max: $takingPictureZoomMaxString")
      }
      
    }
    // 获取支持的缩放值
    val motZoomValuesString = parameters.get("mot-zoom-values")
    if (motZoomValuesString != null) {
      // 寻找最好的缩放值
      tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom)
    }
    // 获取缩放步长
    val motZoomStepString = parameters.get("mot-zoom-step")
    if (motZoomStepString != null) {
      try {
        val motZoomStep = motZoomStepString.trim { it <= ' ' }.toDouble()
        // 转为整型
        val tenZoomStep = (10.0 * motZoomStep).toInt()
        // 设置期望值
        if (tenZoomStep > 1) {
          tenDesiredZoom -= tenDesiredZoom % tenZoomStep
        }
      } catch (nfe: NumberFormatException) {
        // continue
      }
      
    }
    // 设置缩放, 帮助我们获取最好的图片
    if (maxZoomString != null || motZoomValuesString != null) {
      parameters.set("zoom", (tenDesiredZoom / 10.0).toString())
    }
    // 大多数设备, 像"27"意味着2.7x缩放
    if (takingPictureZoomMaxString != null) {
      parameters.set("taking-picture-zoom", tenDesiredZoom)
    }
  }
  
  
  companion object {
    /** 变焦 */
    private const val TEN_DESIRED_ZOOM = 27
    /** 正则表达 */
    private val COMMA_PATTERN = Pattern.compile(",")
  
    /**
     * 获取相机区域位置
     * @param parameters 相机参数
     * @param screenResolution 屏幕像素
     * @return 坐标
     */
    private fun getCameraResolution(parameters: Camera.Parameters, screenResolution: Point): Point {
      // 获取预览值
      var previewSizeValueString: String? = parameters.get("preview-size-values")
      // 如果从`preview-size-values`没有取到值, 则从`preview-size-value`中取
      if (previewSizeValueString == null) {
        previewSizeValueString = parameters.get("preview-size-value")
      }
      // 创建相机区域
      var cameraResolution: Point? = null
      // 寻找最好的预览值
      if (previewSizeValueString != null) {
        L.d("preview-size-values parameter: $previewSizeValueString")
        cameraResolution = findBestPreviewSizeValue(previewSizeValueString, screenResolution)
      }
      // 判断最好的预览值是否为空
      if (cameraResolution == null) {
        // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
        // 将屏幕坐标右移3位后左移3位
        cameraResolution = Point(
                screenResolution.x shr 3 shl 3,
                screenResolution.y shr 3 shl 3)
      }
      // 返回相机区域
      return cameraResolution
    }
  
    /**
     * 寻找最好的预览值
     * @param previewSizeValueString 预览值
     * @param screenResolution 屏幕像素
     * @return 宽高点
     */
    private fun findBestPreviewSizeValue(previewSizeValueString: CharSequence, screenResolution: Point): Point? {
      // 最好的宽
      var bestX = 0
      // 最好的长
      var bestY = 0
      // 设置不同为整型最大值
      var diff = Integer.MAX_VALUE
      // 将预览值使用`,`分割, 并遍历
      for (previewSize in COMMA_PATTERN.split(previewSizeValueString)) {
        // 取出空格
        val size = previewSize.trim { it <= ' ' }
        // 获取值
        val dimPosition = size.indexOf('x')
        // 如果不符合标准, 则跳出此处循环
        if (dimPosition < 0) {
          L.d("Bad preview-size: $size")
          continue
        }
        // 创建新的x和y
        val newX: Int
        val newY: Int
        try {
          // 获取x和y
          newX = Integer.parseInt(size.substring(0, dimPosition))
          newY = Integer.parseInt(size.substring(dimPosition + 1))
        } catch (nfe: NumberFormatException) {
          L.d("Bad preview-size: $size")
          continue
        }
        // 计算绝对值之和
        val newDiff = Math.abs(newX - screenResolution.x) + Math.abs(newY - screenResolution.y)
        // 等于0为最好的预览长宽
        if (newDiff == 0) {
          // 赋值, 表示已找到最好的长宽
          bestX = newX
          bestY = newY
          break
        } else if (newDiff < diff) {
          // 判断差值是否小于上一次的差值, 如果是则赋值, 并更新差值
          bestX = newX
          bestY = newY
          diff = newDiff
        }
        
      }
      // 判断最好的预览长宽是否大于0, 大于0则返回长宽, 否则返回null
      return if (bestX > 0 && bestY > 0) {
        Point(bestX, bestY)
      } else null
    }
  
    /**
     * 寻找到最适合的对焦值
     * @param stringValues 可选值
     * @param tenDesiredZoom 对焦值
     * @return 最好的对焦值
     */
    private fun findBestMotZoomValue(stringValues: CharSequence, tenDesiredZoom: Int): Int {
      // 设置最好的对焦值默认为0
      var tenBestValue = 0
      // 根据正则分割字符串, 并遍历
      for (stringValue in COMMA_PATTERN.split(stringValues)) {
        // 去除空格
        val sValue = stringValue.trim { it <= ' ' }
        // 定义值
        val value: Double
        try {
          // 转换为Double雷兴国
//          value = Double.parseDouble(sValue)
          value = sValue.toDouble()
        } catch (nfe: NumberFormatException) {
          // 数字格式化异常, 直接返回
          return tenDesiredZoom
        }
        // 转换为整型
        val tenValue = (10.0 * value).toInt()
        // 判断绝对值之差, 检测是否为最好的对焦值
        if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
          tenBestValue = tenValue
        }
      }
      // 返回最好的对焦值
      return tenBestValue
    }
  }
  
}
