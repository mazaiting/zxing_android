package com.mazaiting.zxing.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.HashMap;

/**
 * 编码管理者
 */
public class EncodeFormatManager {
  /**
   * 生成二维码
   * @param content 二维码内容
   * @param width 二维码宽高
   * @return Bitmap对象
   */
  public static Bitmap encodeQrCode(String content, int width) {
    try {
      //1,创建实例化对象
      QRCodeWriter writer = new QRCodeWriter();
      //2,设置字符集
      HashMap<EncodeHintType, String> map = new HashMap<>();
      map.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      //3,通过encode方法将内容写入矩阵对象
      BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, width, width, map);
      //4,定义一个二维码像素点的数组，向每个像素点中填充颜色
      int[] pixels = new int[width * width];
      //5,往每一像素点中填充颜色（像素没数据则用黑色填充，没有则用彩色填充，不过一般用白色）
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < width; j++) {
          if (matrix.get(j, i)) {
            pixels[i * width + j] = -0x1000000;
          } else {
            pixels[i * width + j] = -0x1;
          }
        }
      }
      //6,创建一个指定高度和宽度的空白bitmap对象
      Bitmap bmQR = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
      //7，将每个像素的颜色填充到bitmap对象
      bmQR.setPixels(pixels, 0, width, 0, 0, width, width);
    
      return bmQR;
    } catch (WriterException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * 用于向创建的二维码中添加一个login
   * @param bmQr 二维码对象
   * @param bmIcon 图标对象
   * @return bitmap 对象
   */
  public static Bitmap addIcon(Bitmap bmQr, Bitmap bmIcon) {
    //获取图片的宽高
    int bmQrWidth = bmQr.getWidth();
    int bmQrHeight = bmQr.getHeight();
    int bmIconWidth = bmIcon.getWidth();
    int bmIconHeight = bmIcon.getHeight();
  
    //设置login的大小为二维码整体大小的1/5
    float scaleLogin = bmQrWidth * 1.0f / 5f / bmIconWidth;
    Bitmap bitmap = Bitmap.createBitmap(bmQrWidth, bmQrHeight, Bitmap.Config.ARGB_8888);
  
    if (bitmap != null) {
      Canvas canvas = new Canvas(bitmap);
      canvas.drawBitmap(bmQr, 0.0f, 0.0f, null);
      canvas.scale(scaleLogin, scaleLogin, Float.intBitsToFloat(bmQrWidth / 2), Float.intBitsToFloat(bmQrHeight / 2));
      canvas.drawBitmap(bmIcon, Float.intBitsToFloat((bmQrWidth - bmIconWidth) / 2), Float.intBitsToFloat((bmQrHeight - bmIconHeight) / 2), null);
      
      canvas.save();
      canvas.restore();
    } else bitmap = bmQr;
  
    return bitmap;
  }
}
