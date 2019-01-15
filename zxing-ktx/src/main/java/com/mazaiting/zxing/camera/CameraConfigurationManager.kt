package com.mazaiting.zxing.camera

import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.view.WindowManager
import com.mazaiting.log.L
import com.mazaiting.zxing.BuildConfig
import java.util.*
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
   * Sets the camera up to take preview images which are used for both preview and decoding.
   * We detect the preview format here so that buildLuminanceSource() can build an appropriate
   * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
   * and the planar Y can be used for barcode scanning without a copy in some cases.
   */
  internal fun setDesiredCameraParameters(camera: Camera) {
    
    val parameters = camera.parameters
    
    if (parameters == null) {
      L.d("Device error: no camera parameters are available. Proceeding without configuration.")
      return
    }
    
    L.d("Initial camera parameters: " + parameters.flatten())
    
    //if (safeMode) {
    //  Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
    //}
    parameters.setPreviewSize(cameraResolution!!.x, cameraResolution!!.y)
    camera.parameters = parameters
    
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
    
    val afterSize = afterParameters.previewSize
    if (afterSize != null && (cameraResolution!!.x != afterSize.width || cameraResolution!!.y != afterSize
                    .height)) {
      L.d("Camera said it supported preview size " + cameraResolution!!.x + 'x'.toString() +
              cameraResolution!!.y + ", but after setting it, preview size is " + afterSize.width + 'x'.toString()
              + afterSize.height)
      cameraResolution!!.x = afterSize.width
      cameraResolution!!.y = afterSize.height
    }
    
    /** 设置相机预览为竖屏  */
    camera.setDisplayOrientation(90)
  }
  
  private fun setFlash(parameters: Camera.Parameters) {
    // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
    // And this is a hack-hack to work around a different value on the Behold II
    // Restrict Behold II check to Cupcake, per Samsung's advice
    //if (Build.MODEL.contains("Behold II") &&
    //    CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
    parameters.set("flash-value", 2)
    // This is the standard setting to turn the flash off that all devices should honor.
    parameters.set("flash-mode", "off")
  }
  
  private fun setZoom(parameters: Camera.Parameters) {
    
    val zoomSupportedString = parameters.get("zoom-supported")
    if (zoomSupportedString != null && !java.lang.Boolean.parseBoolean(zoomSupportedString)) {
      return
    }
    
    var tenDesiredZoom = TEN_DESIRED_ZOOM
    
    val maxZoomString = parameters.get("max-zoom")
    if (maxZoomString != null) {
      try {
        val tenMaxZoom = (10.0 * java.lang.Double.parseDouble(maxZoomString)).toInt()
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom
        }
      } catch (nfe: NumberFormatException) {
        L.d("Bad max-zoom: $maxZoomString")
      }
      
    }
    
    val takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max")
    if (takingPictureZoomMaxString != null) {
      try {
        val tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString)
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom
        }
      } catch (nfe: NumberFormatException) {
        L.d("Bad taking-picture-zoom-max: $takingPictureZoomMaxString")
      }
      
    }
    
    val motZoomValuesString = parameters.get("mot-zoom-values")
    if (motZoomValuesString != null) {
      tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom)
    }
    
    val motZoomStepString = parameters.get("mot-zoom-step")
    if (motZoomStepString != null) {
      try {
        val motZoomStep = java.lang.Double.parseDouble(motZoomStepString.trim { it <= ' ' })
        val tenZoomStep = (10.0 * motZoomStep).toInt()
        if (tenZoomStep > 1) {
          tenDesiredZoom -= tenDesiredZoom % tenZoomStep
        }
      } catch (nfe: NumberFormatException) {
        // continue
      }
      
    }
    
    // Set zoom. This helps encourage the user to pull back.
    // Some devices like the Behold have a zoom parameter
    if (maxZoomString != null || motZoomValuesString != null) {
      parameters.set("zoom", (tenDesiredZoom / 10.0).toString())
    }
    
    // Most devices, like the Hero, appear to expose this zoom parameter.
    // It takes on values like "27" which appears to mean 2.7x zoom
    if (takingPictureZoomMaxString != null) {
      parameters.set("taking-picture-zoom", tenDesiredZoom)
    }
  }
  
  
  /**
   * 从相机支持的分辨率中计算出最适合的预览界面尺寸
   *
   * @param parameters
   * @param screenResolution
   * @return
   */
  private fun findBestPreviewSizeValues(parameters: Camera.Parameters, screenResolution: Point): Point {
    val rawSupportedSizes = parameters.supportedPreviewSizes
    if (rawSupportedSizes == null) {
      L.d("Device returned no supported preview sizes; using default")
      val defaultSize = parameters.previewSize
      return Point(defaultSize.width, defaultSize.height)
    }
    
    // Sort by size, descending
    val supportedPreviewSizes = ArrayList(rawSupportedSizes)
    Collections.sort<Camera.Size>(supportedPreviewSizes, Comparator<Camera.Size> { a, b ->
      val aPixels = a.height * a.width
      val bPixels = b.height * b.width
      if (bPixels < aPixels) {
        return@Comparator -1
      }
      if (bPixels > aPixels) {
        1
      } else 0
    })
    
    if (BuildConfig.DEBUG) {
      val previewSizesString = StringBuilder()
      for (supportedPreviewSize in supportedPreviewSizes) {
        previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ')
      }
      L.d("Supported preview sizes: $previewSizesString")
    }

//    if (Log.isLoggable(TAG, Log.INFO)) {
//      val previewSizesString = StringBuilder()
//      for (supportedPreviewSize in supportedPreviewSizes) {
//        previewSizesString.append(supportedPreviewSize.width).append('x').append(supportedPreviewSize.height).append(' ')
//      }
//      Log.i(TAG, "Supported preview sizes: $previewSizesString")
//    }
    
    val screenAspectRatio = screenResolution.x.toDouble() / screenResolution.y.toDouble()
    
    // Remove sizes that are unsuitable
    val it = supportedPreviewSizes.iterator()
    while (it.hasNext()) {
      val supportedPreviewSize = it.next()
      val realWidth = supportedPreviewSize.width
      val realHeight = supportedPreviewSize.height
      if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
        it.remove()
        continue
      }
      
      val isCandidatePortrait = realWidth < realHeight
      val maybeFlippedWidth = if (isCandidatePortrait) realHeight else realWidth
      val maybeFlippedHeight = if (isCandidatePortrait) realWidth else realHeight
      
      val aspectRatio = maybeFlippedWidth.toDouble() / maybeFlippedHeight.toDouble()
      val distortion = Math.abs(aspectRatio - screenAspectRatio)
      if (distortion > MAX_ASPECT_DISTORTION) {
        it.remove()
        continue
      }
      
      if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
        val exactPoint = Point(realWidth, realHeight)
        L.d("Found preview size exactly matching screen size: $exactPoint")
        return exactPoint
      }
    }
    
    // If no exact match, use largest preview size. This was not a great
    // idea on older devices because
    // of the additional computation needed. We're likely to get here on
    // newer Android 4+ devices, where
    // the CPU is much more powerful.
    if (!supportedPreviewSizes.isEmpty()) {
      val largestPreview = supportedPreviewSizes[0]
      val largestSize = Point(largestPreview.width, largestPreview.height)
      L.d("Using largest suitable preview size: $largestSize")
      return largestSize
    }
    
    // If there is nothing at all suitable, return current preview size
    val defaultPreview = parameters.previewSize
    val defaultSize = Point(defaultPreview.width, defaultPreview.height)
    L.d("No suitable preview sizes, using default: $defaultSize")
    
    return defaultSize
  }
  
  companion object {
    //得到类的简写名称
    private val TAG = CameraConfigurationManager::class.java.simpleName
    
    private val MIN_PREVIEW_PIXELS = 480 * 320
    private val MAX_ASPECT_DISTORTION = 0.15
    //变焦
    private val TEN_DESIRED_ZOOM = 27
    //锐利度
    val desiredSharpness = 30
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
    
    //寻找到最适合的对焦值
    private fun findBestMotZoomValue(stringValues: CharSequence, tenDesiredZoom: Int): Int {
      var tenBestValue = 0
      for (stringValue in COMMA_PATTERN.split(stringValues)) {
        val sValue = stringValue.trim { it <= ' ' }
        val value: Double
        try {
          value = java.lang.Double.parseDouble(sValue)
        } catch (nfe: NumberFormatException) {
          return tenDesiredZoom
        }
        
        val tenValue = (10.0 * value).toInt()
        if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
          tenBestValue = tenValue
        }
      }
      return tenBestValue
    }
  }
  
}
