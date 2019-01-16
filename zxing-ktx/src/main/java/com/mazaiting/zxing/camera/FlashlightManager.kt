package com.mazaiting.zxing.camera

import android.os.IBinder

import com.mazaiting.log.L

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * 闪光灯工具
 */
internal object FlashlightManager {
  /** 硬件服务 */
  private val mIHardwareService: Any?
  /** 设置闪光灯方法 */
  private val mSetFlashEnabledMethod: Method?
  /** 获取硬件服务 */
  private val mHardwareService: Any?
    get() {
      // 获取服务
      val serviceManagerClass = maybeForName("android.os.ServiceManager") ?: return null
      val getServiceMethod = maybeGetMethod(serviceManagerClass, "getService", String::class.java)
              ?: return null
      val hardwareService = invoke(getServiceMethod, null, "hardware") ?: return null
      val iHardwareServiceStubClass = maybeForName("android.os.IHardwareService\$Stub")
              ?: return null
      val asInterfaceMethod = maybeGetMethod(iHardwareServiceStubClass, "asInterface", IBinder::class.java)
              ?: return null
      return invoke(asInterfaceMethod, null, hardwareService)
    }
  
  init {
    // 设置服务
    mIHardwareService = mHardwareService
    // 获取设置方法
    mSetFlashEnabledMethod = getSetFlashEnabledMethod(mIHardwareService)
    if (mIHardwareService == null) {
      L.d("This device does supports control of a flashlight")
    } else {
      L.d("This device does not support control of a flashlight")
    }
  }
  
  /**
   * 设置开启闪光
   */
  //FIXME
  fun enableFlashlight() {
    setFlashlight(true)
  }
  
  /**
   * 关闭闪光灯
   */
  fun disableFlashlight() {
    setFlashlight(false)
  }
  
  /**
   * 获取设置闪光灯服务
   * @param iHardwareService 硬件服务
   * @return 返回方法
   */
  private fun getSetFlashEnabledMethod(iHardwareService: Any?): Method? {
    if (iHardwareService == null) {
      return null
    }
    // 获取类字节码
    val proxyClass = iHardwareService.javaClass
    return maybeGetMethod(proxyClass, "setFlashlightEnabled", Boolean::class.java)
  }
  
  /**
   * 可能的名称
   * @param name 名称
   * @return 类字节码
   */
  private fun maybeForName(name: String): Class<*>? {
    return try {
      Class.forName(name)
    } catch (cnfe: ClassNotFoundException) {
      // OK
      null
    } catch (re: RuntimeException) {
      L.d("Unexpected error while finding class " + name + ", re: " + re.message)
      null
    }
    
  }
  
  /**
   * 可能的方法
   * @param clazz 类字节码
   * @param name 名字
   * @param argClasses 参数
   * @return 方法名
   */
  private fun maybeGetMethod(clazz: Class<*>, name: String, vararg argClasses: Class<*>): Method? {
    return try {
      clazz.getMethod(name, *argClasses)
    } catch (nsme: NoSuchMethodException) {
      // OK
      null
    } catch (re: RuntimeException) {
      L.d("Unexpected error while finding method " + name + ", re: " + re.message)
      null
    }
    
  }
  
  /**
   * 执行方法
   * @param method 方法
   * @param instance 实例
   * @param args 参数
   * @return 方法返回的结果
   */
  private operator fun invoke(method: Method?, instance: Any?, vararg args: Any): Any? {
    return try {
      method!!.invoke(instance, *args)
    } catch (e: IllegalAccessException) {
      L.d("Unexpected error while invoking " + method + ", e: " + e.message)
      null
    } catch (e: RuntimeException) {
      L.d("Unexpected error while invoking " + method + ", e: " + e.message)
      null
    } catch (e: InvocationTargetException) {
      L.d("Unexpected error while invoking " + method + ",e: " + e.cause)
      null
    }
    
  }
  
  /**
   * 设置闪光灯
   * @param active 是否开启. true: 开启; false: 关闭
   */
  private fun setFlashlight(active: Boolean) {
    if (mIHardwareService != null) {
      invoke(mSetFlashEnabledMethod, mIHardwareService, active)
    }
  }
  
}
