package tw.com.topbs.exception;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import tw.com.topbs.system.exception.SysChunkFileException;
import tw.com.topbs.utils.R;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
	// 全局異常處理,當這個異常沒有被特別處理時,一定會走到全局異常,因為Exception範圍最大
	// 執行的方法,如果返回data沒有特別的值,統一泛型用Map即可

	/**
	 * 如果是遇到Hibernate查詢實體類未被查詢到時,直接返回
	 * 
	 * @param ex
	 * @return
	 */
	//	@ResponseBody
	//	@ExceptionHandler(EntityNotFoundException.class)
	//	public R<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {
	//		return R.ok();
	//	}

	/**
	 * 跟自定義表單相關的業務邏輯錯誤
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.CONFLICT)
	@ExceptionHandler(value = FormException.class)
	public R<Map<String, Object>> formException(FormException exception) {
		String message = exception.getMessage();
		return R.fail(409, message);
	}

	/**
	 * 處理排程任務 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = ScheduleEmailTaskException.class)
	public R<Map<String, Object>> scheduleEmailTaskException(ScheduleEmailTaskException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理審稿委員公文檔案 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = PaperReviewerFileException.class)
	public R<Map<String, Object>> paperReviewerFileException(PaperReviewerFileException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-Excel匯入 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = ImportExcelException.class)
	public R<Map<String, Object>> importExcelException(ImportExcelException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-簽到/退紀錄 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = CheckinRecordException.class)
	public R<Map<String, Object>> checkinRecordException(CheckinRecordException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-Email 每日額度 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = EmailException.class)
	public R<Map<String, Object>> emailException(EmailException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-Redis Key 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = RedisKeyException.class)
	public R<Map<String, Object>> redisKeyException(RedisKeyException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-投稿摘要異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = PaperAbstractsException.class)
	public R<Map<String, Object>> paperAbstractsException(PaperAbstractsException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-找回密碼異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = ForgetPasswordException.class)
	public R<Map<String, Object>> forgetPasswordException(ForgetPasswordException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-訂單付款表單異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = OrderPaymentException.class)
	public R<Map<String, Object>> orderPaymentException(OrderPaymentException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-超過投稿時間異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = PaperClosedException.class)
	public R<Map<String, Object>> paperClosedException(PaperClosedException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-超過註冊時間異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = RegistrationClosedException.class)
	public R<Map<String, Object>> registrationClosedException(RegistrationClosedException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-信箱已被註冊過異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = RegisteredAlreadyExistsException.class)
	public R<Map<String, Object>> registeredAlreadyExistsException(RegisteredAlreadyExistsException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-註冊資訊異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = RegistrationInfoException.class)
	public R<Map<String, Object>> registrationInfoException(RegistrationInfoException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-登入時帳號密碼錯誤異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = AccountPasswordWrongException.class)
	public R<Map<String, Object>> accountPasswordWrongException(AccountPasswordWrongException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-會員異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = MemberException.class)
	public R<Map<String, Object>> memberException(MemberException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理自定義-SysChunkFile 大檔案分片上傳相關的問題
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = SysChunkFileException.class)
	public R<Map<String, Object>> sysChunkFileException(SysChunkFileException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 處理環境設置 相關的異常
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = SettingException.class)
	public R<Map<String, Object>> settingException(SettingException exception) {
		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 超出Spring 設定單個檔案最大上傳大小, 如需調整請去 application.yml ,
	 * spring.servlet.multipart.max-file-size <br>
	 * HTTP 狀態碼 413 請求體過大
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
	@ExceptionHandler(value = MaxUploadSizeExceededException.class)
	public R<Map<String, Object>> maxUploadSizeExceptionHandler(MaxUploadSizeExceededException exception) {
		exception.printStackTrace();
		log.error(exception.getMessage());
		return R.fail(413, exception.getMessage());
	}

	/**
	 * 呼叫端傳了「語意上不合法」的參數，但物件本身狀態是正常的<br>
	 * 通常用於Enum
	 * 
	 * @param ex
	 * @return
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(IllegalArgumentException.class)
	public R<?> handleIllegalArgument(IllegalArgumentException ex) {
		log.error("Illegal argument: {}", ex.getMessage());
		ex.printStackTrace();
		return R.fail(400, ex.getMessage());
	}

	/**
	 * Json 反序列化為 Java Bean 異常
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	public R<Map<String, Object>> jsonFormatExceptionHandler(HttpMessageNotReadableException exception) {
		exception.printStackTrace();
		log.error(exception.getMessage());
		return R.fail(400, "請求格式錯誤，請檢查內容");
	}

	/**
	 * 
	 * 參數校驗異常MethodArgumentNotValidException<br>
	 * 在Controller層 , 校驗DTO時發生
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public R<Map<String, Object>> argumentExceptionHandler(MethodArgumentNotValidException exception) {
		exception.printStackTrace();
		log.error(exception.getMessage());
		// 取得第一個錯誤訊息,但這樣返回給前端的錯誤信息太明確 , 先不使用
	    String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();

//	    return R.fail(400, message);
		return R.fail(400, "Parameter verification exception");
	}

	/**
	 * 
	 * 參數校驗異常ConstraintViolationException<br>
	 * 在Service層 或者 「非」Controller的其他處理層 , class中標註@Validated , 校驗DTO時發生
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(value = ConstraintViolationException.class)
	public R<Map<String, Object>> argumentValidExceptionHandler(ConstraintViolationException exception) {
		exception.printStackTrace();
		log.error(exception.getMessage());
		return R.fail(500, "Parameter verification exception ");
	}

	/**
	 * token校驗異常<br>
	 * HTTP 狀態碼 401 token 憑證較驗失敗 或是 憑證過期
	 * 
	 * @param nle
	 * @return
	 * @throws Exception
	 */
	@ExceptionHandler(NotLoginException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ResponseBody
	public R<Map<String, Object>> handlerNotLoginException(NotLoginException nle) throws Exception {

		// 打印堆栈，以供调试
		nle.printStackTrace();
		// 判断登入場景值，定制化異常信息
		String message = "";
		if (nle.getType().equals(NotLoginException.NOT_TOKEN)) {
			message = "Failed to read token, please log in again";
		} else if (nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
			message = "Token is invalid, please log in again";
		} else if (nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
			// 使用Redis的話並不會出現這個狀態,只有使用JWT時才會有,因為當token過期時會直接從redis消失
			message = "The token has expired, please log in again";
		} else if (nle.getType().equals(NotLoginException.BE_REPLACED)) {
			message = "token has been canceled and offline";
		} else if (nle.getType().equals(NotLoginException.KICK_OUT)) {
			message = "token has been kicked offline, please try to log in again after 24 hours";
		} else if (nle.getType().equals(NotLoginException.TOKEN_FREEZE)) {
			// 這邊通常只適用active-timeout 有設置時間, 且超過可允許的待機時間時,才會報token凍結異常
			message = "token has been frozen";
		} else if (nle.getType().equals(NotLoginException.NO_PREFIX)) {
			message = "The token was not submitted according to the specified prefix";
		} else {
			message = "Not logged in for the current session";
		}

		// 返回给前端
		return R.fail(nle.getCode(), message);
	}

	/**
	 * token 較驗通過,但無此角色權限異常<br>
	 * HTTP 狀態碼 403 權限不足
	 * 
	 * @param nre
	 * @return
	 * @throws Exception
	 */
	@ExceptionHandler(NotRoleException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN) // 
	@ResponseBody
	public R<Map<String, Object>> handlerNotRoleException(NotRoleException nre) throws Exception {
		// 打印堆栈，以供调试
		nre.printStackTrace();
		log.error(nre.getMessage());

		return R.fail(nre.getCode(), "Insufficient permissions to access this resource");

	}

	/**
	 * 
	 * @param npe
	 * @return
	 * @throws Exception
	 */
	@ExceptionHandler(NotPermissionException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	@ResponseBody
	public R<Map<String, Object>> handlerNotPermissionException(NotPermissionException npe) throws Exception {

		// 打印堆栈，以供调试
		npe.printStackTrace();
		log.error(npe.getMessage());

		return R.fail(403,
				"Insufficient permissions to access this resource , Missing permissions: " + npe.getPermission());

	}

	/**
	 * HTTP 狀態碼為 401 , 因為token失效 或 token
	 * 
	 * @param e
	 * @return
	 */
	@ExceptionHandler(SaTokenException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ResponseBody
	public R<Map<String, Object>> error(SaTokenException e) {
		e.printStackTrace();
		// 根据不同异常细分状态码返回不同的提示
		// 前端沒回傳token的時候
		if (e.getCode() == 11001 || e.getCode() == 11011) {
			return R.fail(401, "Failed to read valid Token, please log in again");
		}

		// 最常見應該是這個
		if (e.getCode() == 11012) {
			return R.fail(401, "Token is invalid, please log in again");
		}
		// 前端回傳的token已經過期的時候,因為放在localStorage所以有機會過期
		if (e.getCode() == 11013) {
			return R.fail(401, "Token has expired, please log in again");
		}

		// 更多 code 码判断 ...

		// 默认的提示
		return R.fail(e.getCode(), e.getMessage());
	}

	/**
	 * 通用異常處理<br>
	 * HTTP 狀態碼 server端異常 500
	 * 
	 * @param e
	 * @return
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public R<Map<String, Object>> error(Exception e) {
		log.error(e.getMessage());
		e.printStackTrace();
		return R.fail("Function abnormal, please try again later...");
	}

}
