package tw.com.topbs.system.pojo.BO;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class RouteBO {

	/*路由ID*/
	private Long menuId;
	
	/*父路由ID*/
	private Long parentId;
	
	/*路由類型 功能表類型（M目錄 C功能表 F按鈕）*/
	private String menuType;
	
	/*顯示順序*/
	private int orderNum;
	
	/*路由地址*/
	private String path;
	
	/*路由組件*/
	private String component;
	
	/*路由參數*/
	private String queryParams;
	
	/*路由圖標*/
	private String icon;
	
	/*路由是否為外連結, 0為是 1為否*/
	private String isFrame;
	
	/*路由是否為緩存路由, 0為是 1為否 */
	private String isCache;
	
	/*路由是否顯示 , 0為顯示 1為隱藏 */
	private String visible;
	
	/*路由是否啟用 , 0為啟用 1為停用 */
	private String status;
	
	/*該路由對象的子路由*/
	private List<RouteBO> children;
	
	/*創建子路由的function*/
    public void addChild(RouteBO child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }
	
	
	
}
