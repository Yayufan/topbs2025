package tw.com.topbs.utils;

import java.util.Locale;

public class CountryUtil {
	
	//設定常數本國人,此地方為台灣
	private static final String NATIONAL = "Taiwan";
	
	 /**
     * 將輸入國家名稱標準化為：首字母大寫，其餘小寫
     * Ex: "taiWAN" -> "Taiwan"
     */
    public static String normalize(String country) {
        if (country == null || country.isBlank()) return "";
        String trimmed = country.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.ROOT)
                + trimmed.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * 判斷是否為台灣本國人
     * 不區分大小寫，會自動 normalize 再比較
     */
    public static Boolean isNational(String country) {
        return NATIONAL.equals(normalize(country));
    }

    /**
     * 取得標準的國家名稱常數（如未來要統一使用 enum 可集中管理）
     */
    public static String getNational() {
        return NATIONAL;
    }
}
