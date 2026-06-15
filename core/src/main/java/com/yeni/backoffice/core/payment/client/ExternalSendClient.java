package com.yeni.backoffice.core.payment.client;

import com.yeni.backoffice.core.payment.entity.ExternalSendRequest;

public interface ExternalSendClient {

    ExternalSendResult send(ExternalSendRequest request);
}
