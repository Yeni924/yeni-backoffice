package com.yeni.backoffice.core.payment.client;

import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;
import org.springframework.stereotype.Component;

@Component
public class MockExternalSendClient implements ExternalSendClient {

    @Override
    public ExternalSendResult send(ExternalSendRequest request) {
        if (request.getRequestKey().toUpperCase().contains("FAIL")) {
            return new ExternalSendResult(false, "MOCK_FAIL", "Mock 외부전송 실패");
        }
        return new ExternalSendResult(true, "MOCK_OK", "Mock 외부전송 성공");
    }
}
