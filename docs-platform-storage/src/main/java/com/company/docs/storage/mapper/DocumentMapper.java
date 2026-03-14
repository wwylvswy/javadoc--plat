package com.company.docs.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.docs.storage.model.entity.DocumentDO;
import com.company.docs.storage.model.entity.DocumentSearchRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * documents 表访问接口。
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentDO> {

    @Insert("INSERT INTO documents(page_id, content, metadata, sort_order, created_at) VALUES(#{pageId}, #{content}, #{metadata}, #{sortOrder}, CURRENT_TIMESTAMP)")
    int insertOne(@Param("pageId") Long pageId,
                  @Param("content") String content,
                  @Param("metadata") String metadata,
                  @Param("sortOrder") Integer sortOrder);

    @Delete("DELETE FROM documents WHERE page_id=#{pageId}")
    int deleteByPageId(@Param("pageId") Long pageId);

    @Delete("DELETE FROM documents WHERE page_id IN (SELECT p.id FROM pages p WHERE p.version_id=#{versionId})")
    int deleteByVersionId(@Param("versionId") Long versionId);

    @Select("SELECT COUNT(1) FROM documents d JOIN pages p ON d.page_id=p.id JOIN versions v ON p.version_id=v.id JOIN libraries l ON v.library_id=l.id " +
            "WHERE lower(l.name)=lower(#{library}) AND lower(v.name)=lower(COALESCE(#{version}, ''))")
    int countByLibraryVersion(@Param("library") String library, @Param("version") String version);

    @Select("SELECT p.url AS url, p.title AS title, d.content AS content, bm25(documents_fts) AS score, v.name AS version " +
            "FROM documents_fts " +
            "JOIN documents d ON documents_fts.rowid=d.id " +
            "JOIN pages p ON d.page_id=p.id " +
            "JOIN versions v ON p.version_id=v.id " +
            "JOIN libraries l ON v.library_id=l.id " +
            "WHERE lower(l.name)=lower(#{library}) " +
            "AND lower(v.name)=lower(COALESCE(#{version}, '')) " +
            "AND documents_fts MATCH #{query} " +
            "ORDER BY bm25(documents_fts) LIMIT #{limit}")
    List<DocumentSearchRow> searchByFts(@Param("library") String library,
                                        @Param("version") String version,
                                        @Param("query") String query,
                                        @Param("limit") Integer limit);

    @Select("SELECT p.url AS url, p.title AS title, d.content AS content, 0.0 AS score, v.name AS version " +
            "FROM documents d " +
            "JOIN pages p ON d.page_id=p.id " +
            "JOIN versions v ON p.version_id=v.id " +
            "JOIN libraries l ON v.library_id=l.id " +
            "WHERE lower(l.name)=lower(#{library}) " +
            "AND lower(v.name)=lower(COALESCE(#{version}, '')) " +
            "AND lower(d.content) LIKE '%' || lower(#{query}) || '%' " +
            "LIMIT #{limit}")
    List<DocumentSearchRow> searchByLike(@Param("library") String library,
                                         @Param("version") String version,
                                         @Param("query") String query,
                                         @Param("limit") Integer limit);
}
