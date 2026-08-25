package com.savory.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置类
 */
@Configuration
public class SpringDocConfiguration {

    @Bean
    public OpenAPI savoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("知味生活 · SavoryLife API")
                        .description("融合 AI Agent 的本地生活服务平台接口文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("SavoryLife Team"))
                        .license(new License()
                                .name("MIT License")));
    }
}
