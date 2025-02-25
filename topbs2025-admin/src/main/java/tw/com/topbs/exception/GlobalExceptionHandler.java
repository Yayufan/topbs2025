package tw.com.topbs.exception;

import java.util.Map;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import jakarta.validation.ConstraintViolationException;
import tw.com.topbs.utils.R;

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
	 * 超出Spring 設定單個檔案最大上傳大小, 如需調整請去 application.yml ,
	 * spring.servlet.multipart.max-file-size
	 * 
	 * @param exception
	 * @return
	 */
	@ResponseBody
	@ExceptionHandler(value = MaxUploadSizeExceededException.class)
	public R<Map<String, Object>> maxUploadSizeExceptionHandler(MaxUploadSizeExceededException exception) {

		String message = exception.getMessage();
		return R.fail(500, message);
	}

	/**
	 * 日期Json轉換異常
	 */
	@ResponseBody
	@ExceptionHandler(value = HttpMessageNotReadableException.class)
	public R<Map<String, Object>> jsonFormatExceptionHandler(HttpMessageNotReadableException exception) {

		Throwable cause = exception.getCause();
		String message;

		if (cause instanceof InvalidFormatException) {
			message = " Date format error, please make sure your date format is 'yyyy-MM-dd HH:mm:ss ";
		} else {
			message = " Request format error ";
		}

		return R.fail(500, message);
	}

	/**
	 * 参数校验异常MethodArgumentNotValidException
	 */
	@ResponseBody
	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public R<Map<String, Object>> argumentExceptionHandler(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldError().getDefaultMessage();
		return R.fail(500, "Parameter verification exception : " + message);
	}

	/**
	 * 參數校驗異常ConstraintViolationException
	 */
	@ResponseBody
	@ExceptionHandler(value = ConstraintViolationException.class)
	public R<Map<String, Object>> argumentValidExceptionHandler(ConstraintViolationException exception) {
		String message = exception.getLocalizedMessage();
		return R.fail(500, "Parameter verification exception : " + message);
	}

	/**
	 * token校驗異常
	 */
	@ExceptionHandler(NotLoginException.class)
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
		return R.fail(401, message);
	}

	// saToken異常處理
	@ExceptionHandler(SaTokenException.class)
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

	// 特定異常處理,處理數據為0異常
	@ExceptionHandler(ArithmeticException.class)
	@ResponseBody
	public R<Map<String, Object>> error(ArithmeticException e) {
		e.printStackTrace();
		return R.fail("Calculation exception");
	}

	/**
	 * 通用異常處理
	 */
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public R<Map<String, Object>> error(Exception e) {
		e.printStackTrace();
		return R.fail("Function abnormal, please try again later...");
	}

}
