package com.sambath.admincafe.khqr;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "khqr")
public class KhqrProperties {

    private String bakongAccountId;
    private String merchantName;
    private String merchantCity = "Phnom Penh";
    private String acquiringBank;
    private String merchantId;
    private String merchantCategoryCode = "5999";
    private String currency = "USD";
    private String storeLabel;
    private String terminalLabel;
    private String mobileNumber;
    private long expirationMinutes = 15;
    private Deeplink deeplink = new Deeplink();

    @Getter
    @Setter
    public static class Deeplink {
        private String apiUrl;
        private String appName;
        private String appIconUrl;
        private String appCallback;
    }
}
