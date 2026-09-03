package com.savory.modules.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * 多数据源路由由 baomidou dynamic-datasource 自动配置，
 * 通过 @DS 注解指定各 Mapper 使用的数据源：
 *   @DS("auth")     → savory_auth     (认证)
 *   @DS("user")     → savory_user     (用户)
 *   @DS("merchant") → savory_merchant (商家)
 *   @DS("trade")    → savory_trade    (交易)
 *   @DS("market")   → savory_market   (营销)
 *   @DS("social")   → savory_social   (社区)
 */
@Configuration
@MapperScan(basePackages = {
        "com.savory.*.mapper",
        "com.savory.market.shortlink.mapper",
        "com.savory.trade.pay.mapper"
})
public class DataSourceConfig {
}
