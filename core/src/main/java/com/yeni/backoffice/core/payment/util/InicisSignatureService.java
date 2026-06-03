package com.yeni.backoffice.core.payment.util;

import com.yeni.backoffice.core.payment.config.InicisStdPayProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class InicisSignatureService {

    private final InicisStdPayProperties properties;

    public InicisSignatureService(InicisStdPayProperties properties) {
        this.properties = properties;
    }

    public String createReadySignature(String orderNo, BigDecimal amount, String timestamp) {
        return sha256("oid=" + orderNo + "&price=" + normalizeAmount(amount) + "&timestamp=" + timestamp);
    }

    public String createAuthSignature(String orderNo, BigDecimal amount, String authToken) {
        return sha256("oid=" + orderNo + "&price=" + normalizeAmount(amount) + "&authToken=" + authToken);
    }

    public String createMKey() {
        return sha256(properties.getSignKey());
    }

    public boolean matchesAuthSignature(String orderNo, BigDecimal amount, String authToken, String signature) {
        return createAuthSignature(orderNo, amount, authToken).equalsIgnoreCase(signature);
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : encoded) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
