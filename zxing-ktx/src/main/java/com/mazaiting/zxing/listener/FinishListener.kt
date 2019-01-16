package com.mazaiting.zxing.listener

import android.app.Activity
import android.content.DialogInterface

/**
 * 退出app监听
 * @param activity Activity
 */
class FinishListener(private val activity: Activity) : DialogInterface.OnClickListener, DialogInterface.OnCancelListener, Runnable {
  
  override fun onCancel(dialogInterface: DialogInterface) {
    run()
  }
  
  override fun onClick(dialogInterface: DialogInterface, i: Int) {
    run()
  }
  
  override fun run() {
    // 关闭当前界面
    activity.finish()
  }
}
