package tw.com.topbs.context;

import java.util.Map;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.topbs.config.ProjectConfig;
import tw.com.topbs.enums.ProjectModeEnum;
import tw.com.topbs.strategy.project.ProjectModeStrategy;


/**
 * 僅由 Manager層調用,請勿在Service中使用,造成循環依賴
 * 
 */
@Component
@RequiredArgsConstructor
public class ProjectModeContext {
	private final ProjectConfig projectConfig;
	private final Map<String, ProjectModeStrategy> strategyMap;

	/**
	 * 取得目前專案模式 enum
	 */
	public ProjectModeEnum getCurrentMode() {
		return projectConfig.getMode();
	}

	/**
	 * 根據當前 mode 回傳對應策略
	 */
	public ProjectModeStrategy getStrategy() {
		ProjectModeEnum mode = this.getCurrentMode();
		ProjectModeStrategy strategy = strategyMap.get(mode.getRegistrationStrategyKey());
		if (strategy == null) {
			throw new IllegalStateException("No strategy found for mode: " + mode);
		}
		return strategy;
	}
}
