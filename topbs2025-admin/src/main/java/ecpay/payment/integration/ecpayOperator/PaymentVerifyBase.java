package ecpay.payment.integration.ecpayOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ecpay.payment.integration.errorMsg.ErrorMessage;
import ecpay.payment.integration.exception.EcpayException;

public class PaymentVerifyBase {
	protected String confPath = "/ecpay/payment/integration/config/EcpayPayment.xml";
	protected Document doc;

	public PaymentVerifyBase() {
		/* 原本的代碼 */
//		URL fileURL = this.getClass().getResource(confPath);
//		doc = EcpayFunction.xmlParser(fileURL.toString());
//		doc.getDocumentElement().normalize();

		/* 2025/02/20 Joey自行修改 */
		try {
			// 獲取 resource 目錄下的 XML 文件
			Resource resource = new ClassPathResource("EcpayPayment.xml");
			// 解析 XML 并返回 Document
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(resource.getInputStream());
			doc.getDocumentElement().normalize();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void requireCheck(String FieldName, String objValue, String require) {
		if (require.equals("1") && objValue.isEmpty())
			throw new EcpayException(FieldName + "為必填");
	}

	protected void valueCheck(String type, String objValue, Element ele) {
		if (objValue.isEmpty()) {
			return;
		}
		if (type.equals("String")) {
			if (ele.getElementsByTagName("pattern") != null) {
				Pattern r = Pattern.compile(ele.getElementsByTagName("pattern").item(0).getTextContent().toString());
				Matcher m = r.matcher(objValue);
				if (!m.find())
					throw new EcpayException(ele.getAttribute("name") + ErrorMessage.COLUMN_RULE_ERROR);
			}
		} else if (type.equals("Opt")) {
			List<String> opt = new ArrayList<String>();
			NodeList n = ele.getElementsByTagName("option");
			for (int i = 0; i < n.getLength(); i++) {
				opt.add(n.item(i).getTextContent().toString());
			}
			if (!opt.contains(objValue))
				throw new EcpayException(ele.getAttribute("name") + ErrorMessage.COLUMN_RULE_ERROR);
		} else if (type.equals("Int")) {
			String mode = ele.getElementsByTagName("mode").item(0).getTextContent();
			String minimum = ele.getElementsByTagName("minimal").item(0).getTextContent();
			String maximum = ele.getElementsByTagName("maximum").item(0).getTextContent();
			if (objValue.isEmpty())
				throw new EcpayException(ele.getAttribute("name") + ErrorMessage.CANNOT_BE_EMPTY);
			int value = Integer.valueOf(objValue);
			if (mode.equals("GE") && value < Integer.valueOf(minimum))
				throw new EcpayException(ele.getAttribute("name") + "不能小於" + minimum);
			else if (mode.equals("LE") && value > Integer.valueOf(maximum))
				throw new EcpayException(ele.getAttribute("name") + "不能大於" + maximum);
			else if (mode.equals("BETWEEN") && value < Integer.valueOf(minimum) && value > Integer.valueOf(maximum))
				throw new EcpayException(ele.getAttribute("name") + "必須介於" + minimum + "和" + maximum + "之間");
			else if (mode.equals("EXCLUDE") && value >= Integer.valueOf(minimum) && value <= Integer.valueOf(maximum))
				throw new EcpayException(ele.getAttribute("name") + "必須小於" + minimum + "或大於" + maximum);
		} else if (type.equals("DepOpt")) {
			// TODO
		}
	}
}
