package com.yongquan.propertysaas.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.yongquan.propertysaas.tenant.context.TenantContext;
import java.util.Locale;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class MybatisPlusTenantConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantProperties tenantProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new PropertyTenantLineHandler(tenantProperties)));
        return interceptor;
    }

    private static final class PropertyTenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

        private final TenantProperties tenantProperties;

        private PropertyTenantLineHandler(TenantProperties tenantProperties) {
            this.tenantProperties = tenantProperties;
        }

        @Override
        public Expression getTenantId() {
            return new LongValue(TenantContext.requiredTenantId());
        }

        @Override
        public boolean ignoreTable(String tableName) {
            if (!TenantContext.hasTenant() || TenantContext.isPlatformUser()) {
                return true;
            }
            return tenantProperties.getIgnoreTables().contains(tableName.toLowerCase(Locale.ROOT));
        }
    }
}
