package com.yongquan.propertysaas.payment.wechat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "property-saas.payment.wechat")
public class WechatPayProperties {

    private String mode = "DEV_SIMULATED";
    private long callbackToleranceSeconds = 300;
    private String merchantPrivateKeyPath;
    private String merchantCertificatePath;
    private String platformCertificatePath;
    private String apiV3KeyRef;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public long getCallbackToleranceSeconds() {
        return callbackToleranceSeconds;
    }

    public void setCallbackToleranceSeconds(long callbackToleranceSeconds) {
        this.callbackToleranceSeconds = callbackToleranceSeconds;
    }

    public String getMerchantPrivateKeyPath() {
        return merchantPrivateKeyPath;
    }

    public void setMerchantPrivateKeyPath(String merchantPrivateKeyPath) {
        this.merchantPrivateKeyPath = merchantPrivateKeyPath;
    }

    public String getMerchantCertificatePath() {
        return merchantCertificatePath;
    }

    public void setMerchantCertificatePath(String merchantCertificatePath) {
        this.merchantCertificatePath = merchantCertificatePath;
    }

    public String getPlatformCertificatePath() {
        return platformCertificatePath;
    }

    public void setPlatformCertificatePath(String platformCertificatePath) {
        this.platformCertificatePath = platformCertificatePath;
    }

    public String getApiV3KeyRef() {
        return apiV3KeyRef;
    }

    public void setApiV3KeyRef(String apiV3KeyRef) {
        this.apiV3KeyRef = apiV3KeyRef;
    }

    public boolean realWechatV3() {
        return "REAL_WECHAT_V3".equalsIgnoreCase(mode);
    }
}
