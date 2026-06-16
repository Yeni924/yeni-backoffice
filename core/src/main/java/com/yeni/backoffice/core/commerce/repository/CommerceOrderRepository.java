package com.yeni.backoffice.core.commerce.repository;

import com.yeni.backoffice.core.commerce.entity.CommerceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommerceOrderRepository extends JpaRepository<CommerceOrder, Long> {

    Optional<CommerceOrder> findByOrderNo(String orderNo);

    List<CommerceOrder> findAllByOrderByIdDesc();
}
