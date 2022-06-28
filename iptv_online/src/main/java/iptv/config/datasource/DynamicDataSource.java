package iptv.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 动态数据源，配置好后可以通过key获取数据源
 *
 * @author Jason
 */
public class DynamicDataSource extends AbstractRoutingDataSource {
    /**
     * 如果不希望数据源在启动配置时就加载好，可以定制这个方法，从任何你希望的地方读取并返回数据源
     * 比如从数据库、文件、外部接口等读取数据源信息，并最终返回一个DataSource实现类对象即可
     * @return DataSource
     */
    @Override
    protected DataSource determineTargetDataSource() {
        return super.determineTargetDataSource();
    }

    /**
     * 设置需要获取的数据源的key，通过该key获取数据源，该方法直接返回需要获取的数据源的key即可
     * targetDataSources找不到该key，则会使用默认数据源
     * @return
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }

    /**
     * 设置动态数据源，设置后可以通过determineCurrentLookupKey方法返回的key动态获取targetDataSources中的数据源
     * @param defaultTargetDataSource 默认数据源
     * @param targetDataSources 动态数据源集合：key={@link iptv.common.enums.DataSourceType} value={@link DataSource}
     */
    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources) {
        // 设置默认数据源（MASTER）
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        // 设置指定数据源（MASTER SLAVE SLAVE2）
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }
}