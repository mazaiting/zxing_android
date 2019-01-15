# zxing_android
This is a Qrcode encode and decode library.

#### Use Library
1. Encode QrCode
```
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
```
2. Decode QrCode with Camera
```
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
```
3. Decode QrCode with local picture
```
  /**
   * 本地图片识别
   */
  private fun localScan() {
    val text = DecodeManager.syncDecodeQRCode("${Environment.getExternalStorageDirectory()}/qrcode.png")
    Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
  }
```