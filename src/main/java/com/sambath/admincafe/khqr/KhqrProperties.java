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
    private BakongApi bakongApi = new BakongApi();

    @Getter
    @Setter
    public static class Deeplink {
        private String apiUrl;
        private String appName;
        private String appIconUrl;
        private String appCallback;
    }

    // Bakong Open API — used to check whether a generated KHQR has been paid
    // (POST {baseUrl}/v1/check_transaction_by_md5 with a developer token).
    @Getter
    @Setter
    public static class BakongApi {
        private String baseUrl = "https://api-bakong.nbc.gov.kh";
        private String token;
    }
}
