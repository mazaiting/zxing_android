package com.mazaiting.zxing.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.Camera
import android.os.Handler
import android.text.TextUtils
import android.view.SurfaceHolder
import com.mazaiting.log.L
import com.mazaiting.zxing.util.OpenCameraUtil
import com.mazaiting.zxing.listener.AutoFocusCallback
import com.mazaiting.zxing.listener.PreviewCallback
import java.io.IOException

/**
 * 照相机管理者
 * @param context 上下文
 */
class CameraManager private constructor(val context: Context) {
  
  companion object {
    /** 取景框最小宽度 */
    private const val MIN_FRAME_WIDTH = 400
    /** 取景框最小高度 */
    private const val MIN_FRAME_HEIGHT = 400
    /** 取景框最大宽度 */
    private const val MAX_FRAME_WIDTH = 500
    /** 取景框最大高度 */
    private const val MAX_FRAME_HEIGHT = 500
    /** 照相机管理类, 此处上下文为Application,因此不会造成内存泄漏 */
    @SuppressLint("StaticFieldLeak")
    private var sCameraManager: CameraManager? = null
    
    /**
     * 采用单例模式，只允许有一个CameraManager管理类
     * @param context 上下文
     */
    fun init(context: Context) {
      if (sCameraManager == null) {
        // 创建相机管理者
        sCameraManager = CameraManager(context)
      }
    }
    
    /**
     * 获取CameraManager单例对象
     * @return CameraManager单例对象
     */
    fun get(): CameraManager = sCameraManager!!
  }
  
  /** 相机管理者 */
  private val mConfigManager: CameraConfigurationManager = CameraConfigurationManager(context)
  /** 照相机 */
  private var mCamera: Camera? = null
  /** 帧矩形 */
  private var mFramingRect: Rect? = null
  /** 预览帧矩形 */
  private var mFramingRectInPreview: Rect? = null
  /** 是否初始化 */
  private var isInitialized: Boolean = false
  /** 是否正在预览 */
  private var isPreviewing: Boolean = false
  /** 预览回调 */
  private val mPreviewCallback: PreviewCallback
  /** 自动对焦回调  */
  private val mAutoFocusCallback: AutoFocusCallback
  
  init {
    // 初始化预览回调
    mPreviewCallback = PreviewCallback(mConfigManager)
    // 初始化自动对焦回调
    mAutoFocusCallback = AutoFocusCallback()
  }
  
  /**
   * 打开摄像头驱动和初始化硬件参数。
   * @param holder 传入的这个是camera将要绘制的frames  holder类
   * @throws IOException 指示这个camera打开失败
   */
  @Throws(IOException::class)
  fun openDriver(holder: SurfaceHolder) {
    /**
     * if (mCamera == null) {
     * mCamera = Camera.open();
     * if (mCamera == null) {
     * throw new IOException();
     * }
     * mCamera.setPreviewDisplay(holder);
     * //在设置横屏时相机旋转90°
     * //mCamera.setDisplayOrientation(180);
     *
     * if (!isInitialized) {
     * isInitialized = true;
     * mConfigManager.initFromCameraParameters(mCamera);
     * }
     * mConfigManager.setDesiredCameraParameters(mCamera);
     *
     * FlashlightManager.enableFlashlight();
     * } */
    // 获取相机
    var camera = mCamera
    // 判断相机是否为空
    if (camera == null) {
      // 开启相机
      camera = OpenCameraUtil.open()
      // 如果打开失败, 则抛出异常
      if (camera == null) {
        throw IOException()
      }
      // 赋值相机
      mCamera = camera
    }
    // 设置预览Holder
    camera.setPreviewDisplay(holder)
    // 是否初始化化
    if (!isInitialized) {
      // 设置已经初始化参数
      isInitialized = true
      // 初始化相机参数
      mConfigManager.initFromCameraParameters(camera)
    }
    try {
      // 设置期望相机参数
      mConfigManager.setDesiredCameraParameters(camera)
    } catch (re: RuntimeException) {
      L.d("Camera rejected parameters. Setting only minimal safe-mode parameters")
      // 获取相机参数
      var parameters: Camera.Parameters? = mCamera?.parameters
      // 获取所有参数
      val parametersFlattened = parameters?.flatten() // Save
      // 设置失败
      L.d("Resetting to saved mCamera params: $parametersFlattened")
      // 重置相机参数
      if (!TextUtils.isEmpty(parametersFlattened)) {
        // 获取参数
        parameters = camera.parameters
        // 将字符串解码为键值
        parameters!!.unflatten(parametersFlattened)
        try {
          // 赋值参数
          camera.parameters = parameters
          // 设置期望相机参数
          mConfigManager.setDesiredCameraParameters(camera)
        } catch (re2: RuntimeException) {
          // 放弃设置相机参数
          L.d("Camera rejected even safe-mode parameters! No configuration")
        }
      }
    }
    // 开启闪光灯
//    FlashlightManager.enableFlashlight()
  }
  
  /**
   * 如果仍在使用时关闭这个camera
   */
  fun closeDriver() {
    if (mCamera != null) {
      // 关闭手电筒
      FlashlightManager.disableFlashlight()
      // 释放相机
      mCamera!!.release()
      mCamera = null
    }
  }
  
  /**
   * 开始预览图像
   */
  fun startPreview() {
    // 判断相机设备是否存在且此时未预览
    if (mCamera != null && !isPreviewing) {
      // 开始预览
      mCamera!!.startPreview()
      // 正在预览
      isPreviewing = true
    }
  }
  
