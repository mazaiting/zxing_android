package com.mazaiting.zxing

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.os.Parcelable
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.mazaiting.log.L
import com.mazaiting.zxing.camera.CameraManager
import com.mazaiting.zxing.constant.*
import com.mazaiting.zxing.util.DecodeThread
import com.mazaiting.zxing.view.ViewfinderResultPointCallback
import java.util.Vector

/**
 * 根据状态处理CaptureActivity中的所有消息
 * @param activity 要处理的页面
 * @extend 继承自Handler
 */
class CaptureActivityHandler(private val activity: CaptureActivity) : Handler() {
  /** 解码线程 */
  private val mDecodeThread: DecodeThread = DecodeThread(activity,
          ViewfinderResultPointCallback(activity.getViewfinderView()))
  /** 扫描状态 */
  private var state: State? = null
  
  /**
   * 枚举类 状态枚举
   */
  private enum class State {
    /** 预览 */
    PREVIEW,
    /** 成功 */
    SUCCESS,
    /** 结束 */
    DONE
  }
  
  init {
    // 线程启动
    mDecodeThread.start()
    // 初始化初始化状态
    state = State.SUCCESS
    // 开启预览并开始解码
    CameraManager.get().startPreview()
    // 重启预览和解码
    restartPreviewAndDecode()
  }
  
  /** 根据message所传回的信息，做出对应动作*/
  override fun handleMessage(message: Message) {
    when (message.what) {
      // 收到自动对焦消息
      AUTO_FOCUS -> {
        L.d("Got auto-focus message")
        // 判断状态为预览
        if (state == State.PREVIEW) CameraManager.get().requestAutoFocus(this, R.id.zxing_auto_focus)
      }
      // 收到重新预览消息
      RESTART_PREVIEW -> {
        L.d("Got restart preview message")
        // 重新预览并解码
        restartPreviewAndDecode()
      }
      // 收到解码成功消息
      DECODE_SUCCEEDED -> {
        L.d("Got decode succeeded message")
        // 设置解码状态为成功
        state = State.SUCCESS
        // 获取消息内容
        val bundle = message.data
        // 获取二维码图像
        val barcode = bundle?.getParcelable<Parcelable>(DecodeThread.BARCODE_BITMAP)
        // 方法消息
        activity.handleDecode(message.obj as Result, (barcode as Bitmap?)!!)
//        activity.handleDecode(message.obj as Result)
      }
      // 收到解码失败消息
      DECODE_FAILED -> {
        // We're decoding as fast as possible, so when one decode fails, start another.
        state = State.PREVIEW
        CameraManager.get().requestPreviewFrame(mDecodeThread.handler, DECODE)
      }
      // 收到解码返回结果消息
      RETURN_SCAN_RESULT -> {
        L.d("Got return scan result message")
        activity.setResult(Activity.RESULT_OK, message.obj as Intent)
        activity.finish()
      }
      // 收到启动产品查询消息
      LAUNCH_PRODUCT_QUERY -> {
        L.d("Got product query message")
        val url = message.obj as String
        // 设置URL
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        } else {
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        }
        activity.startActivity(intent)
      }
    }
//    if (message.what == R.id.zxing_auto_focus) {
//      // 收到自动对焦消息
//      L.d("Got auto-focus message");
//      // 判断状态为预览
//      if (state == State.PREVIEW) {
//        // 请求自动对象
//        CameraManager.get().requestAutoFocus(this, R.id.zxing_auto_focus)
//      }
//    } else if (message.what == R.id.zxing_restart_preview) {
//      // 收到重新预览消息
//      L.d("Got restart preview message")
//      // 重新预览并解码
//      restartPreviewAndDecode()
//    } else if (message.what == R.id.zxing_decode_succeeded) {
//      // 收到解码成功消息
//      L.d("Got decode succeeded message")
//      // 设置解码状态为成功
//      state = State.SUCCESS
//      // 获取消息内容
//      val bundle = message.data
//      // 获取二维码图像
//      val barcode = bundle?.getParcelable<Parcelable>(DecodeThread.BARCODE_BITMAP)
//      // 方法消息
//      activity.handleDecode(message.obj as Result, (barcode as Bitmap?)!!)
//    } else if (message.what == R.id.zxing_decode_failed) {// We're decoding as fast as possible, so when one decode fails, start another.
//      // 收到解码失败消息
//      state = State.PREVIEW
//      CameraManager.get().requestPreviewFrame(mDecodeThread.handler, R.id.zxing_decode)
//      //返回扫描结果通知前一个activity
//    } else if (message.what == R.id.zxing_return_scan_result) {
//      // 收到解码返回结果消息
//      L.d("Got return scan result message")
//      activity.setResult(Activity.RESULT_OK, message.obj as Intent)
//      activity.finish()
//    } else if (message.what == R.id.zxing_launch_product_query) {
//      // 收到启动产品查询消息
//      L.d("Got product query message")
//      val url = message.obj as String
//      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
//      activity.startActivity(intent)
//    }
  }
  
  /**
   * 同步退出
   */
  fun quitSynchronously() {
    // 扫描结束
    state = State.DONE
    // 停止预览
    CameraManager.get().stopPreview()
    // 生成退出消息
    val quit = Message.obtain(mDecodeThread.handler, QUIT)
    // 发送消息
    quit.sendToTarget()
    try {
      // 进入线程排队
      mDecodeThread.join()
    } catch (e: InterruptedException) {
      // 继续
      L.d(e.message)
    }
    
    // 移除消息
    removeMessages(DECODE_SUCCEEDED)
    removeMessages(DECODE_FAILED)
  }
  
  /**
   * 重新预览并解码
   */
  private fun restartPreviewAndDecode() {
    // 判断状态是否为成功
    if (state == State.SUCCESS) {
      // 状态设置为预览
      state = State.PREVIEW
      // 请求预览帧
      CameraManager.get().requestPreviewFrame(mDecodeThread.handler, DECODE)
      // 请求自动对焦
      CameraManager.get().requestAutoFocus(this, AUTO_FOCUS)
      // 绘制画中画
      activity.drawViewfinder()
    }
  }
}
