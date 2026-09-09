package com.hotelos.service;

import com.hotelos.config.RazorpayProperties;
import com.hotelos.domain.CartItem;
import com.hotelos.domain.Order;
import com.hotelos.domain.Payment;
import com.hotelos.domain.Room;
import com.hotelos.domain.User;
import com.hotelos.domain.enums.BookingSource;
import com.hotelos.domain.enums.OrderStatus;
import com.hotelos.domain.enums.PaymentProvider;
import com.hotelos.domain.enums.PaymentStatus;
import com.hotelos.repository.OrderRepository;
import com.hotelos.repository.PaymentRepository;
import com.hotelos.util.PublicIds;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.BookingDtos.CheckoutRequest;
import com.hotelos.web.dto.BookingDtos.CheckoutResponse;
import com.hotelos.web.dto.BookingDtos.ConfirmPaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartService cartService;
    private final RoomQueryService roomQueryService;
    private final PricingService pricingService;
    private final OccupancyService occupancyService;
    private final BookingService bookingService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayProperties razorpayProperties;

    @Transactional
    public CheckoutResponse checkout(User customer, CheckoutRequest request) {
        List<CartItem> items = cartService.items(customer);
        if (items.isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        items.stream()
                .sorted(Comparator.comparing(item -> item.getRoom().getId()))
                .forEach(item -> {
                    Room locked = occupancyService.lockRoom(item.getRoom().getId());
                    bookingService.assertBookable(locked, item.getCheckIn(), item.getCheckOut());
                });

        BigDecimal total = items.stream()
                .map(CartItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setPublicId(PublicIds.next());
        order.setUser(customer);
        order.setGuestName(request.guestName().trim());
        order.setGuestEmail(request.guestEmail().trim().toLowerCase());
        order.setGuestPhone(request.guestPhone().trim());
        order.setCurrency("INR");
        order.setSubtotalAmount(total);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.pending_payment);
        order.setSource(BookingSource.online);
        order.setCreatedBy(customer);
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setPublicId(PublicIds.next());
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.razorpay);
        payment.setAmount(total);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.created);
        boolean simulated = !razorpayProperties.enabled();
        payment.setRazorpayOrderId(simulated ? "order_sim_" + order.getPublicId() : "order_pending_" + order.getPublicId());
        payment.setRawPayload(Map.of("simulated", simulated, "cartSize", items.size()));
        paymentRepository.save(payment);

        return new CheckoutResponse(
                order.getPublicId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                payment.getRazorpayOrderId(),
                blankToNull(razorpayProperties.keyId()),
                simulated
        );
    }

    @Transactional
    public CheckoutResponse confirm(User customer, String orderPublicId, ConfirmPaymentRequest request) {
        Order order = orderRepository.findByPublicId(orderPublicId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        if (order.getUser() == null || !order.getUser().getId().equals(customer.getId())) {
            throw ApiException.forbidden("This order does not belong to you");
        }
        if (order.getStatus() == OrderStatus.paid) {
            Payment existing = paymentRepository.findByOrder(order).orElse(null);
            return toResponse(order, existing);
        }
        if (order.getStatus() != OrderStatus.pending_payment) {
            throw ApiException.badRequest("Order cannot be confirmed");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> ApiException.notFound("Payment not found"));

        boolean simulated = !razorpayProperties.enabled();
        if (!simulated) {
            verifySignature(request);
            payment.setRazorpayOrderId(request.razorpayOrderId());
            payment.setRazorpayPaymentId(request.razorpayPaymentId());
            payment.setRazorpaySignature(request.razorpaySignature());
        } else {
            payment.setRazorpayPaymentId("pay_sim_" + payment.getPublicId());
        }

        List<CartItem> items = cartService.items(customer);
        if (items.isEmpty()) {
            throw ApiException.badRequest("Cart is empty");
        }

        items.stream()
                .sorted(Comparator.comparing(item -> item.getRoom().getId()))
                .forEach(item -> {
                    Room locked = occupancyService.lockRoom(item.getRoom().getId());
                    PricingService.StayQuote quote = pricingService.quote(locked, item.getCheckIn(), item.getCheckOut());
                    quote.requireAvailable();
                    bookingService.assertBookable(locked, item.getCheckIn(), item.getCheckOut());
                    bookingService.persistStay(
                            order,
                            locked,
                            customer,
                            order.getGuestName(),
                            order.getGuestEmail(),
                            order.getGuestPhone(),
                            item.getCheckIn(),
                            item.getCheckOut(),
                            quote,
                            BookingSource.online,
                            null,
                            customer
                    );
                });

        payment.setStatus(PaymentStatus.captured);
        payment.setPaidAt(Instant.now());
        order.setStatus(OrderStatus.paid);
        paymentRepository.save(payment);
        orderRepository.save(order);
        cartService.clear(customer);
        return toResponse(order, payment);
    }

    private CheckoutResponse toResponse(Order order, Payment payment) {
        boolean simulated = payment != null
                && payment.getRazorpayOrderId() != null
                && payment.getRazorpayOrderId().startsWith("order_sim_");
        return new CheckoutResponse(
                order.getPublicId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                payment == null ? null : payment.getRazorpayOrderId(),
                blankToNull(razorpayProperties.keyId()),
                simulated
        );
    }

    private void verifySignature(ConfirmPaymentRequest request) {
        if (request == null
                || request.razorpayOrderId() == null
                || request.razorpayPaymentId() == null
                || request.razorpaySignature() == null) {
            throw ApiException.badRequest("Razorpay payment details are required");
        }
        String payload = request.razorpayOrderId() + "|" + request.razorpayPaymentId();
        String expected = hmacSha256(payload, razorpayProperties.keySecret());
        if (!expected.equals(request.razorpaySignature())) {
            throw ApiException.badRequest("Invalid Razorpay signature");
        }
    }

    private static String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Unable to verify payment signature", ex);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
