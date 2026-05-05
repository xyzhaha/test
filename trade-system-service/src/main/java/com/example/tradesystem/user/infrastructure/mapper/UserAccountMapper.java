package com.example.tradesystem.user.infrastructure.mapper;

import com.example.tradesystem.user.domain.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户账户Mapper
 */
@Mapper
public interface UserAccountMapper {

    /**
     * 查询用户账户(带行锁)
     */
    UserAccount selectForUpdate(@Param("userId") Long userId);

    /**
     * 查询用户账户
     */
    UserAccount selectById(@Param("userId") Long userId);

    /**
     * 插入用户账户
     */
    int insert(UserAccount userAccount);

    /**
     * 更新余额(乐观锁)
     */
    int updateBalanceWithVersion(@Param("userId") Long userId, 
                                  @Param("balance") java.math.BigDecimal balance,
                                  @Param("version") Integer version);

    /**
     * 扣减余额(乐观锁)
     */
    int deductBalanceWithVersion(@Param("userId") Long userId,
                                  @Param("amount") java.math.BigDecimal amount,
                                  @Param("version") Integer version);

    /**
     * 增加余额(乐观锁)
     */
    int increaseBalanceWithVersion(@Param("userId") Long userId,
                                    @Param("amount") java.math.BigDecimal amount,
                                    @Param("version") Integer version);
}
