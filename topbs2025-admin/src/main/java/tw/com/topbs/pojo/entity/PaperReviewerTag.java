package tw.com.topbs.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.esotericsoftware.kryo.serializers.FieldSerializer.NotNull;

import java.io.Serializable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * paper_reviewer表 和 tag表的關聯表
 * </p>
 *
 * @author Joey
 * @since 2025-01-23
 */
@Getter
@Setter
@TableName("paper_reviewer_tag")
@Schema(name = "PaperReviewerTag", description = "paper_reviewer表 和 tag表的關聯表")
@NoArgsConstructor
public class PaperReviewerTag implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 只初始化 paperReviewerId 和 tagId 的構造器
	 * 
	 * @param paperReviewerId
	 * @param tagId
	 */
    public PaperReviewerTag(Long paperReviewerId, Long tagId) {
        this.paperReviewerId = paperReviewerId;
        this.tagId = tagId;
    }
	
	@Schema(description = "關聯表唯一主鍵")
	@TableId("id")
	private Long id;

	@Schema(description = "審稿委員ID")
	@TableField("paper_reviewer_id")
	private Long paperReviewerId;

	@Schema(description = "標籤ID")
	@TableField("tag_id")
	private Long tagId;
}
