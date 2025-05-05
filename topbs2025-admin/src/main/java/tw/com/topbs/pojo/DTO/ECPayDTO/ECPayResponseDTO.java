package tw.com.topbs.pojo.DTO.ECPayDTO;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ECPayResponseDTO {

	@Schema(description = "特店編號")
	private String MerchantID;

	@Schema(description = "特店交易編號，訂單產生時傳送給綠界的特店交易編號")
	private String MerchantTradeNo;

	@Schema(description = "特店旗下店舖代號")
	private String StoreID;

	@Schema(description = "交易狀態，若回傳值為1時，為付款成功；其餘代碼皆為交易異常，請至廠商管理後台確認後再出貨。")
	private Integer RtnCode;

	@Schema(description = "交易訊息")
	private String RtnMsg;

	@Schema(description = "綠界的交易編號，請保存綠界的交易編號與特店交易編號[MerchantTradeNo]的關連。")
	private String TradeNo;

	@Schema(description = "交易金額")
	private Integer TradeAmt;

	@Schema(description = "付款時間，格式為yyyy/MM/dd HH:mm:ss")
	private String PaymentDate;

	@Schema(description = "特店選擇的付款方式")
	private String PaymentType;

	@Schema(description = "交易手續費金額")
	private Integer PaymentTypeChargeFee;

	@Schema(description = "訂單成立時間，格式為yyyy/MM/dd HH:mm:ss")
	private String TradeDate;

	@Schema(description = "特約合作平台商代號，為專案合作的平台商使用。")
	private String PlatformID;

	@Schema(description = "是否為模擬付款，0：代表此交易非模擬付款；1：代表此交易為模擬付款，" + "RtnCode也為1。並非是由消費者實際真的付款，所以綠界也不會撥款給廠商，請勿對該筆交易做出貨等動作，"
			+ "以避免損失。")
	private Integer SimulatePaid;

	@Schema(description = "自訂名稱欄位1，提供合作廠商使用記錄用客製化使用欄位")
	private String CustomField1;

	@Schema(description = "自訂名稱欄位2，提供合作廠商使用記錄用客製化使用欄位")
	private String CustomField2;

	@Schema(description = "自訂名稱欄位3，提供合作廠商使用記錄用客製化使用欄位")
	private String CustomField3;

	@Schema(description = "自訂名稱欄位4，提供合作廠商使用記錄用客製化使用欄位")
	private String CustomField4;

	@Schema(description = "檢查碼，特店必須檢查檢查碼 [CheckMacValue] 來驗證，請參考附錄檢查碼機制。")
	private String CheckMacValue;
}
