package iptv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Profiles;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.VendorExtension;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;


import java.util.ArrayList;
/**
 * Swagger2 配置类
 * 在与spring boot 集成时，放在与application.java 同级的目录下
 * 通过@Configuration注解，让spring来加载该配置
 */
@Configuration
/*@Profile({"product"})*/
class Swagger2Config {

    @Value("${swagger.is.enable}")
    private boolean swagger_is_enable;
    /**
     * 创建API应用
     * appinfo()增加API相关信息
     * 通过select()函数返回一个ApiSelectorBuilder实例，用来控制那些接口暴露给Swagger来展现
     * 本例采用置顶扫描的包路径来定义指定要建立API的目录
     *
     * @return
     */
    @Bean
    public Docket createRestApi() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .enable(swagger_is_enable)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("iptv.modules.music.controller"))
                .paths(PathSelectors.any()).build();
        return docket;
    }


    /**
     * 创建改API的基本信息（这些基本信息会展示在文档页面中）
     * 访问地址： http://项目实际地址/swagger-ui.html
     * @return
     */
    public ApiInfo apiInfo() {
        return new ApiInfoBuilder()

                // 设置页面标题
                .title("iptv在线后端api接口文档")
                // 设置联系人
               /* .contact(new Contact("iptv在线", "857569302@qq.com", "857569302@qq.com"))*/
                // 描述
                .description("欢迎访问接口文档")
                // 定义版本号
                .version("1.0").build();

    }
}
