package com.example.tradesystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商品交易系统启动类
 *
 * @author 技术架构团队
 * @date 2026-05-02
 */
@SpringBootApplication
@MapperScan("com.example.tradesystem.**.mapper")
@EnableScheduling
public class TradeSystemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeSystemServiceApplication.class, args);
    }

}
