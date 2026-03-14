package com.company.docs.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.docs.storage.model.entity.JobDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * jobs 表访问接口。
 */
@Mapper
public interface JobMapper extends BaseMapper<JobDO> {

    @Select("SELECT * FROM jobs WHERE library_id=#{libraryId} AND version_id=#{versionId} AND status IN ('QUEUED', 'RUNNING', 'CANCELLING') ORDER BY created_at DESC")
    List<JobDO> selectActiveJobs(@Param("libraryId") Long libraryId, @Param("versionId") Long versionId);

    @Select("SELECT * FROM jobs ORDER BY created_at DESC")
    List<JobDO> selectAllJobs();

    @Update("UPDATE jobs SET status=#{status}, error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP, started_at=CASE WHEN #{status}='RUNNING' THEN CURRENT_TIMESTAMP ELSE started_at END, finished_at=CASE WHEN #{status} IN ('COMPLETED','FAILED','CANCELLED') THEN CURRENT_TIMESTAMP ELSE finished_at END WHERE id=#{jobId}")
    int updateStatus(@Param("jobId") String jobId,
                     @Param("status") String status,
                     @Param("errorMessage") String errorMessage);

    @Update("UPDATE jobs SET progress_pages=#{pages}, progress_max_pages=#{maxPages}, updated_at=CURRENT_TIMESTAMP WHERE id=#{jobId}")
    int updateProgress(@Param("jobId") String jobId,
                       @Param("pages") Integer pages,
                       @Param("maxPages") Integer maxPages);

    @Delete("DELETE FROM jobs WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')")
    int clearFinishedJobs();
}
