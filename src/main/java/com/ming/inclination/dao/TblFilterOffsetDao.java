package com.ming.inclination.dao;


import com.ming.inclination.entity.TblFilterOffset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TblFilterOffsetDao {

    List<TblFilterOffset> findDataByTimeRange(@Param("headTime") String headTime, @Param("endTime") String endTime);

    void insertList(@Param("pojos") List<TblFilterOffset> pojos);

}
