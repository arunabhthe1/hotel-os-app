package com.hotelos.web;

import com.hotelos.security.CurrentUserService;
import com.hotelos.service.BookingService;
import com.hotelos.service.CartService;
import com.hotelos.service.CheckoutService;
import com.hotelos.web.dto.BookingDtos.BookingResponse;
import com.hotelos.web.dto.BookingDtos.CheckoutRequest;
import com.hotelos.web.dto.BookingDtos.CheckoutResponse;
import com.hotelos.web.dto.BookingDtos.ConfirmPaymentRequest;
import com.hotelos.web.dto.CartDtos.AddCartItemRequest;
import com.hotelos.web.dto.CartDtos.CartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CurrentUserService currentUserService;
    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final BookingService bookingService;

    @GetMapping("/cart")
    public CartResponse cart() {
        return cartService.getCart(currentUserService.entity());
    }

    @PostMapping("/cart")
    public CartResponse addToCart(@Valid @RequestBody AddCartItemRequest request) {
        return cartService.addItem(currentUserService.entity(), request);
    }

    @DeleteMapping("/cart/{roomId}")
    public CartResponse removeFromCart(@PathVariable String roomId) {
        return cartService.removeItem(currentUserService.entity(), roomId);
    }

    @DeleteMapping("/cart")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart() {
        cartService.clear(currentUserService.entity());
    }

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return checkoutService.checkout(currentUserService.entity(), request);
    }

    @PostMapping("/checkout/{orderId}/confirm")
    public CheckoutResponse confirm(
            @PathVariable String orderId,
            @RequestBody(required = false) ConfirmPaymentRequest request
    ) {
        ConfirmPaymentRequest body = request == null
                ? new ConfirmPaymentRequest(null, null, null)
                : request;
        return checkoutService.confirm(currentUserService.entity(), orderId, body);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> bookings() {
        return bookingService.forCustomer(currentUserService.entity());
    }
}
