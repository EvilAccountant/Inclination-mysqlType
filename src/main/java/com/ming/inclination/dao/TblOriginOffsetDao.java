package com.ming.inclination.dao;


import com.ming.inclination.entity.TblOriginOffset;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface TblOriginOffsetDao {

    int insert(@Param("pojo") TblOriginOffset pojo);

    int insertList(@Param("pojos") List<TblOriginOffset> pojo);

    List<TblOriginOffset> findDataByTimeRange(@Param("headTime") String headTime,@Param("endTime") String endTime);

    int update(@Param("pojo") TblOriginOffset pojo);

    String findBridgeIdByNo(@Param("bridgeNo") String bridgeNo);

    String findGroupIdByField(@Param("field") String field);

    String findSideTypeByGroupIdAndName(@Param("groupId") String groupId, @Param("name") String name);

    String findTypeIdByName(@Param("name") String name);

}
