package com.yeni.backoffice.core.payment.client;

import com.yeni.backoffice.core.payment.entity.AlimtalkQueue;

public interface AlimtalkClient {

    AlimtalkSendResult send(AlimtalkQueue queue);
}
