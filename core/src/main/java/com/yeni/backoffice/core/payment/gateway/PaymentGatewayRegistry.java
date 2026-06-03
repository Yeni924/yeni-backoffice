package com.yeni.backoffice.core.payment.gateway;

import com.yeni.backoffice.core.payment.enums.PgProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayRegistry {

    private final Map<PgProvider, PaymentGateway> gateways = new EnumMap<>(PgProvider.class);

    public PaymentGatewayRegistry(List<PaymentGateway> paymentGateways) {
        for (PaymentGateway gateway : paymentGateways) {
            gateways.put(gateway.provider(), gateway);
        }
    }

    public PaymentGateway get(PgProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new IllegalArgumentException("Payment gateway is not registered: " + provider);
        }
        return gateway;
    }
}
