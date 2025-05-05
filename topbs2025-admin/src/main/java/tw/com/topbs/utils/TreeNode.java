package tw.com.topbs.utils;

import java.util.ArrayList;
import java.util.List;

public interface TreeNode<T extends TreeNode<T, ID>, ID> {
    ID getId();
    ID getParentId();
    List<T> getChildren();
    // 內部使用的設置子節點的方法
    void setChildrenInternal(List<T> children);
    
    // 添加一個默認方法來設置子節點
    default void addChild(T child) {
        List<T> children = getChildren();
        if (children == null) {
            children = new ArrayList<>();
            setChildrenInternal(children);
        }
        children.add(child);
    }
    
  
}
