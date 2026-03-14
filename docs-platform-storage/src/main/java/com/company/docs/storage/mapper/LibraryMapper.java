package com.company.docs.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.docs.storage.model.entity.LibraryDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * libraries 表访问接口。
 */
@Mapper
public interface LibraryMapper extends BaseMapper<LibraryDO> {

    @Select("SELECT * FROM libraries WHERE lower(name)=lower(#{name}) LIMIT 1")
    LibraryDO selectByNameIgnoreCase(@Param("name") String name);

    @Insert("INSERT INTO libraries(name, created_at, updated_at) VALUES(#{name}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING")
    int insertIgnore(@Param("name") String name);

    @Select("SELECT COUNT(1) FROM libraries WHERE id=#{id}")
    int existsById(@Param("id") Long id);
}
