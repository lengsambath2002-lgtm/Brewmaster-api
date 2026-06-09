package com.sambath.admincafe.khqr;

import com.sambath.admincafe.khqr.dto.CheckResponse;
import com.sambath.admincafe.khqr.dto.DecodeResponse;
import com.sambath.admincafe.khqr.dto.DeeplinkRequest;
import com.sambath.admincafe.khqr.dto.DeeplinkResponse;
import com.sambath.admincafe.khqr.dto.GenerateIndividualRequest;
import com.sambath.admincafe.khqr.dto.GenerateMerchantRequest;
import com.sambath.admincafe.khqr.dto.KhqrResponse;
import com.sambath.admincafe.khqr.dto.OrderKhqrRequest;
import com.sambath.admincafe.khqr.dto.VerifyResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/khqr")
@RequiredArgsConstructor
public class KhqrController {

    private final KhqrService khqrService;

    @PostMapping("/individual")
    public KhqrResponse generateIndividual(@Valid @RequestBody GenerateIndividualRequest request) {
        return khqrService.generateIndividual(request);
    }

    @PostMapping("/merchant")
    public KhqrResponse generateMerchant(@Valid @RequestBody GenerateMerchantRequest request) {
        return khqrService.generateMerchant(request);
    }

    @PostMapping("/orders/{id}")
    public KhqrResponse generateForOrder(
            @PathVariable Long id,
            @RequestBody(required = false) OrderKhqrRequest request
    ) {
        return khqrService.generateForOrder(id, request);
    }

    @PostMapping("/verify")
    public VerifyResponse verify(@Valid @RequestBody QrPayload payload) {
        return khqrService.verify(payload.qr());
    }

    @PostMapping("/check")
    public CheckResponse check(@Valid @RequestBody Md5Payload payload) {
        return khqrService.checkByMd5(payload.md5());
    }

    @PostMapping("/decode")
    public DecodeResponse decode(@Valid @RequestBody QrPayload payload) {
        return khqrService.decode(payload.qr());
    }

    @PostMapping("/decode-non-khqr")
    public Map<String, Object> decodeNonKhqr(@Valid @RequestBody QrPayload payload) {
        return khqrService.decodeNonKhqr(payload.qr());
    }

    @PostMapping("/deeplink")
    public DeeplinkResponse generateDeeplink(@Valid @RequestBody DeeplinkRequest request) {
        return khqrService.generateDeeplink(request);
    }

    public record QrPayload(@NotBlank String qr) {}

    public record Md5Payload(@NotBlank String md5) {}
}
