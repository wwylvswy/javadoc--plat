package com.company.docs.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.docs.storage.model.entity.PageDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * pages 表访问接口。
 */
@Mapper
public interface PageMapper extends BaseMapper<PageDO> {

    @Select("SELECT * FROM pages WHERE version_id=#{versionId} AND url=#{url} LIMIT 1")
    PageDO selectByVersionAndUrl(@Param("versionId") Long versionId, @Param("url") String url);

    @Insert("INSERT INTO pages(version_id, url, title, etag, last_modified, content_type, depth, created_at, updated_at) " +
            "VALUES(#{versionId}, #{url}, #{title}, #{etag}, #{lastModified}, #{contentType}, #{depth}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
            "ON CONFLICT(version_id, url) DO UPDATE SET title=excluded.title, etag=excluded.etag, last_modified=excluded.last_modified, content_type=excluded.content_type, depth=excluded.depth, updated_at=CURRENT_TIMESTAMP")
    int upsert(@Param("versionId") Long versionId,
               @Param("url") String url,
               @Param("title") String title,
               @Param("etag") String etag,
               @Param("lastModified") String lastModified,
               @Param("contentType") String contentType,
               @Param("depth") Integer depth);

    @Select("SELECT * FROM pages WHERE version_id=#{versionId} ORDER BY id")
    List<PageDO> selectByVersionId(@Param("versionId") Long versionId);

    @Select("SELECT p.* FROM pages p JOIN versions v ON p.version_id=v.id JOIN libraries l ON v.library_id=l.id WHERE lower(l.name)=lower(#{library}) AND lower(v.name)=lower(COALESCE(#{version}, ''))")
    List<PageDO> selectByLibraryVersion(@Param("library") String library, @Param("version") String version);
}
