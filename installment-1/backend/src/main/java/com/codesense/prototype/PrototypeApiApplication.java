package com.codesense.prototype;

import com.codesense.prototype.config.CodeSenseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(CodeSenseProperties.class)
public class PrototypeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrototypeApiApplication.class, args);
    }
}
