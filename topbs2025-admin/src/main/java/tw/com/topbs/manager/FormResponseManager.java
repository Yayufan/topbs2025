package tw.com.topbs.manager;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormConvert;
import tw.com.topbs.convert.ResponseAnswerConvert;
import tw.com.topbs.enums.CommonStatusEnum;
import tw.com.topbs.pojo.DTO.addEntityDTO.AddFormResponseDTO;
import tw.com.topbs.pojo.DTO.putEntityDTO.PutResponseAnswerDTO;
import tw.com.topbs.pojo.VO.FormFieldVO;
import tw.com.topbs.pojo.VO.FormVO;
import tw.com.topbs.pojo.entity.Form;
import tw.com.topbs.pojo.entity.FormResponse;
import tw.com.topbs.pojo.entity.ResponseAnswer;
import tw.com.topbs.service.FormFieldService;
import tw.com.topbs.service.FormResponseService;
import tw.com.topbs.service.FormService;
import tw.com.topbs.service.ResponseAnswerService;

@Component
@RequiredArgsConstructor
public class FormResponseManager {

	private final FormConvert formConvert;
	private final ResponseAnswerConvert responseAnswerConvert;

	private final FormService formService;
	private final FormFieldService formFieldService;
	private final FormResponseService formResponseService;
	private final ResponseAnswerService responseAnswerService;

	/**
	 * 獲取 可編輯的 表單對象
	 * 
	 * @param responseId
	 * @return
	 */
	public FormVO getEditableForm(Long responseId) {

		// 1.查詢此次表單回覆
		FormResponse formResponse = formResponseService.getById(responseId);

		// 2.查詢要填寫的表單
		Form form = formService.getForm(formResponse.getFormId());

		// 3.轉換資料
		FormVO formVO = formConvert.entityToVO(form);

		// 4.根據 formId 查詢表單 及其 欄位
		List<FormFieldVO> formFieldVOList = formFieldService.getFormFieldsByFormId(form.getFormId());

		// 5.根據 responseId 查詢已填寫的結果映射
		Map<Long, ResponseAnswer> responseAnswerMap = responseAnswerService.getAnswersKeyedByFieldId(responseId);

		// 6.遍歷formFieldVO , 把PutResponseAnswerDTO組裝進answer中
		List<FormFieldVO> processedFormFieldVOList = formFieldVOList.stream().map(vo -> {
			ResponseAnswer responseAnswer = responseAnswerMap.get(vo.getFormFieldId());
			PutResponseAnswerDTO answer = responseAnswerConvert.entityToPutDTO(responseAnswer);
			vo.setAnswer(answer);

			return vo;
		}).toList();

		// 7.VO填充欄位
		formVO.setFormFields(processedFormFieldVOList);

		return formVO;

	}

	/**
	 * 新增 表單回覆 及 回覆細項
	 * 
	 * @param formResponseDTO
	 */
	public void addFormResponse(AddFormResponseDTO formResponseDTO) {

		// 1.表單回覆進來,先查詢表單基本資訊
		Form form = formService.getForm(formResponseDTO.getFormId());

		// 2.如果表單要求登入 , 但是memberId沒傳遞則代表不合規
		if (CommonStatusEnum.YES.getValue().equals(form.getRequireLogin()) && formResponseDTO.getMemberId() == null) {
			throw new IllegalStateException("此表單填寫需先登入");
		}

		// 3.先輸入這筆表單回覆,拿到表單回覆ID
		FormResponse formResponse = formResponseService.addFormResponse(formResponseDTO);

		// 4.拿到回答結果,進行轉換,最終拿到詳細回覆結果
		List<ResponseAnswer> responseAnswerList = formResponseDTO.getResponseAnswerList()
				.stream()
				.map(responseAnswerDTO -> {

					// 轉換
					ResponseAnswer responseAnswer = responseAnswerConvert.addDTOToEntity(responseAnswerDTO);
					// 塞入本次表單回覆ID
					responseAnswer.setFormResponseId(formResponse.getFormResponseId());

					return responseAnswer;

				})
				.toList();

		// 5.批量插入
		responseAnswerService.saveBatch(responseAnswerList);

	}

	/**
	 * 更新表單回覆
	 * 
	 * @param putResponseAnswerDTOList
	 */
	public void updateFormResponse(List<PutResponseAnswerDTO> putResponseAnswerDTOList) {

		// 1.轉換
		List<ResponseAnswer> responseAnswerList = putResponseAnswerDTOList.stream().map(putResponseAnswerDTO -> {
			ResponseAnswer responseAnswer = responseAnswerConvert.putDTOToEntity(putResponseAnswerDTO);
			return responseAnswer;
		}).toList();

		// 2.批量更新
		responseAnswerService.updateBatchById(responseAnswerList);

	}

	/**
	 * 刪除表單回覆
	 * 
	 * @param formResponseId
	 */
	public void deleteFormResponse(Long formResponseId) {

		// 1.先刪除表單內所有的回覆細項
		responseAnswerService.deleteAnswerByResponseId(formResponseId);

		// 2.再刪除表單回覆本身
		formResponseService.deleteFormResponse(formResponseId);

	}

}
