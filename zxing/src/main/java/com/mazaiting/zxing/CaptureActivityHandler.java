package com.mazaiting.zxing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.mazaiting.zxing.camera.CameraManager;
import com.mazaiting.zxing.util.DecodeThread;
import com.mazaiting.zxing.view.ViewfinderResultPointCallback;

import java.util.Vector;

/**
 * This class handles all the messaging which comprises the state machine for activity_capture.
 */
public final class CaptureActivityHandler extends Handler {

  private static final String TAG = CaptureActivityHandler.class.getSimpleName();

  private final CaptureActivity activity;
  private final DecodeThread decodeThread;
  private State state;

  //枚举类 关于状态
  private enum State {
    PREVIEW,
    SUCCESS,
    DONE
  }

  public CaptureActivityHandler(CaptureActivity activity, Vector<BarcodeFormat> decodeFormats,
      String characterSet) {
    this.activity = activity;
    decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
        new ViewfinderResultPointCallback(activity.getViewfinderView()));
    decodeThread.start();
    state = State.SUCCESS;
    // Start ourselves capturing previews and decoding.
    CameraManager.get().startPreview();
    restartPreviewAndDecode();
  }

  //根据message所传回的信息，做出对应动作
  @Override
  public void handleMessage(Message message) {
    if (message.what == R.id.zxing_auto_focus) {
      //Log.d(TAG, "Got auto-focus message");
      // When one auto focus pass finishes, start another. This is the closest thing to
      // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
      if (state == State.PREVIEW) {
        CameraManager.get().requestAutoFocus(this, R.id.zxing_auto_focus);
      }
    
    } else if (message.what == R.id.zxing_restart_preview) {
      Log.d(TAG, "Got restart preview message");
      restartPreviewAndDecode();
    
    } else if (message.what == R.id.zxing_decode_succeeded) {
      Log.d(TAG, "Got decode succeeded message");
      state = State.SUCCESS;
      Bundle bundle = message.getData();
    
      //***********************************************************************
      Bitmap barcode = bundle == null ? null :
              (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
    
      activity.handleDecode((Result) message.obj, barcode);
      //***********************************************************************
    
    } else if (message.what == R.id.zxing_decode_failed) {// We're decoding as fast as possible, so when one decode fails, start another.
      state = State.PREVIEW;
      CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.zxing_decode);
    
      //返回扫描结果通知前一个activity
    } else if (message.what == R.id.zxing_return_scan_result) {
      Log.d(TAG, "Got return scan result message");
      activity.setResult(Activity.RESULT_OK, (Intent) message.obj);
      activity.finish();
    
    } else if (message.what == R.id.zxing_launch_product_query) {
      Log.d(TAG, "Got product query message");
      String url = (String) message.obj;
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      activity.startActivity(intent);
    
    }
  }

  public void quitSynchronously() {
    state = State.DONE;
    CameraManager.get().stopPreview();
    Message quit = Message.obtain(decodeThread.getHandler(), R.id.zxing_quit);
    quit.sendToTarget();
    try {
      decodeThread.join();
    } catch (InterruptedException e) {
      // continue
    }

    // Be absolutely sure we don't send any queued up messages
    removeMessages(R.id.zxing_decode_succeeded);
    removeMessages(R.id.zxing_decode_failed);
  }

  private void restartPreviewAndDecode() {
    if (state == State.SUCCESS) {
      state = State.PREVIEW;
      CameraManager.get().requestPreviewFrame(decodeThread.getHandler(), R.id.zxing_decode);
      CameraManager.get().requestAutoFocus(this, R.id.zxing_auto_focus);
      activity.drawViewfinder();
    }
  }

}
