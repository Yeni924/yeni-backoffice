package com.yeni.backoffice.core.payment.adapter;

import com.yeni.backoffice.core.payment.enums.PgCompany;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayAdapterResolver {

    private final Map<PgCompany, PaymentGatewayAdapter> adapters = new EnumMap<>(PgCompany.class);

    public PaymentGatewayAdapterResolver(List<PaymentGatewayAdapter> adapterList) {
        for (PaymentGatewayAdapter adapter : adapterList) {
            adapters.put(adapter.pgCompany(), adapter);
        }
    }

    public PaymentGatewayAdapter resolve(PgCompany pgCompany) {
        PaymentGatewayAdapter adapter = adapters.get(pgCompany);
        if (adapter == null) {
            throw new IllegalArgumentException("Payment gateway adapter not found: " + pgCompany);
        }
        return adapter;
    }
}
