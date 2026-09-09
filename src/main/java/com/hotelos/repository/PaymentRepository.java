package com.hotelos.repository;

import com.hotelos.domain.Order;
import com.hotelos.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder(Order order);

    Optional<Payment> findByPublicId(String publicId);
}
