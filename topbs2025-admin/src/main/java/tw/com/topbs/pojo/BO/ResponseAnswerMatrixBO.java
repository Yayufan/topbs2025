package tw.com.topbs.pojo.BO;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseAnswerMatrixBO {
	
	// 內部封裝複雜的 Map
    private final Map<Long, Map<Long, String>> matrix;

    /**
     * 讓調用者不需要通靈，直接透過方法名獲取數據
     */
    public String getAnswer(Long responseId, Long fieldId) {
        if (matrix == null || !matrix.containsKey(responseId)) return "";
        return matrix.get(responseId).getOrDefault(fieldId, "");
    }
}
