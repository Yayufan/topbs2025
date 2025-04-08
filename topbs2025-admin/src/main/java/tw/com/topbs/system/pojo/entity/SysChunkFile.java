package tw.com.topbs.system.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 系統通用，大檔案分片上傳，5MB以上就可處理，這邊僅記錄這個大檔案的上傳進度 和 狀況，
 * 合併後的檔案在minio，真實的分片區塊，會放在臨時資料夾，儲存資料會在redis
 * </p>
 *
 * @author Joey
 * @since 2025-04-07
 */
@Getter
@Setter
@TableName("sys_chunk_file")
@Schema(name = "SysChunkFile", description = "系統通用，大檔案分片上傳，5MB以上就可處理，這邊僅記錄這個大檔案的上傳進度 和 狀況，合併後的檔案在minio，真實的分片區塊，會放在臨時資料夾，儲存資料會在redis")
public class SysChunkFile {

	@Schema(description = "主鍵ID")
	@TableId("sys_chunk_file_id")
	private Long sysChunkFileId;

	@Schema(description = "文件唯一標示，可由前端或後端產生都可以，一致就好")
	@TableField("file_id")
	private String fileId;

	@Schema(description = "文件原始名稱")
	@TableField("file_name")
	private String fileName;

	@Schema(description = "文件儲存路徑")
	@TableField("file_path")
	private String filePath;

	@Schema(description = "文件類型")
	@TableField("file_type")
	private String fileType;

	@Schema(description = "文件大小(字節 byte)")
	@TableField("file_size")
	private Long fileSize;

	@Schema(description = "文件sha256值，用於秒傳")
	@TableField("file_sha256")
	private String fileSha256;

	@Schema(description = "已上傳的分片總數")
	@TableField("total_chunks")
	private Integer totalChunks;

	@Schema(description = "已上傳的分片數")
	@TableField("uploaded_chunks")
	private Integer uploadedChunks;

	@Schema(description = "上傳狀態(0-未完成，1-已完成)")
	@TableField("status")
	private Integer status;

	@Schema(description = "預設為0代表存在, 更改為1代表刪除")
	@TableField("is_deleted")
	@TableLogic
	private Integer isDeleted;

	@Schema(description = "創建者")
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private String createBy;

	@Schema(description = "創建時間")
	@TableField(value = "create_date", fill = FieldFill.INSERT)
	private LocalDateTime createDate;

	@Schema(description = "更新者")
	@TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

	@Schema(description = "更新時間")
	@TableField(value = "update_date", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateDate;

}
