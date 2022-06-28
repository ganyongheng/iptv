package iptv.config;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("iptv.modules.*")
public class MybatisPlusConfig {
}
