package tw.com.topbs.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tw.com.topbs.exception.ImportExcelException;
import tw.com.topbs.mapper.AttendeesHistoryMapper;
import tw.com.topbs.pojo.entity.AttendeesHistory;
import tw.com.topbs.pojo.excelPojo.AttendeesHistoryImportExcel;
import tw.com.topbs.service.AttendeesHistoryService;

/**
 * <p>
 * 往年與會者名單 服务实现类
 * </p>
 *
 * @author Joey
 * @since 2025-05-14
 */
@Service
@RequiredArgsConstructor
public class AttendeesHistoryServiceImpl extends ServiceImpl<AttendeesHistoryMapper, AttendeesHistory> implements AttendeesHistoryService {
	@Override
	public AttendeesHistory getAttendeesHistory(Long attendeesHistoryId) {
		AttendeesHistory attendeesHistory = baseMapper.selectById(attendeesHistoryId);
		return attendeesHistory;
	}

	@Override
	public List<AttendeesHistory> getAttendeesHistoryList() {
		List<AttendeesHistory> attendeesHistoryList = baseMapper.selectList(null);
		return attendeesHistoryList;
	}

	@Override
	public IPage<AttendeesHistory> getAttendeesHistoryPage(Page<AttendeesHistory> page) {
		Page<AttendeesHistory> attendeesHistoryPage = baseMapper.selectPage(page, null);
		return attendeesHistoryPage;
	}

	@Override
	public Boolean existsAttendeesHistory(Integer year, String idCard, String email) {
		LambdaQueryWrapper<AttendeesHistory> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AttendeesHistory::getYear, year);

		if (idCard != null && !idCard.isBlank()) {
			wrapper.eq(AttendeesHistory::getIdCard, idCard);
		} else {
			wrapper.eq(AttendeesHistory::getEmail, email);
		}

		// 有可能為null 有可能查詢有值
		AttendeesHistory result = baseMapper.selectOne(wrapper);

		System.out.println("result 的值為" + result);

		// 回傳 true：資料庫有符合條件的紀錄 (result 不為 null)
		// 回傳 false：資料庫無符合條件的紀錄 (result 為 null)
		return result != null;
	}

	@Override
	public void generateImportTemplate(HttpServletResponse response) throws IOException {

		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		// 这里URLEncoder.encode可以防止中文乱码 ， 和easyexcel没有关系
		String fileName = URLEncoder.encode("匯入模板", "UTF-8").replaceAll("\\+", "%20");
		response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

		// 模擬範例資料 (用1筆範例讓使用者看得懂)
		AttendeesHistoryImportExcel example = new AttendeesHistoryImportExcel();
		example.setYear(LocalDate.now().getYear() - 1);
		example.setIdCard("A123456789");
		example.setEmail("user@example.com");
		example.setName("王小明");

		EasyExcel.write(response.getOutputStream(), AttendeesHistoryImportExcel.class)
				.sheet("範例模板")
				.doWrite(List.of(example));

	}

	@Override
	@Transactional
	public void importAttendeesHistory(MultipartFile file) throws IOException {

		List<AttendeesHistoryImportExcel> dataList = EasyExcel.read(file.getInputStream())
				.head(AttendeesHistoryImportExcel.class)
				.sheet()
				.doReadSync();

		List<AttendeesHistory> entities = dataList.stream().map(row -> {

			// 確保年份和email都要存在
			if (row.getYear() == null || row.getEmail() == null) {
				// 丟出異常
				throw new ImportExcelException("年份 和 Email 為必填項目");
			}

			AttendeesHistory entity = new AttendeesHistory();
			entity.setYear(row.getYear()); // 年份補1月1日
			entity.setIdCard(row.getIdCard());
			entity.setEmail(row.getEmail());
			entity.setName(row.getName());
			return entity;
		}).toList();

		// 先收集所有匯入的 Email
		Set<String> emailSet = entities.stream().map(AttendeesHistory::getEmail).collect(Collectors.toSet());

		// 檢查是否有重複的 Email
		if (emailSet.size() != entities.size()) {
			throw new ImportExcelException("匯入資料中有重複的 Email ");
		}

		// 確認沒問題將資料新增進資料庫
		for (AttendeesHistory entity : entities) {
			baseMapper.insert(entity);
		}

	}

	@Override
	public void clearAllAttendeesHistory() {
		// 全數清空
		baseMapper.cleanAllData();
	}
}
