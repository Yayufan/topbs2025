package tw.com.topbs;

import java.io.File;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;

/**
 * 這個生成測試類, 主要是後續開發中有新增table , 進行擴展使用的
 */
public class MybatisPlusGeneratedBusinessReplenish {

	// 設置配置屬性

	/**
	 * 作者名(自行替換)
	 */
	public static final String AUTHOR = "Joey";

	/**
	 * 生成的表名（多个表用英文逗号分隔，所有表输入 all,後續業務新增表,只要指定某個表生成就好）
	 */
	private static final String TABLES = "all";

	/**
	 * jdbc 路径(自行替換)
	 */
	private static final String URL = "jdbc:mariadb://192.168.56.1:3306/topbs2025_international_conferenct";

	/**
	 * 数据库账号(自行替換)
	 */
	private static final String USER_NAME = "root";

	/**
	 * 数据库密码(自行替換)
	 */
	private static final String PASS_WORD = "kamikazey0022";

	/**
	 * 项目所在目录(自行替換,在Windows使用,複製檔案路徑最快) 或者像現在這樣直接獲取路徑到當前模塊
	 */
	private static final String PROJECT_MODEL = System.getProperty("user.dir");

	/**
	 * 模块名(自行替換,通常就是子模塊名,因為創建子模塊會生成對應路徑)
	 */
	// private static final String MODEL = "demo_admin";

	/**
	 * 包名(自行替換,通常三層中取前兩層,例:com.example.demo,取前兩層)
	 */
	private static final String PARENT = "tw.com.topbs";

	/**
	 * 业务名(自行替換,通常三層中取最後一層,代表某個業務功能)
	 */
	private static final String BUSINESS = "admin";

	/**
	 * 代码主路径（一般不用更改）
	 */
	private static final String MAIN = "src\\main\\java";

	/**
	 * 一般mapperXml 路径（一般不用更改）
	 */
	public static final String MAPPER = "src\\main\\resources\\mapper";

	public static final String SYS_MAPPER = "src\\main\\resources\\system\\mapper";

	/**
	 * 父类公共字段（一般不用更改）
	 */
	public static final List<String> COMMON_COLUMNS = new ArrayList<>(
			Arrays.asList("id", "create_by", "create_name", "create_date", "update_by", "update_name", "update_date",
					"del_flag", "remarks", "remark1", "remark2", "remark3", "remark4", "remark5"));

	public static void main(String[] args) {

		// -------------------接下來是非system的表生成Java代碼--------------------------------------------

		// 表結構對應Java包結構的建立
		FastAutoGenerator.create(URL, USER_NAME, PASS_WORD)
				// 全域配置
				.globalConfig(builder -> {
					builder.author(AUTHOR) // 设置作者
							// .disableOpenDir() //禁止打開輸出目錄,其實開不開沒差
							.enableSpringdoc() // 开启 springDoc 模式,線上API文檔
							// 這邊要設置自己目前的工程位置所在
							.commentDate("yyyy-MM-dd") // 注释日期
							.dateType(DateType.TIME_PACK)// 定义生成的实体类中日期类型 DateType.ONLY_DATE 默认值: DateType.TIME_PACK
							.outputDir(PROJECT_MODEL + File.separator + MAIN); // 指定输出目录
				})
				// 數據源配置,這邊默認應該就可以了
				.dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
					int typeCode = metaInfo.getJdbcType().TYPE_CODE;
					if (typeCode == Types.SMALLINT) {
						// 自定义类型转换
						return DbColumnType.INTEGER;
					}
					return typeRegistry.getColumnType(metaInfo);

				}))
				// 包配置
				.packageConfig(builder -> {
					builder.parent(PARENT) // 设置父包名
							.moduleName("") // 设置父包模块(業務)名,看情況設置,因為一設置,Controller路徑就多一層父包模块(業務)名
							.entity("pojo.entity") // 重新設置實體類包名
							// 設置xml輸出位置
							.pathInfo(
									Collections.singletonMap(OutputFile.xml, PROJECT_MODEL + File.separator + MAPPER)); // 设置mapperXml生成路径
				})
				// 策略配置
				.strategyConfig(builder -> {
					// 设置需要生成的表名,排除system類的table
					builder.addInclude("invited_speaker").addTablePrefix() // 设置过滤表前缀, sys_user設置成 user表,一般不用寫

							.controllerBuilder().enableRestStyle().enableHyphenStyle().enableFileOverride() // controller層
																											// 原檔案覆蓋
							.mapperBuilder().enableFileOverride() // mapper層 原檔案覆蓋
							// 將默認的IService的I去掉
							.serviceBuilder().formatServiceFileName("%sService").enableFileOverride() // service層 原檔案覆蓋

							.entityBuilder()
							// 移除Boolean 類會自動添加is的問題
							.enableRemoveIsPrefix()
							// 开启生成实体时生成字段注解
							.enableTableFieldAnnotation()
							// 開啟Lombok
							.enableLombok()
							// 逻辑删除字段名(数据库)
							.logicDeleteColumnName("is_deleted")
							// 逻辑删除属性名(实体)
							.logicDeletePropertyName("deleted")
							// 覆蓋現有文件
							.enableFileOverride() // entity層 原檔案覆蓋
							// 自動填充屬性值,create_time 在新增數據時填充
							.addTableFills(new Column("create_date", FieldFill.INSERT))
							// 自動填充屬性值,update_time 在更新數據時填充
							.addTableFills(new Column("update_date", FieldFill.UPDATE));

					// .build();

				}).templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
				.execute();

	}

}
