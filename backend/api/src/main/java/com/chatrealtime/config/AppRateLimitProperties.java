package com.chatrealtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public class AppRateLimitProperties {
    private boolean enabled = true;
    private boolean trustForwardedHeaders = false;
    private boolean distributed = false;
    private String keyPrefix = "in-chat:rate-limit";
    private int loginLimitPerMinute = 5;
    private int registerLimitPerMinute = 3;
    private int refreshLimitPerMinute = 20;
    private int uploadLimitPerMinute = 30;
    private int websocketLimitPerMinute = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public boolean isDistributed() {
        return distributed;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public int getLoginLimitPerMinute() {
        return loginLimitPerMinute;
    }

    public void setLoginLimitPerMinute(int loginLimitPerMinute) {
        this.loginLimitPerMinute = loginLimitPerMinute;
    }

    public int getRegisterLimitPerMinute() {
        return registerLimitPerMinute;
    }

    public void setRegisterLimitPerMinute(int registerLimitPerMinute) {
        this.registerLimitPerMinute = registerLimitPerMinute;
    }

    public int getRefreshLimitPerMinute() {
        return refreshLimitPerMinute;
    }

    public void setRefreshLimitPerMinute(int refreshLimitPerMinute) {
        this.refreshLimitPerMinute = refreshLimitPerMinute;
    }

    public int getUploadLimitPerMinute() {
        return uploadLimitPerMinute;
    }

    public void setUploadLimitPerMinute(int uploadLimitPerMinute) {
        this.uploadLimitPerMinute = uploadLimitPerMinute;
    }

    public int getWebsocketLimitPerMinute() {
        return websocketLimitPerMinute;
    }

    public void setWebsocketLimitPerMinute(int websocketLimitPerMinute) {
        this.websocketLimitPerMinute = websocketLimitPerMinute;
    }
}
