package com.sambath.admincafe.khqr;

import com.sambath.admincafe.common.NotFoundException;
import com.sambath.admincafe.khqr.dto.DecodeResponse;
import com.sambath.admincafe.khqr.dto.DeeplinkRequest;
import com.sambath.admincafe.khqr.dto.DeeplinkResponse;
import com.sambath.admincafe.khqr.dto.GenerateIndividualRequest;
import com.sambath.admincafe.khqr.dto.GenerateMerchantRequest;
import com.sambath.admincafe.khqr.dto.KhqrResponse;
import com.sambath.admincafe.khqr.dto.OrderKhqrRequest;
import com.sambath.admincafe.khqr.dto.VerifyResponse;
import com.sambath.admincafe.order.Order;
import com.sambath.admincafe.order.OrderRepository;
import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.CRCValidation;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRDecodeData;
import kh.gov.nbc.bakong_khqr.model.KHQRDeepLinkData;
import kh.gov.nbc.bakong_khqr.model.KHQRHashMap;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.KHQRStatus;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;
import kh.gov.nbc.bakong_khqr.model.SourceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KhqrService {

    private final KhqrProperties properties;
    private final OrderRepository orderRepository;

    public KhqrResponse generateIndividual(GenerateIndividualRequest request) {
        IndividualInfo info = new IndividualInfo();
        info.setBakongAccountId(request.bakongAccountId());
        info.setMerchantName(request.merchantName());
        info.setMerchantCity(orDefault(request.merchantCity(), properties.getMerchantCity()));
        info.setAccountInformation(request.accountInformation());
        info.setAcquiringBank(orDefault(request.acquiringBank(), properties.getAcquiringBank()));
        info.setCurrency(parseCurrency(orDefault(request.currency(), properties.getCurrency())));
        info.setAmount(request.amount());
        info.setBillNumber(request.billNumber());
        info.setMobileNumber(orDefault(request.mobileNumber(), properties.getMobileNumber()));
        info.setStoreLabel(orDefault(request.storeLabel(), properties.getStoreLabel()));
        info.setTerminalLabel(orDefault(request.terminalLabel(), properties.getTerminalLabel()));
        info.setPurposeOfTransaction(request.purposeOfTransaction());
        info.setUpiAccountInformation(request.upiAccountInformation());
        info.setMerchantAlternateLanguagePreference(request.merchantAlternateLanguagePreference());
        info.setMerchantNameAlternateLanguage(request.merchantNameAlternateLanguage());
        info.setMerchantCityAlternateLanguage(request.merchantCityAlternateLanguage());
        info.setExpirationTimestamp(resolveExpiration(request.expirationTimestamp(), request.amount()));
        info.setMerchantCategoryCode(orDefault(request.merchantCategoryCode(), properties.getMerchantCategoryCode()));

        return toKhqrResponse(unwrap(BakongKHQR.generateIndividual(info)));
    }

    public KhqrResponse generateMerchant(GenerateMerchantRequest request) {
        MerchantInfo info = new MerchantInfo();
        info.setBakongAccountId(request.bakongAccountId());
        info.setMerchantId(request.merchantId());
        info.setAcquiringBank(request.acquiringBank());
        info.setMerchantName(request.merchantName());
        info.setMerchantCity(orDefault(request.merchantCity(), properties.getMerchantCity()));
        info.setCurrency(parseCurrency(orDefault(request.currency(), properties.getCurrency())));
        info.setAmount(request.amount());
        info.setBillNumber(request.billNumber());
        info.setMobileNumber(orDefault(request.mobileNumber(), properties.getMobileNumber()));
        info.setStoreLabel(orDefault(request.storeLabel(), properties.getStoreLabel()));
        info.setTerminalLabel(orDefault(request.terminalLabel(), properties.getTerminalLabel()));
        info.setPurposeOfTransaction(request.purposeOfTransaction());
        info.setUpiAccountInformation(request.upiAccountInformation());
        info.setMerchantAlternateLanguagePreference(request.merchantAlternateLanguagePreference());
        info.setMerchantNameAlternateLanguage(request.merchantNameAlternateLanguage());
        info.setMerchantCityAlternateLanguage(request.merchantCityAlternateLanguage());
        info.setExpirationTimestamp(resolveExpiration(request.expirationTimestamp(), request.amount()));
        info.setMerchantCategoryCode(orDefault(request.merchantCategoryCode(), properties.getMerchantCategoryCode()));

        return toKhqrResponse(unwrap(BakongKHQR.generateMerchant(info)));
    }

    public KhqrResponse generateForOrder(Long orderId, OrderKhqrRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        BigDecimal total = order.getTotal();
        Double amount = request != null && request.amount() != null
                ? request.amount()
                : (total != null ? total.doubleValue() : null);

        String billNumber = request != null && request.billNumber() != null
                ? request.billNumber()
                : defaultBillNumber(order);

        String currency = request != null && request.currency() != null
                ? request.currency()
                : properties.getCurrency();

        Long expiration = request != null && request.expirationTimestamp() != null
                ? request.expirationTimestamp()
                : null;

        String merchantId = properties.getMerchantId();
        if (merchantId != null && !merchantId.isBlank()) {
            return generateMerchant(new GenerateMerchantRequest(
                    properties.getBakongAccountId(),
                    merchantId,
                    properties.getAcquiringBank(),
                    properties.getMerchantName(),
                    properties.getMerchantCity(),
                    currency,
                    amount,
                    billNumber,
                    properties.getMobileNumber(),
                    properties.getStoreLabel(),
                    properties.getTerminalLabel(),
                    null, null, null, null, null,
                    expiration,
                    properties.getMerchantCategoryCode()
            ));
        }

        return generateIndividual(new GenerateIndividualRequest(
                properties.getBakongAccountId(),
                properties.getMerchantName(),
                properties.getMerchantCity(),
                properties.getAcquiringBank(),
                null,
                currency,
                amount,
                billNumber,
                properties.getMobileNumber(),
                properties.getStoreLabel(),
                properties.getTerminalLabel(),
                null, null, null, null, null,
                expiration,
                properties.getMerchantCategoryCode()
        ));
    }

    public VerifyResponse verify(String qrCode) {
        KHQRResponse<CRCValidation> response = BakongKHQR.verify(qrCode);
        CRCValidation data = response.getData();
        return new VerifyResponse(data != null && data.isValid());
    }

    public DecodeResponse decode(String qrCode) {
        KHQRDecodeData data = unwrap(BakongKHQR.decode(qrCode));
        return new DecodeResponse(
                data.getPayloadFormatIndicator(),
                data.getPointOfInitiationMethod(),
                data.getMerchantType(),
                data.getBakongAccountID(),
                data.getMerchantId(),
                data.getAccountInformation(),
                data.getUpiAccountInformation(),
                data.getAcquiringBank(),
                data.getMerchantCategoryCode(),
                data.getCountryCode(),
                data.getMerchantName(),
                data.getMerchantCity(),
                data.getTransactionCurrency(),
                data.getTransactionAmount(),
                data.getBillNumber(),
                data.getMobileNumber(),
                data.getStoreLabel(),
                data.getTerminalLabel(),
                data.getPurposeOfTransaction(),
                data.getMerchantAlternateLanguagePreference(),
                data.getMerchantNameAlternateLanguage(),
                data.getMerchantCityAlternateLanguage(),
                data.getCreationTimestamp(),
                data.getExpirationTimestamp(),
                data.getCrc()
        );
    }

    public Map<String, Object> decodeNonKhqr(String qrCode) {
        KHQRHashMap data = unwrap(BakongKHQR.decodeNonKHQR(qrCode));
        return data == null ? Map.of() : data;
    }

    public DeeplinkResponse generateDeeplink(DeeplinkRequest request) {
        String url = orDefault(request.url(), properties.getDeeplink().getApiUrl());
        if (url == null || url.isBlank()) {
            throw new KhqrException(29, "Deep Link URL is not configured.");
        }

        SourceInfo source = new SourceInfo();
        source.setAppName(orDefault(request.appName(), properties.getDeeplink().getAppName()));
        source.setAppIconUrl(orDefault(request.appIconUrl(), properties.getDeeplink().getAppIconUrl()));
        source.setAppDeepLinkCallback(orDefault(request.appCallback(), properties.getDeeplink().getAppCallback()));

        KHQRDeepLinkData data = unwrap(BakongKHQR.generateDeepLink(url, request.qr(), source));
        return new DeeplinkResponse(data.getShortLink());
    }

    private String defaultBillNumber(Order order) {
        if (order.getOrderDate() != null && order.getDailyNumber() != null) {
            return order.getOrderDate().toString().replace("-", "") + "-" + order.getDailyNumber();
        }
        return "ORDER-" + order.getId();
    }

    private Long resolveExpiration(Long explicit, Double amount) {
        if (explicit != null) {
            return explicit;
        }
        if (amount == null || amount <= 0d) {
            return null;
        }
        long minutes = properties.getExpirationMinutes();
        if (minutes <= 0) {
            return null;
        }
        return Instant.now().plusSeconds(minutes * 60L).toEpochMilli();
    }

    private KHQRCurrency parseCurrency(String value) {
        if (value == null || value.isBlank()) {
            return KHQRCurrency.USD;
        }
        try {
            return KHQRCurrency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new KhqrException(28, "Unsupported currency: " + value);
        }
    }

    private KhqrResponse toKhqrResponse(KHQRData data) {
        return new KhqrResponse(data.getQr(), data.getMd5());
    }

    private <T> T unwrap(KHQRResponse<T> response) {
        KHQRStatus status = response.getKHQRStatus();
        if (status == null || status.getCode() != 0) {
            Integer errorCode = status != null ? status.getErrorCode() : null;
            String message = status != null ? status.getMessage() : "Unknown KHQR error";
            throw new KhqrException(errorCode, message);
        }
        return response.getData();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
