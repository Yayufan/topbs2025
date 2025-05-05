package tw.com.topbs.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import tw.com.topbs.system.pojo.entity.SysChunkFile;

/**
 * <p>
 * 系統通用，大檔案分片上傳，5MB以上就可處理，這邊僅記錄這個大檔案的上傳進度 和 狀況，
 * 合併後的檔案在minio，真實的分片區塊，會放在臨時資料夾，儲存資料會在redis Mapper 接口
 * </p>
 *
 * @author Joey
 * @since 2025-04-07
 */
public interface SysChunkFileMapper extends BaseMapper<SysChunkFile> {

}
