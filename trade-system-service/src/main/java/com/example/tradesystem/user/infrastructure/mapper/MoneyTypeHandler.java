package com.example.tradesystem.user.infrastructure.mapper;

import com.example.tradesystem.user.domain.model.Money;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Money类型的TypeHandler
 * 用于在MyBatis中处理Money对象与数据库BigDecimal之间的转换
 */
@MappedTypes(Money.class)
public class MoneyTypeHandler extends BaseTypeHandler<Money> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Money parameter, JdbcType jdbcType) throws SQLException {
        ps.setBigDecimal(i, parameter.getAmount());
    }

    @Override
    public Money getNullableResult(ResultSet rs, String columnName) throws SQLException {
        BigDecimal amount = rs.getBigDecimal(columnName);
        if (amount == null) {
            return null;
        }
        return new Money(amount);
    }

    @Override
    public Money getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        BigDecimal amount = rs.getBigDecimal(columnIndex);
        if (amount == null) {
            return null;
        }
        return new Money(amount);
    }

    @Override
    public Money getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        BigDecimal amount = cs.getBigDecimal(columnIndex);
        if (amount == null) {
            return null;
        }
        return new Money(amount);
    }
}
