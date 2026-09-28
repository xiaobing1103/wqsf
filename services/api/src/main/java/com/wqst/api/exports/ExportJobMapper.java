package com.wqst.api.exports;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
@Mapper public interface ExportJobMapper extends BaseMapper<ExportJobEntity>{
    @Update("UPDATE export_job SET status='RUNNING',started_at=NOW(3) WHERE id=#{id} AND status='PENDING'") int claim(long id);
    @Update("UPDATE export_job SET status='PENDING',started_at=NULL WHERE status='RUNNING' AND completed_at IS NULL AND started_at<DATE_SUB(NOW(3),INTERVAL 60 MINUTE)") int resetStale();
    @Update("UPDATE export_job SET download_count=download_count+1 WHERE id=#{id}") int incrementDownload(long id);
}
