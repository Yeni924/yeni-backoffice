package com.yeni.backoffice.core.payment.gateway;

import com.yeni.backoffice.core.payment.enums.PgProvider;
import org.springframework.stereotype.Component;

@Component
public class PaymentGatewayRouter {

    public PgProvider route(PgProvider requestedProvider, String channelType, String storeCode, String paymentMethod) {
        if (requestedProvider != null) {
            return requestedProvider;
        }
        return PgProvider.MOCK;
    }
}
