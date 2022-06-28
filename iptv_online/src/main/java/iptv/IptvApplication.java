package iptv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2;



@EnableScheduling  //开启定时任务
@EnableAsync // 启动异步调用
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableSwagger2
public class IptvApplication {

    public static void main(String[] args) {
        SpringApplication.run(IptvApplication.class, args);
    }

}
