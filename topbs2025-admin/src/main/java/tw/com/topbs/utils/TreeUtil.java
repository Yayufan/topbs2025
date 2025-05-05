package tw.com.topbs.utils;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TreeUtil {
	/**
	 * 使用前此工具類前,需調用的實體類務必要實現TreeNode接口
	 * @param <T>   實體類List的類型
	 * @param <ID>  如果是用mybatis plus雪花ID,通常是Long類型
	 * @param items 實體類List
	 * @return
	 */
	public static <T extends TreeNode<T, ID>, ID> List<T> buildTree(List<T> items) {
		Map<ID, T> itemMap = Maps.newHashMap();
		List<T> roots = Lists.newArrayList();

		// 第一次遍歷：建立ID到項目的映射
		for (T item : items) {
			itemMap.put(item.getId(), item);
		}

		// 第二次遍歷：構建樹形結構
		for (T item : items) {
			ID parentId = item.getParentId();
			//如果parentId為null或者 itemMap不包含parentId這個key
			if (parentId == null || !itemMap.containsKey(parentId)) {
				//直接在根結點新增
				roots.add(item);
			} else {
				//parentId有值,所以找到父級結構添增進去
				T parent = itemMap.get(parentId);
				parent.addChild(item);
			}
		}

		return roots;
	}

}
