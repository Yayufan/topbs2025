package ecpay.payment.integration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ecpay.payment.integration.ecpayOperator.EcpayFunction;
import ecpay.payment.integration.errorMsg.ErrorMessage;
import ecpay.payment.integration.exception.EcpayException;

public class AllInOneBase {
	protected static String operatingMode;
	protected static String mercProfile;
	protected static String isProjectContractor;
	protected static String HashKey;
	protected static String HashIV;
	protected static String MerchantID;
	protected static String PlatformID;
	protected static String aioCheckOutUrl;
	protected static String doActionUrl;
	protected static String queryCreditTradeUrl;
	protected static String queryTradeInfoUrl;
	protected static String queryTradeUrl;
	protected static String tradeNoAioUrl;
	protected static String fundingReconDetailUrl;
	protected static String createServerOrderUrl;
	protected static Document verifyDoc;
	protected static String[] ignorePayment;

	public AllInOneBase() {
		try {
			Document doc;
			/* when using web project */
//			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//			String configPath = URLDecoder.decode(classLoader.getResource("payment_conf.xml").getPath(), "UTF-8");
//			System.out.println("原本內建的 configPath: " + configPath);
//			doc = EcpayFunction.xmlParser(configPath);

			/* when using testing code */
//			String paymentConfPath = "./src/main/resources/payment_conf.xml";
//			doc = EcpayFunction.xmlParser(paymentConfPath);

			/* when use Spring Boot3  */
			 // 獲取 resource 目錄下的 XML 文件
			Resource resource = new ClassPathResource("payment_conf.xml");
			// 解析 XML 并返回 Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(resource.getInputStream());
			

			doc.getDocumentElement().normalize();
			// OperatingMode
			Element ele = (Element) doc.getElementsByTagName("OperatingMode").item(0);
			operatingMode = ele.getTextContent();
			// MercProfile
			ele = (Element) doc.getElementsByTagName("MercProfile").item(0);
			mercProfile = ele.getTextContent();
			// IsProjectContractor
			ele = (Element) doc.getElementsByTagName("IsProjectContractor").item(0);
			isProjectContractor = ele.getTextContent();
			// MID, HashKey, HashIV, PlatformID
			NodeList nodeList = doc.getElementsByTagName("MInfo");
			for (int i = 0; i < nodeList.getLength(); i++) {
				ele = (Element) nodeList.item(i);
				if (ele.getAttribute("name").equalsIgnoreCase(mercProfile)) {
					MerchantID = ele.getElementsByTagName("MerchantID").item(0).getTextContent();
					HashKey = ele.getElementsByTagName("HashKey").item(0).getTextContent();
					HashIV = ele.getElementsByTagName("HashIV").item(0).getTextContent();
					PlatformID = isProjectContractor.equalsIgnoreCase("N") ? "" : MerchantID;
				}
			}
			// IgnorePayment
			ele = (Element) doc.getElementsByTagName("IgnorePayment").item(0);
			nodeList = ele.getElementsByTagName("Method");
			ignorePayment = new String[nodeList.getLength()];
			for (int i = 0; i < nodeList.getLength(); i++) {
				ignorePayment[i] = nodeList.item(i).getTextContent();
			}
			if (HashKey == null) {
				throw new EcpayException(ErrorMessage.MInfo_NOT_SETTING);
			}
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}
}
