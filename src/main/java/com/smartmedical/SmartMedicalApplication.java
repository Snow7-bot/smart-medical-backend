package com.smartmedical;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("com.smartmedical.**.mapper")
@ConfigurationPropertiesScan("com.smartmedical.config")
public class SmartMedicalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartMedicalApplication.class, args);
    }
}
