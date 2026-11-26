package com.GAJI.GAJI.location;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LocationMapper {

    // 동네 목록 조회 (회원가입 선택지)
    @Select("""
            SELECT location_id AS locationId, name
            FROM location
            ORDER BY location_id
            """)
    @ConstructorArgs({
            @Arg(column = "locationId", javaType = int.class),
            @Arg(column = "name", javaType = String.class)
    })
    List<LocationResponse> findAll();
}
