package com.mazaiting.zxingtest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mazaiting.zxing.CaptureActivity
import com.mazaiting.zxing.util.DecodeManager
import com.mazaiting.zxing.util.EncodeManager

class MainActivity : AppCompatActivity() {
  
  companion object {
    /** 二维码扫描 */
    private const val QR_CODE_QUERY = 0x100
    /** 请求权限代码 */
    private const val SCAN_PERMISSION_CODE = 0x1000
  }
  
  private lateinit var imageView: ImageView
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val etContent = this.findViewById<EditText>(R.id.et_content)
    val btnProduct = this.findViewById<Button>(R.id.btn_product)
    imageView = this.findViewById(R.id.imageView)
    val btnScan = this.findViewById<Button>(R.id.btn_scan)
    val btnLocalScan = this.findViewById<Button>(R.id.btn_local_scan)
//    requestPermission()
    // 生成二维码
    btnProduct.setOnClickListener {
      // 获取输入内容呢
      val content = etContent.text.toString()
      // 获取二维码
      val bitmap = EncodeManager.encodeQrCode(content, 1024)
      // 添加图标
      val iconBitmap = EncodeManager.addIcon(bitmap!!, BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
      // 设置图像
      imageView.setImageBitmap(iconBitmap)
    }
    
    // 扫描二维码
    btnScan.setOnClickListener {
      qrCodeScan()
    }
    
    // 本地图片识别
    btnLocalScan.setOnClickListener {
      // 本地图片识别
      localScan()
    }
  }
  
  /**
   * 请求必要权限
   */
  private fun requestPermission() {
    // 判断版本, 6.0以上请求必要权限
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      // 请求权限
      requestPermissionForStorageAndCamera()
    }
  }
  
  /**
   * 请求存储权限与照相机权限
   * Android 6.0以上的版本
   */
  @RequiresApi(api = Build.VERSION_CODES.M)
  private fun requestPermissionForStorageAndCamera() {
    val list = ArrayList<String>()
    // 检测是否具有写入内存卡权限
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    // 检测是否具有使用照相机权限
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      list.add(Manifest.permission.CAMERA)
    }
    if (list.size > 0) {
      // 请求权限, 参数1：将list转换为数组
      requestPermissions(list.toTypedArray(), SCAN_PERMISSION_CODE)
    }
  }
  
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (SCAN_PERMISSION_CODE == requestCode) {
//      Toast.makeText(this@MainActivity, "未授予相应的权限",Toast.LENGTH_SHORT).show()
    }
  }
  
  /**
   * 二维码扫描
   */
  private fun qrCodeScan() {
    val openCameraIntent = Intent(this@MainActivity, CaptureActivity::class.java)
    startActivityForResult(openCameraIntent, QR_CODE_QUERY)
  }
  
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      // 根据请求码判断
      when (requestCode) {
        QR_CODE_QUERY -> {
          // 获取二维码内容
          val result = data!!.getStringExtra(CaptureActivity.SCAN_RESULT_TEXT)
          Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
  
  /**
   * 本地图片识别
   */
  private fun localScan() {
    val text = DecodeManager.syncDecodeQRCode("${Environment.getExternalStorageDirectory()}/qrcode.png")
    Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
  }
}
