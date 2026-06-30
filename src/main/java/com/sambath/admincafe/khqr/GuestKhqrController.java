package com.sambath.admincafe.khqr;

import com.sambath.admincafe.khqr.dto.CheckResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guest/khqr")
@RequiredArgsConstructor
public class GuestKhqrController {

    private final KhqrService khqrService;

    @PostMapping("/check")
    public CheckResponse check(@Valid @RequestBody CheckPayload payload) {
        return khqrService.checkByMd5(payload.orderId(), payload.md5());
    }

    public record CheckPayload(@NotNull Long orderId, @NotBlank String md5) {}
}
