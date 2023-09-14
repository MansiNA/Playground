package de.dbuss.tefcontrol.data;

import javax.sql.DataSource;

public class DynamicDataSourceContextHolder {
    private static final ThreadLocal<DataSource> contextHolder = new ThreadLocal<>();

    public static void setDataSource(DataSource dataSource) {
        contextHolder.set(dataSource);
    }

    public static DataSource getDataSource() {
        return contextHolder.get();
    }

    public static void clearDataSourceKey() {
        contextHolder.remove();
    }
}
