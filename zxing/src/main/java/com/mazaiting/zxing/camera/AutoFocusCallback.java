package com.mazaiting.zxing.camera;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
//自动对焦回调
final class AutoFocusCallback implements Camera.AutoFocusCallback {

  private static final String TAG = AutoFocusCallback.class.getSimpleName();
  //自动对焦延时
  private static final long AUTOFOCUS_INTERVAL_MS = 1500L;
  //自动对焦Handler
  private Handler autoFocusHandler;
  //传入通知Handler的Message
  private int autoFocusMessage;

  //传入自动对焦Handler和自动对焦信息Message
  void setHandler(Handler autoFocusHandler, int autoFocusMessage) {
    this.autoFocusHandler = autoFocusHandler;
    this.autoFocusMessage = autoFocusMessage;
  }

  //自动对焦函数
  public void onAutoFocus(boolean success, Camera camera) {
    if (autoFocusHandler != null) {
      Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
      autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
      autoFocusHandler = null;
    } else {
      Log.d(TAG, "Got auto-focus callback, but no handler for it");
    }
  }

}
