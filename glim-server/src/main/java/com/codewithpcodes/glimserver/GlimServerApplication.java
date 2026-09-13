package com.codewithpcodes.glimserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GlimServerApplication {

    static void main(String[] args) {
        SpringApplication.run(GlimServerApplication.class, args);
    }

}
