package com.yongquan.propertysaas.payment.wechat;

import com.yongquan.propertysaas.payment.domain.PayOrderView;
import com.yongquan.propertysaas.payment.domain.PayRefundView;
import com.yongquan.propertysaas.payment.dto.WechatPayNotifyRequest;
import com.yongquan.propertysaas.payment.dto.WechatRefundNotifyRequest;

public interface WechatPayClient {

    void verifyPayNotify(PayOrderView order, String secret, WechatPayNotifyRequest request);

    void verifyRefundNotify(Long tenantId, PayRefundView refund, String secret, WechatRefundNotifyRequest request);
}
