package tw.com.topbs.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QrcodeUtil {

	private static final String SECRET_KEY = "ThisIsA32ByteSecretKey1234567890"; // 32字節的密鑰

	public static byte[] generateQRCode(String text, int width, int height) throws WriterException, IOException {

		// 配置QRcode的額外設定, 這邊設定字符編碼為 UTF-8
		Map<EncodeHintType, Object> hints = new HashMap<>();
		hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());

		// 產生Qrcode編輯器的實例
		QRCodeWriter qrCodeWriter = new QRCodeWriter();

		// 使用 Jackson 將對象轉換為 JSON 字串
		// ObjectMapper objectMapper = new ObjectMapper();
		// String jsonString = objectMapper.writeValueAsString(sysUser);

		// 加密 JSON 字串
		// String encryptedString = encrypt(jsonString);
		String encryptedString = encrypt(text);

		// 使用QRCodeWriter將文本編碼為QR碼。這裡生成了一個BitMatrix，它是QR碼的二維表示
		BitMatrix bitMatrix = qrCodeWriter.encode(encryptedString, BarcodeFormat.QR_CODE, width, height, hints);

		// 創建一個ByteArrayOutputStream，用於存儲生成的PNG圖像數據。
		ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();

		// 使用MatrixToImageWriter將BitMatrix轉換為PNG圖像，並寫入到pngOutputStream。
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

		// System.out.println("這是解碼後的attendeesId" +
		// decrypt("r2uRCTLOZbIgRg3BWB9BaKNsc/pKQ0bNM+IjxKY6Mk8="));

		// 將ByteArrayOutputStream轉換為字節數組並返回。
		return pngOutputStream.toByteArray();
	}

	// AES 加密方法
	public static String encrypt(String strToEncrypt) {
		try {
			// 使用預定義的 SECRET_KEY 創建一個 AES 密鑰
			SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
			// 這裡創建了一個 AES 加密器，使用 ECB 模式和 PKCS5Padding 填充。
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			// 然後將加密器 cipher 帶入AES 密鑰 初始化為加密模式。
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			// cipher.doFinal(strToEncrypt.getBytes("UTF-8"))這行代碼執行實際的加密操作，將輸入字符串轉換為字節數組並加密。
			// Base64.getEncoder().encodeToString()將加密後的字節數組轉換為 Base64 編碼的字符串。
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}
		return null;
	}

	// AES 解密方法
	public static String decrypt(String encryptedString) {
		try {
			// 使用相同的 SECRET_KEY 創建 AES 密鑰。
			SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
			// 這裡創建了一個 AES 加密器，使用 ECB 模式和 PKCS5Padding 填充。
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			// 將加密器 Cipher 帶入AES 密鑰 初始化為解密模式。
			cipher.init(Cipher.DECRYPT_MODE, secretKey);

			// 將 Base64 編碼的加密字符串解碼為字節數組，然後使用 cipher 進行解密。
			// 基本上就是加密的順序反向操作
			byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedString));
			// 將解密後的字節數組轉換回字符串。
			return new String(decryptedBytes);
		} catch (Exception e) {
			System.out.println("解密錯誤: " + e.toString());
			return null;
		}
	}
}
