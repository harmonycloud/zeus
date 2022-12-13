package com.middleware.zeus;

import com.dtflys.forest.springboot.annotation.ForestScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import springfox.documentation.oas.annotations.EnableOpenApi;

@EnableAsync
@EnableOpenApi
@EnableScheduling
@EnableFeignClients
@EnableTransactionManagement
@MapperScan("com.middleware.zeus.dao")
@ForestScan(basePackages = {"com.middleware.zeus.skyviewservice","com.middleware.zeus.httpservice", "com.middleware.zeus.integration.dashboard"})
@SpringBootApplication(scanBasePackages = "com.middleware", exclude = {LdapAutoConfiguration.class, RedisAutoConfiguration.class})
public class ZeusApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZeusApplication.class, args);
    }

}