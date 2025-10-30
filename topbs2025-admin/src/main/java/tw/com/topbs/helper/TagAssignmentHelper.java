package tw.com.topbs.helper;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tw.com.topbs.pojo.entity.Tag;

@Component
public class TagAssignmentHelper {

	@Value("${project.group-size}")
	private int GROUP_SIZE;
	
    /**
     * 群組化標籤的命名後綴
     */
    private static final String GROUP_TAG_SUFFIX = "-group-";

	/**
	 * 標籤分配
	 * 
	 * @param entityId         實體ID
	 * @param groupIndexGetter 群組index獲取器
	 * @param tagResolver      tag解析器(獲取或產生Tag),實際怎麼分配由調用者指定
	 * @param tagAssociator    tag分配器(新增entity和tag關聯),實際怎麼分配由調用者指定
	 */
	public void assignTag(Long entityId, Function<Integer, Integer> groupIndexGetter,
			Function<Integer, Tag> tagResolver, BiConsumer<Long, Long> tagAssociator) {

		int groupIndex = groupIndexGetter.apply(GROUP_SIZE);
		Tag tag = tagResolver.apply(groupIndex);
		tagAssociator.accept(entityId, tag.getTagId());
	}


	/**
	 * 簡化版標籤分配 (已知 groupIndex)
	 * 
	 * @param entityId      實體ID
	 * @param groupIndex    群組index
	 * @param tagResolver   tag解析器(獲取或產生Tag),實際怎麼分配由調用者指定
	 * @param tagAssociator tag分配器(新增entity和tag關聯),實際怎麼分配由調用者指定
	 */
	public void assignTagWithIndex(Long entityId, int groupIndex, Function<Integer, Tag> tagResolver,
			BiConsumer<Long, Long> tagAssociator) {

		Tag tag = tagResolver.apply(groupIndex);
		tagAssociator.accept(entityId, tag.getTagId());
	}
	
	 /**
	 * 移除群組化標籤(帶有-group-的標籤)
     * 透過 pattern 批量移除標籤<br>
     * 
     * @param entityId       實體ID
     * @param tagType        標籤類型
     * @param tagNamePattern 標籤名稱前綴(不含 "-group-")
     * @param tagIdsFetcher  透過 pattern 查詢 tagIds 的邏輯
     * @param tagRemover     批量移除標籤關聯的邏輯
     */
    public void removeGroupTagsByPattern(
            Long entityId,
            String tagType,
            String tagNamePattern,
            BiFunction<String, String, Set<Long>> tagIdsFetcher,
            BiConsumer<Long, Set<Long>> tagRemover) {
        
        // 在 Helper 內部組合完整的 pattern
        String fullPattern = tagNamePattern + GROUP_TAG_SUFFIX;
        Set<Long> tagIds = tagIdsFetcher.apply(tagType, fullPattern);
        tagRemover.accept(entityId, tagIds);
    }

}
