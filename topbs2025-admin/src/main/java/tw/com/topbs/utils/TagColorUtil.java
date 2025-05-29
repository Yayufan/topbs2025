package tw.com.topbs.utils;

import java.awt.Color;

public class TagColorUtil {

	/**
	 * 用於計算相似顏色的tag color
	 * 
	 * 原色 #4A7056（一個深綠色） → 做「同色系明亮度/飽和度漸變」
	 * 
	 * 把顏色轉成 HSL（色相 Hue、飽和度 Saturation、亮度 Lightness）
	 * 
	 * 固定 Hue (色相不變，保持綠色)
	 * 
	 * 小幅調整 S / L (每個 group 差 5-10%)，產生相近色
	 * 
	 * @param hexColor    基本色
	 * @param groupIndex  群組角標(index)
	 * @param stepPercent 每組亮度+5%
	 * @return
	 */
	public static String adjustColor(String hexColor, int groupIndex, int stepPercent) {
		Color color = Color.decode(hexColor);

		// 轉 HSB (Hue, Saturation, Brightness)
		float[] hsbVals = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);

		// 增加亮度 (Brightness)
		float newBrightness = Math.min(1.0f, hsbVals[2] + (groupIndex - 1) * (stepPercent / 100f));

		// 轉回 RGB
		int rgb = Color.HSBtoRGB(hsbVals[0], hsbVals[1], newBrightness);

		// 格式化 Hex
		return String.format("#%06X", (0xFFFFFF & rgb));
	}

	
}
