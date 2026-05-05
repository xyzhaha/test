package com.example.tradesystem.common.infrastructure.mapper;

import com.example.tradesystem.common.model.ExceptionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 异常记录Mapper
 */
@Mapper
public interface ExceptionRecordMapper {

    /**
     * 插入异常记录
     */
    int insert(ExceptionRecord exceptionRecord);

    /**
     * 根据异常ID查询
     */
    ExceptionRecord selectById(@Param("exceptionId") String exceptionId);

    /**
     * 查询待处理的异常列表
     */
    java.util.List<ExceptionRecord> selectPendingList(@Param("offset") int offset, 
                                                       @Param("limit") int limit);

    /**
     * 统计待处理异常数量
     */
    long countPending();

    /**
     * 更新异常状态为已解决
     */
    int updateStatusToResolved(@Param("exceptionId") String exceptionId);
}
