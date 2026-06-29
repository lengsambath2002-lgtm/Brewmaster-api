package com.sambath.admincafe.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class TenantKhqrSettings {

    @Column(name = "khqr_bakong_account_id")
    private String bakongAccountId;

    @Column(name = "khqr_merchant_name")
    private String merchantName;

    @Column(name = "khqr_merchant_city")
    private String merchantCity;

    @Column(name = "khqr_acquiring_bank")
    private String acquiringBank;

    @Column(name = "khqr_merchant_id")
    private String merchantId;

    @Column(name = "khqr_merchant_category_code")
    private String merchantCategoryCode;

    @Column(name = "khqr_currency")
    private String currency;

    @Column(name = "khqr_store_label")
    private String storeLabel;

    @Column(name = "khqr_terminal_label")
    private String terminalLabel;

    @Column(name = "khqr_mobile_number")
    private String mobileNumber;
}
