package tw.com.topbs.pojo.DTO.ECPayDTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ECPayResponseDTO {

	@Schema(description = "特店編號")
	@JsonProperty("MerchantID")
	private String merchantId;

	@Schema(description = "特店交易編號，訂單產生時傳送給綠界的特店交易編號")
	@JsonProperty("MerchantTradeNo")
	private String merchantTradeNumber;

	@Schema(description = "特店旗下店舖代號")
	@JsonProperty("StoreID")
	private String storeId;

	@Schema(description = "交易狀態，若回傳值為1時，為付款成功；其餘代碼皆為交易異常，請至廠商管理後台確認後再出貨。")
	@JsonProperty("RtnCode")
	private Integer rtnCode;

	@Schema(description = "交易訊息")
	@JsonProperty("RtnMsg")
	private String rtnMsg;

	@Schema(description = "綠界的交易編號，請保存綠界的交易編號與特店交易編號[MerchantTradeNo]的關連。")
	@JsonProperty("TradeNo")
	private String tradeNumber;

	@Schema(description = "交易金額")
	@JsonProperty("TradeAmt")
	private Integer tradeAmt;

	@Schema(description = "付款時間，格式為yyyy/MM/dd HH:mm:ss")
	@JsonProperty("PaymentDate")
	private String paymentDate;

	@Schema(description = "特店選擇的付款方式")
	@JsonProperty("PaymentType")
	private String paymentType;

	@Schema(description = "交易手續費金額")
	@JsonProperty("PaymentTypeChargeFee")
	private Integer paymentTypeChargeFee;

	@Schema(description = "訂單成立時間，格式為yyyy/MM/dd HH:mm:ss")
	@JsonProperty("TradeDate")
	private String tradeDate;

	@Schema(description = "特約合作平台商代號，為專案合作的平台商使用。")
	@JsonProperty("PlatformID")
	private String platformId;

	@Schema(description = "是否為模擬付款，0：代表此交易非模擬付款；1：代表此交易為模擬付款，" + "RtnCode也為1。並非是由消費者實際真的付款，所以綠界也不會撥款給廠商，請勿對該筆交易做出貨等動作，"
			+ "以避免損失。")
	@JsonProperty("SimulatePaid")
	private Integer simulatePaid;

	@Schema(description = "自訂名稱欄位1，提供合作廠商使用記錄用客製化使用欄位")
	@JsonProperty("CustomField1")
	private String customField1;

	@Schema(description = "自訂名稱欄位2，提供合作廠商使用記錄用客製化使用欄位")
	@JsonProperty("CustomField2")
	private String customField2;

	@Schema(description = "自訂名稱欄位3，提供合作廠商使用記錄用客製化使用欄位")
	@JsonProperty("CustomField3")
	private String customField3;

	@Schema(description = "自訂名稱欄位4，提供合作廠商使用記錄用客製化使用欄位")
	@JsonProperty("CustomField4")
	private String customField4;

	@Schema(description = "檢查碼，特店必須檢查檢查碼 [CheckMacValue] 來驗證，請參考附錄檢查碼機制。")
	@JsonProperty("CheckMacValue")
	private String checkMacValue;
}
