package com.mazaiting.zxing

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.mazaiting.log.L
import com.mazaiting.permission.PermissionResult
import com.mazaiting.permission.Permissions
import com.mazaiting.permission.util.PermissionSettingUtil
import com.mazaiting.permission.util.PermissionUtil
import com.mazaiting.permission.util.State
import com.mazaiting.zxing.camera.CameraManager
import com.mazaiting.zxing.util.InactivityTimer
import com.mazaiting.zxing.view.ViewfinderView
import java.io.IOException

/**
 * 扫描二维码界面
 * @use 使用方法
 * ```
 *  /**
     * 二维码扫描
     */
    private fun qrCodeScan() {
      val openCameraIntent = Intent(this@MainActivity, CaptureActivity::class.java)
      startActivityForResult(openCameraIntent, QR_CODE_QUERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      if (resultCode == Activity.RESULT_OK) {
        when (requestCode) {
          QR_CODE_QUERY -> {
            val result = data!!.getStringExtra("result")
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
          }
          else -> {}
        }
      }
    }
 * ```
 */
@Permissions([Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE], CaptureActivity.SCAN_PERMISSION_CODE)
class CaptureActivity : AppCompatActivity(), SurfaceHolder.Callback {
  
  companion object {
    /** 请求权限代码 */
    const val SCAN_PERMISSION_CODE = 0x1000
    /** 蜂鸣声时长 */
    private const val SCAN_BEEP_VOLUME = 0.10f
    /** 震动时长 */
    private const val SCAN_VIBRATE_DURATION = 200L
    /** 扫描结果键值 */
    const val SCAN_RESULT_TEXT = "result"
    /** 扫描结果图像 */
    const val SCAN_RESULT_BITMAP = "qr_code"
  }
  
  /** 扫描界面的Handler */
  private var mHandler: CaptureActivityHandler? = null
  /** 扫描框view */
  private var mViewfinderView: ViewfinderView? = null
  /** Surface是否可用 */
  private var isHasSurface: Boolean = false
  /** 计时器 */
  private var mInactivityTimer: InactivityTimer? = null
  /** 音频播放器 */
  private var mMediaPlayer: MediaPlayer? = null
  /** 是否播放声音 */
  private var isPlayBeep: Boolean = false
  /** 是否震动 */
  private var isVibrate: Boolean = false
  /** Surface控制器 */
  private var mSurfaceHolder: SurfaceHolder? = null
  
  /**
   * 播放监听, 播放完成后, 将进度置为0
   */
  private val mBeepListener = MediaPlayer.OnCompletionListener {
    // 将播放进度置0
    mediaPlayer -> mediaPlayer.seekTo(0)
  }
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 保持屏幕常亮
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//    initDebug()
//    // 请求权限
//    requestPermission()
    setContentView(R.layout.activity_capture)
    initDebug()
    // 请求权限
//    PermissionUtil.requestPermission(this)
//    requestPermission()
    // 相机管理者初始化
    CameraManager.init(application)
    // 初始化画中画
    mViewfinderView = this.findViewById(R.id.zxing_viewfinder_view)
    // 无Surface
    isHasSurface = false
    // 初始化定时器
    mInactivityTimer = InactivityTimer(this)
  }
  
  /**
   * 初始化Debug
   */
  private fun initDebug() {
    if (BuildConfig.DEBUG) {
      L.setProp(BuildConfig.DEBUG, "SCAN")
    }
  }
  
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    if (SCAN_PERMISSION_CODE == requestCode) {
//      // TODO 此时如果有拒绝的权限，则关闭当前界面
//      L.d("onRequestPermissionResult")
//    }
    PermissionUtil.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }
  
  /**
   * 权限请求结果处理
   * @param state 权限授权状态
   * @param permissions 当状态为Succeed时权限列表为空
   */
  @PermissionResult(SCAN_PERMISSION_CODE)
  fun permissionResult(state: State, permissions: List<String>?) {
    when (state) {
      State.DENIED -> // 再次请求权限
        PermissionUtil.requestPermission(this)
      State.SUCCESS -> // Toast.makeText(this, "权限申请成功!", Toast.LENGTH_SHORT).show()
        initCamera()
      State.NOT_SHOW -> notShow(permissions!!)
    }
  }
  
  /**
   * 点击不再提示后并拒绝
   */
  private fun notShow(permissions: List<String>) {
    val sb = StringBuilder()
    // 迭代读取权限
    permissions.forEach { permission ->
      sb.append("$permission\n")
    }
    // 跳转到应用设置界面
    AlertDialog.Builder(this)
            .setTitle("友情提示！")
            .setMessage("请跳转到设置界面同意下面权限：\n" + sb.toString())
            .setPositiveButton("同意") { _, _ ->
              // 跳转到应用设置界面
//              goToAppSetting(this)
              PermissionSettingUtil.gotoPermissionActivity(this)
            }
            .setNegativeButton("关闭") { _, _ ->
              // 关闭当前页面
              finish()
            }
            .setCancelable(false).show()
  }
  
  override fun onResume() {
    super.onResume()
    // 获取SurfaceView
    val surfaceView = findViewById<SurfaceView>(R.id.zxing_preview_view)
    // 获取SurfaceHolder
    val surfaceHolder = surfaceView.holder
    // 如果已经有Surface, 则进行相机初始化
    if (isHasSurface) {
      mSurfaceHolder = surfaceHolder
      // 初始化相机
      initCameraWithPermission()
    } else {
      // 添加回调
      surfaceHolder.addCallback(this)
      // 设置类型
//      mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
    // 设置允许蜂鸣
    isPlayBeep = true
    // 获取声音服务
    val audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    // 判断蜂鸣模式
    if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
      // 如果不为normal则不播放
      isPlayBeep = false
    }
    // 初始化蜂鸣声音
    initBeepSound()
    // 设置允许震动
    isVibrate = true
  }
  
  override fun onPause() {
    super.onPause()
    // 清除Handler
    if (mHandler != null) {
      // 同步退出
      mHandler!!.quitSynchronously()
      mHandler = null
    }
    // 关闭相机设备
    CameraManager.get().closeDriver()
  }
  
  override fun onDestroy() {
    // 关闭定时器
    mInactivityTimer!!.shutdown()
    super.onDestroy()
  }
  
  /**
   * 处理返回结果
   * @param result 扫描结果
   */
//  fun handleDecode(result: Result) {
  fun handleDecode(result: Result, bitmap: Bitmap) {
    // Activity处理结果
    mInactivityTimer!!.onActivity()
    // 播放声音和震动
    playBeepSoundAndVibrate()
    // 获取结果
    val resultString = result.text
    //FIXME
    if (resultString == "") {
      // 如果内容为空, 提示扫描失败
      Toast.makeText(this@CaptureActivity, resources.getString(R.string.scan_failed), Toast.LENGTH_SHORT).show()
    } else {
      // 设置数据
      val resultIntent = Intent()
      val bundle = Bundle()
      // 存储内容
      bundle.putString(SCAN_RESULT_TEXT, resultString)
      //TODO 待确认是否需要存储bitmap
      // 存储图片
//      bundle.putParcelable(SCAN_RESULT_BITMAP, bitmap)
      resultIntent.putExtras(bundle)
      // 设置扫描结果返回
      this.setResult(AppCompatActivity.RESULT_OK, resultIntent)
    }
    // 关闭当前页面
    this@CaptureActivity.finish()
  }
  
  /**
   * 初始化相机
   */
  private fun initCameraWithPermission() {
    PermissionUtil.requestPermission(this)
  }
  
  /**
   * 初始化相机
   */
  private fun initCamera() {
    try {
      // 打开设备
      mSurfaceHolder?.let { CameraManager.get().openDriver(it) }
    } catch (ioe: IOException) {
      return
    }
    // 如果Handler为空, 则初始化
    if (mHandler == null) {
      // 初始化Handler对象
      mHandler = CaptureActivityHandler(this)
    }
  }
  
  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
  
  override fun surfaceCreated(holder: SurfaceHolder) {
    // 判断Surface是否可用
    if (!isHasSurface) {
      isHasSurface = true
      mSurfaceHolder = holder
      // 初始化相机
      initCameraWithPermission()
    }
  }
  
  override fun surfaceDestroyed(holder: SurfaceHolder) {
    // surface销毁的时候, 将surface置为不可用
    isHasSurface = false
  }
  
  /** 获取ViewfinderView对象 */
  fun getViewfinderView(): ViewfinderView = mViewfinderView!!
  
  /** 返回Handler */
  fun getHandler(): Handler? = mHandler
  
  /** 获取画中画内容 */
  fun drawViewfinder() = mViewfinderView!!.drawViewfinder()
  
  /**
   * 初始化蜂鸣声音文件
   */
  private fun initBeepSound() {
    // 判断是否允许蜂鸣并且播放声音对象为空
    if (isPlayBeep && mMediaPlayer == null) {
      // 设置声音控制流为music, 此处为当前Activity中的
      volumeControlStream = AudioManager.STREAM_MUSIC
      // 初始化播放器
      mMediaPlayer = MediaPlayer()
      // 设置声音流类型
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mMediaPlayer!!.setAudioAttributes(AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build())
      } else {
        mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
      }
      // 设置播放完成监听
      mMediaPlayer!!.setOnCompletionListener(mBeepListener)
      //打开文件
      val file = resources.openRawResourceFd(R.raw.beep)
      try {
        // 设置声音源你
        mMediaPlayer!!.setDataSource(file.fileDescriptor,
                file.startOffset, file.length)
        // 关闭文件
        file.close()
        // 设置音量
        mMediaPlayer!!.setVolume(SCAN_BEEP_VOLUME, SCAN_BEEP_VOLUME)
        // 准备播放
        mMediaPlayer!!.prepare()
      } catch (e: IOException) {
        mMediaPlayer = null
      }
    }
  }
  
  /**
   * 播放声音和震动
   */
  private fun playBeepSoundAndVibrate() {
    // 判断是否播放蜂鸣
    if (isPlayBeep && mMediaPlayer != null) {
      mMediaPlayer!!.start()
    }
    // 判断是否震动
    if (isVibrate) {
      // 获取震动服务
      val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      // 震动
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(SCAN_VIBRATE_DURATION, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        vibrator.vibrate(SCAN_VIBRATE_DURATION)
      }
    }
  }
  
}
