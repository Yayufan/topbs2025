package tw.com.topbs.strategy.project;

import java.math.BigDecimal;

import tw.com.topbs.pojo.entity.Member;

public interface ProjectModeStrategy {
	
	/**
	 * 處理 XX模式 的個人註冊<br>
	 * 依模式決定是否要擋、或額外行為
	 * 
	 * @param member
	 */
	void handleRegistration(Member member);
	
	
	/**
	 * 處理 XX模式 的團體註冊<br>
	 * 依模式決定是否要擋、或額外行為
	 * 
	 * @param member
	 * @param isMaster
	 * @param totalFee
	 */
	void handleGroupRegistration(Member member, boolean isMaster, BigDecimal totalFee);
	
	
    /**
     * 處理 XX模式 投稿流程<br>
     * 依模式決定是否要擋、或額外行為
     * 
     */
    void handlePaperSubmission(String memberId, Object paperRequest);
}
