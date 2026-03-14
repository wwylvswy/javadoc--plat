package com.company.docs.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.docs.storage.model.entity.LibraryVersionStatDO;
import com.company.docs.storage.model.entity.VersionDO;
import com.company.docs.storage.model.entity.VersionWithLibraryDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * versions 表访问接口。
 */
@Mapper
public interface VersionMapper extends BaseMapper<VersionDO> {

    @Select("SELECT * FROM versions WHERE library_id=#{libraryId} AND lower(name)=lower(#{name}) LIMIT 1")
    VersionDO selectByLibraryAndName(@Param("libraryId") Long libraryId, @Param("name") String name);

    @Insert("INSERT INTO versions(library_id, name, status, progress_pages, progress_max_pages, created_at, updated_at) VALUES(#{libraryId}, #{name}, 'NOT_INDEXED', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING")
    int insertIgnore(@Param("libraryId") Long libraryId, @Param("name") String name);

    @Update("UPDATE versions SET status=#{status}, error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP, started_at=CASE WHEN #{status}='RUNNING' THEN CURRENT_TIMESTAMP ELSE started_at END WHERE id=#{versionId}")
    int updateStatus(@Param("versionId") Long versionId, @Param("status") String status, @Param("errorMessage") String errorMessage);

    @Update("UPDATE versions SET progress_pages=#{pages}, progress_max_pages=#{maxPages}, updated_at=CURRENT_TIMESTAMP WHERE id=#{versionId}")
    int updateProgress(@Param("versionId") Long versionId, @Param("pages") Integer pages, @Param("maxPages") Integer maxPages);

    @Update("UPDATE versions SET source_url=#{sourceUrl}, scraper_options=#{optionsJson}, updated_at=CURRENT_TIMESTAMP WHERE id=#{versionId}")
    int updateScraperOptions(@Param("versionId") Long versionId,
                             @Param("sourceUrl") String sourceUrl,
                             @Param("optionsJson") String optionsJson);

    @Select("SELECT v.*, l.name AS library_name FROM versions v JOIN libraries l ON v.library_id=l.id WHERE v.status IN (${statusSql})")
    List<VersionWithLibraryDO> selectByStatusesRaw(@Param("statusSql") String statusSql);

    @Select("SELECT * FROM versions WHERE id=#{id}")
    VersionDO selectOneById(@Param("id") Long id);

    @Select("SELECT * FROM versions WHERE library_id=#{libraryId} ORDER BY name")
    List<VersionDO> selectByLibraryId(@Param("libraryId") Long libraryId);

    @Select("SELECT COUNT(1) FROM versions WHERE library_id=#{libraryId}")
    int countByLibraryId(@Param("libraryId") Long libraryId);

    @Select("SELECT\n" +
            "  l.name AS library,\n" +
            "  v.name AS version,\n" +
            "  v.id AS version_id,\n" +
            "  v.status AS status,\n" +
            "  v.progress_pages AS progress_pages,\n" +
            "  v.progress_max_pages AS progress_max_pages,\n" +
            "  v.source_url AS source_url,\n" +
            "  MIN(p.created_at) AS indexed_at,\n" +
            "  COUNT(d.id) AS document_count,\n" +
            "  COUNT(DISTINCT p.url) AS unique_url_count\n" +
            "FROM versions v\n" +
            "JOIN libraries l ON v.library_id=l.id\n" +
            "LEFT JOIN pages p ON p.version_id=v.id\n" +
            "LEFT JOIN documents d ON d.page_id=p.id\n" +
            "GROUP BY v.id\n" +
            "ORDER BY l.name, v.name")
    List<LibraryVersionStatDO> queryLibraryVersions();
}
