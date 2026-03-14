package com.company.docs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类。
 */
@SpringBootApplication(scanBasePackages = "com.company.docs")
@MapperScan("com.company.docs.storage.mapper")
public class DocsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocsPlatformApplication.class, args);
    }
}
