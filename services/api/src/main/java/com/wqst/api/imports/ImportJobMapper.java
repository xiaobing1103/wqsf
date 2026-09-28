package com.wqst.api.imports;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
@Mapper public interface ImportJobMapper extends BaseMapper<ImportJobEntity>{
    @Update("UPDATE import_job SET status='RUNNING',started_at=NOW(3) WHERE id=#{id} AND status='PENDING'") int claim(long id);
    @Update("UPDATE import_job SET status='PENDING',started_at=NULL WHERE status='RUNNING' AND completed_at IS NULL AND started_at<DATE_SUB(NOW(3),INTERVAL 60 MINUTE)") int resetStale();
}
