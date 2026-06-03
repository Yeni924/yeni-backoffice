package com.yeni.backoffice.core.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "portfolio.inicis")
public class InicisStdPayProperties {

    private String mid = "INIpayTest";
    private String signKey = "portfolio-local-sign-key";
    private String returnUrl = "http://localhost:8080/api/payments/inicis/stdpay/return";
    private String closeUrl = "http://localhost:8080/payments/close";
}
