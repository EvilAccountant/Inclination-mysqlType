package com.ming.inclination.dao;


import com.ming.inclination.entity.TblDataOffset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TblDataOffsetDao {

    List<TblDataOffset> findDataByTimeRange(@Param("headTime") String headTime, @Param("endTime") String endTime);

}
