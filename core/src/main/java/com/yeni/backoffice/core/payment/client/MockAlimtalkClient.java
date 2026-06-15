package com.yeni.backoffice.core.payment.client;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;
import org.springframework.stereotype.Component;

@Component
public class MockAlimtalkClient implements AlimtalkClient {

    @Override
    public AlimtalkSendResult send(AlimtalkQueue queue) {
        if (queue.getMessageKey().toUpperCase().contains("FAIL")) {
            return new AlimtalkSendResult(false, null, "Mock 알림톡 발송 실패");
        }
        return new AlimtalkSendResult(true, "MOCK-" + queue.getId(), "Mock 알림톡 발송 성공");
    }
}
