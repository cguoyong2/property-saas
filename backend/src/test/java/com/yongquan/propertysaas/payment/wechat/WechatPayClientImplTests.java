package com.yongquan.propertysaas.payment.wechat;

import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class WechatPayClientImplTests {

    @Test
    void devModeAcceptsLegacyHmacSignature() throws Exception {
        WechatPayProperties properties = new WechatPayProperties();
        properties.setMode("DEV_SIMULATED");
        WechatPayClientImpl client = new WechatPayClientImpl(properties);
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 10, 10, 0);
        PayOrderView order = new PayOrderView(1L, 1L, 101L, "PAY-1-101-1", 1001L, "WECHAT",
                new BigDecimal("150.00"), "subject", "PAYING", paidAt.plusMinutes(30), null, null, paidAt);
        String plainText = "PAY-1-101-1|WX-TRADE-1|150.00|2026-06-10T10:00:00";
        WechatPayNotifyRequest request = new WechatPayNotifyRequest("PAY-1-101-1", "WX-TRADE-1",
                new BigDecimal("150.00"), paidAt, hmac("secret", plainText), null);

        client.verifyPayNotify(order, "secret", request);
    }

    @Test
    void realModeRequiresRealWechatConfiguration() {
        WechatPayProperties properties = new WechatPayProperties();
        properties.setMode("REAL_WECHAT_V3");
        WechatPayClientImpl client = new WechatPayClientImpl(properties);
        LocalDateTime paidAt = LocalDateTime.of(2026, 6, 10, 10, 0);
        PayOrderView order = new PayOrderView(1L, 1L, 101L, "PAY-1-101-1", 1001L, "WECHAT",
                new BigDecimal("150.00"), "subject", "PAYING", paidAt.plusMinutes(30), null, null, paidAt);
        WechatPayNotifyRequest request = new WechatPayNotifyRequest("PAY-1-101-1", "WX-TRADE-1",
                new BigDecimal("150.00"), paidAt, "signature", null);

        Assertions.assertThatThrownBy(() -> client.verifyPayNotify(order, "secret", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("真实微信支付配置缺失");
    }

    private String hmac(String secret, String plainText) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
