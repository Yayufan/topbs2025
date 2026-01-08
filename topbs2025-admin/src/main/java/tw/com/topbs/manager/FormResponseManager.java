package tw.com.topbs.manager;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.excel.EasyExcel;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.convert.FormConvert;
import tw.com.topbs.convert.ResponseAnswerConvert;
import tw.com.topbs.enums.CommonStatusEnum;
import tw.com.topbs.enums.FormStatusEnum;
import tw.com.topbs.exception.FormException;
import tw.com.topbs.pojo.BO.ResponseAnswerMatrixBO;
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
		Form form = formService.searchForm(formResponse.getFormId());

		// 3.轉換資料
		FormVO formVO = formConvert.entityToVO(form);

		// 4.根據 formId 查詢表單 及其 欄位
		List<FormFieldVO> formFieldVOList = formFieldService.searchFormStructureByForm(form.getFormId());

		// 5.根據 responseId 查詢已填寫的結果映射
		Map<Long, ResponseAnswer> responseAnswerMap = responseAnswerService.searchAnswerMapByFieldId(responseId);

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
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	private boolean isNowWithin(LocalDateTime startTime, LocalDateTime endTime) {
		// 無時間限制
		if (startTime == null && endTime == null) {
			return true;
		}

		LocalDateTime now = LocalDateTime.now();

		if (startTime != null && now.isBefore(startTime)) {
			return false;
		}

		if (endTime != null && now.isAfter(endTime)) {
			return false;
		}

		return true;
	}

	/**
	 * 新增 表單回覆 及 回覆細項
	 * 
	 * @param formResponseDTO
	 */
	public void addFormResponse(AddFormResponseDTO formResponseDTO) {

		// 1.表單回覆進來,先查詢表單基本資訊
		Form form = formService.searchForm(formResponseDTO.getFormId());

		// 2.如果表單要求登入 , 但是memberId沒傳遞則代表不合規
		if (CommonStatusEnum.YES.getValue().equals(form.getRequireLogin()) && formResponseDTO.getMemberId() == null) {
			throw new IllegalStateException("此表單填寫需先登入");
		}

		// 3.判斷表單是否開放
		if (!FormStatusEnum.PUBLISHED.equals(form.getStatus())) {
			throw new FormException("表單不處於發佈狀態");
		}

		// 4.判斷表單是否處於可填寫日期中
		if (!this.isNowWithin(form.getStartTime(), form.getEndTime())) {
			throw new FormException("表單不處於填寫時間");
		}

		// 5.先輸入這筆表單回覆,拿到表單回覆ID
		FormResponse formResponse = formResponseService.submit(formResponseDTO);

		// 6.拿到回答結果,進行轉換,最終拿到詳細回覆結果
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

		// 7.批量插入
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
		responseAnswerService.removeByResponse(formResponseId);

		// 2.再刪除表單回覆本身
		formResponseService.removeById(formResponseId);

	}

	/**
	 * 下載表單回覆的Excel
	 * 
	 * @param response
	 * @param formId
	 * @throws IOException
	 */
	public void downloadFormResponseExcel(HttpServletResponse response, Long formId) throws IOException {

		// 1.獲取表單基本資訊
		Form form = formService.searchForm(formId);

		// 2.拿到這張表單所有的問題,並過濾掉不須輸出到excel的部分
		List<FormFieldVO> formFieldVOs = formFieldService.searchFormStructureByForm(formId);
		List<FormFieldVO> exportFields = formFieldVOs.stream().filter(f -> f.getFieldType().isExportable()).toList();

		// 3. 構建表頭：先加動態欄位，再加固定時間
		List<List<String>> head = new ArrayList<>();
		// 3-1. 加入問題標題
		exportFields.forEach(field -> head.add(Collections.singletonList(field.getLabel())));
		// 3-2. 最後加入固定表頭
		head.add(Collections.singletonList("填單時間"));
		head.add(Collections.singletonList("更新時間"));

		// 4.拿到此表單的所有回覆,並抽取他所有Id
		List<FormResponse> responseList = formResponseService.searchSubmissionsByForm(formId);
		List<Long> responseIds = responseList.stream().map(FormResponse::getFormResponseId).toList();

		// 5.透過Ids 獲取「答案矩陣 BO」
		ResponseAnswerMatrixBO matrixBO = responseAnswerService.searchResponseAnswerMatrixBO(responseIds);

		// 6. 構建內容數據
		List<List<Object>> dataRows = new ArrayList<>();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

		for (FormResponse res : responseList) {
			List<Object> rowData = new ArrayList<>();
			Long resId = res.getFormResponseId();

			// 6-1. 先加入動態問題的答案
			for (FormFieldVO field : exportFields) {
				rowData.add(matrixBO.getAnswer(resId, field.getFormFieldId()));
			}

			// 6-2. 最後再加入時間資訊 (與表頭順序保持一致)
			rowData.add(res.getCreateDate() != null ? res.getCreateDate().format(formatter) : "");
			rowData.add(res.getUpdateDate() != null ? res.getUpdateDate().format(formatter) : "");

			dataRows.add(rowData);
		}

		// 6. 設置 Http Header
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		String fileName = URLEncoder.encode("問卷表單_" + form.getTitle(), "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 7. 使用 EasyExcel 輸出
		EasyExcel.write(response.getOutputStream())
				.head(head) // 傳入動態表頭
				.automaticMergeHead(false) // <--- 加入這一行，禁止自動合併相同標題
				.sheet("回覆結果")
				.doWrite(dataRows); // 傳入構建好的二維數據
	}

}
