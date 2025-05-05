package tw.com.topbs.utils;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupUtil {

	/**
	 * 提取HTML Img標籤的src屬性 
	 * @param html
	 * @return
	 */
    public static List<String> extractImageUrls(String html) {
        List<String> imageUrls = new ArrayList<>();
        
        Document doc = Jsoup.parse(html);
        Elements imgElements = doc.select("img");
        
        for (Element img : imgElements) {
            String src = img.attr("src");
            if (!src.isEmpty()) {
                imageUrls.add(src);
            }
        }
        
        return imageUrls;
    }
	
}
