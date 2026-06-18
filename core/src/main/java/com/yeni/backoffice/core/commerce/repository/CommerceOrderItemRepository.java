package com.yeni.backoffice.core.commerce.repository;

import com.yeni.backoffice.core.commerce.entity.CommerceOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommerceOrderItemRepository extends JpaRepository<CommerceOrderItem, Long> {

    List<CommerceOrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    long countByOrderId(Long orderId);
}