  /**
   * 通知Camera停止绘制图像
   */
  fun stopPreview() {
    // 判断相机设备是否为空并且是否正在预览
    if (mCamera != null && isPreviewing) {
      // 停止预览
      mCamera?.stopPreview()
      // 设置预览的消息处理器为空
      mPreviewCallback.setHandler(null, 0)
      // 设置自动对焦的消息处理器为空
      mAutoFocusCallback.setHandler(null, 0)
      //
      isPreviewing = false
    }
  }
  
  /**
   * 请求预览帧
   * @param handler 消息处理器
   * @param message 消息
   */
  fun requestPreviewFrame(handler: Handler?, message: Int) {
    // 判断相机设备和是否正在预览
    if (mCamera != null && isPreviewing) {
      // 预览回调
      mPreviewCallback.setHandler(handler, message)
      // 单次预览
      mCamera!!.setOneShotPreviewCallback(mPreviewCallback)
    }
  }
  
  /**
   * 请求照相机硬件自动对焦
   * @param handler 自动对焦完成通知Handler
   * @param message 待分发的消息
   */
  fun requestAutoFocus(handler: Handler, message: Int) {
    // 判断照相机是否为空, 当前是否正在预览
    if (mCamera != null && isPreviewing) {
      // 自动对焦回调设置消息处理器
      mAutoFocusCallback.setHandler(handler, message)
      L.d("Requesting auto-focus callback")
      // 照相机设置自动对焦回调
      mCamera!!.autoFocus(mAutoFocusCallback)
    }
  }
  
  /**
   * 计算二维码UI帧矩阵, 目的是帮助我们聚焦时更好的对齐
   * @return 绘制矩形屏幕坐标
   */
  fun getFramingRect(): Rect? {
    // 判断帧矩阵是否为空
    if (mFramingRect == null) {
      // 判断相机是否为空
      if (mCamera == null) {
        return null
      }
      // 获取屏幕像素
      val screenResolution = mConfigManager.screenResolution!!
      // 获取3/4的宽度
      var width = screenResolution.x * 3 / 4
      // 判断宽度是否越界
      if (width < MIN_FRAME_WIDTH) width = MIN_FRAME_WIDTH
      else if (width > MAX_FRAME_WIDTH) width = MAX_FRAME_WIDTH
      // 获取3/4高度
      var height = screenResolution.y * 3 / 4
      // 计算高度是否越界
      if (height < MIN_FRAME_HEIGHT) height = MIN_FRAME_HEIGHT
      else if (height > MAX_FRAME_HEIGHT) height = MAX_FRAME_HEIGHT
      // 计算左侧偏移量
      val leftOffset = (screenResolution.x - width) / 2
      // 计算顶部偏移量
      val topOffset = (screenResolution.y - height) / 2
      // 获取帧矩形
      mFramingRect = Rect(leftOffset, topOffset, leftOffset + width, topOffset + height)
      L.d("Calculated framing rect: $mFramingRect")
    }
    return mFramingRect
  }
  
  /**
   * 类似[.getFramingRect], 但是坐标是预览帧中的, 不是UI或者屏幕
   * @return 矩形对象
   */
  private fun getFramingRectInPreview(): Rect {
    // 判断预览帧矩形是否为空
    if (mFramingRectInPreview == null) {
      // 创建矩形
      val rect = Rect(getFramingRect())
      // 获取照相机分辨率
      val cameraResolution = mConfigManager.cameraResolution!!
      // 获取屏幕分辨率
      val screenResolution = mConfigManager.screenResolution!!
      // 获取上下左右坐标点
      rect.left = rect.left * cameraResolution.y / screenResolution.x
      rect.right = rect.right * cameraResolution.y / screenResolution.x
      rect.top = rect.top * cameraResolution.x / screenResolution.y
      rect.bottom = rect.bottom * cameraResolution.x / screenResolution.y
      // 赋值
      mFramingRectInPreview = rect
    }
    return mFramingRectInPreview!!
  }
  
  /**
   * 基于预览缓冲格式创建适当的亮度源对象的工厂方法, 正如所述Camera.Parameters
   * @param data 预览帧
   * @param width 图像宽度
   * @param height 图像高度
   * @return PlanarYUVLuminanceSource 实例
   */
  fun buildLuminanceSource(data: ByteArray, width: Int, height: Int): PlanarYUVLuminanceSource {
    // 获取帧预览图像
    val rect = getFramingRectInPreview()
    // 获取预览格式
    val previewFormat = mConfigManager.previewFormat
    // 获取预览格式字符串
    val previewFormatString = mConfigManager.previewFormatString
    // 根据预览格式选择
    when (previewFormat) {
      // 标准的Android格式--所有的设备支持。理论上，我们并不关心它
//      PixelFormat.YCbCr_420_SP,
      ImageFormat.NV21,
        // 这个格式并未广泛使用, 但它兼容我们关心的Y通道, 因此使用它
//      PixelFormat.YCbCr_422_SP,
      ImageFormat.NV16 -> return PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
              rect.width(), rect.height())
      else ->
        // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
        // Fortunately, it too has all the Y data up front, so we can read it.
        // 三星使用previewFormat变量不正确, 替代的'sp'这个版本.
        // 幸运的是, 它事先提供了Y数据, 所以我们可以读取它
        if ("yuv420p" == previewFormatString) {
          return PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                  rect.width(), rect.height())
        }
    }
    throw IllegalArgumentException("Unsupported picture format: $previewFormat/$previewFormatString")
  }
}
