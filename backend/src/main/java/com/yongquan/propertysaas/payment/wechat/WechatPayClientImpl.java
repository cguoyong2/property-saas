package com.yongquan.propertysaas.payment.wechat;

import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import com.yongquan.propertysaas.payment.dto.WechatRefundNotifyRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class WechatPayClientImpl implements WechatPayClient {

    private final WechatPayProperties properties;

    public WechatPayClientImpl(WechatPayProperties properties) {
        this.properties = properties;
    }

    @Override
    public void verifyPayNotify(PayOrderView order, String secret, WechatPayNotifyRequest request) {
        if (properties.realWechatV3()) {
            requireRealWechatConfiguration();
            throw new IllegalStateException("真实微信支付 v3 回调需接入 HTTP 原始报文验签、resource 解密和重放校验后启用");
        }
        String plainText = order.orderNo() + "|" + request.thirdTradeNo() + "|" + money(request.amount()) + "|"
                + request.paidAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        verifyHmac(secret, plainText, request.signature(), "微信支付回调签名验证失败");
    }

    @Override
    public void verifyRefundNotify(Long tenantId, PayRefundView refund, String secret, WechatRefundNotifyRequest request) {
        if (properties.realWechatV3()) {
            requireRealWechatConfiguration();
            throw new IllegalStateException("真实微信退款 v3 回调需接入 HTTP 原始报文验签、resource 解密和重放校验后启用");
        }
        String plainText = refund.refundNo() + "|" + request.thirdRefundNo() + "|" + money(request.refundAmount()) + "|"
                + request.refundedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        verifyHmac(secret, plainText, request.signature(), "微信退款回调签名验证失败");
    }

    private void requireRealWechatConfiguration() {
        require(properties.getMerchantPrivateKeyPath(), "WECHAT_PAY_MERCHANT_PRIVATE_KEY_PATH");
        require(properties.getMerchantCertificatePath(), "WECHAT_PAY_MERCHANT_CERTIFICATE_PATH");
        require(properties.getPlatformCertificatePath(), "WECHAT_PAY_PLATFORM_CERTIFICATE_PATH");
        require(properties.getApiV3KeyRef(), "WECHAT_PAY_API_V3_KEY_REF");
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("真实微信支付配置缺失：" + name);
        }
    }

    private void verifyHmac(String secret, String plainText, String signature, String message) {
        String expected = hmacSha256Hex(secret, plainText);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException(message);
        }
    }

    private String hmacSha256Hex(String secret, String plainText) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("微信支付签名计算失败", ex);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
