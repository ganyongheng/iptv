package iptv.config.datasource;

import iptv.common.enums.DataSourceType;

import java.lang.annotation.*;

/**
 * 自定义多数据源切换注解
 *
 * @author Jason
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataSource {
    /**
     * 切换数据源key 默认master
     */
    public DataSourceType value() default DataSourceType.MASTER;
}