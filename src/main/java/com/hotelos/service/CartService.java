package com.hotelos.service;

import com.hotelos.domain.CartItem;
import com.hotelos.domain.Room;
import com.hotelos.domain.User;
import com.hotelos.repository.CartItemRepository;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.CartDtos.AddCartItemRequest;
import com.hotelos.web.dto.CartDtos.CartItemResponse;
import com.hotelos.web.dto.CartDtos.CartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final RoomQueryService roomQueryService;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public CartResponse getCart(User user) {
        List<CartItemResponse> items = cartItemRepository.findByUserWithRoom(user).stream()
                .map(CartItemResponse::from)
                .toList();
        return CartResponse.of(items);
    }

    @Transactional
    public CartResponse addItem(User user, AddCartItemRequest request) {
        Room room = roomQueryService.requireByPublicId(request.roomId());
        if (!roomQueryService.isBookable(room, request.checkIn(), request.checkOut())) {
            throw ApiException.conflict("Room is not available for the selected dates");
        }

        PricingService.StayQuote quote = pricingService.quote(room, request.checkIn(), request.checkOut());
        quote.requireAvailable();

        CartItem item = cartItemRepository.findByUserAndRoom(user, room).orElseGet(CartItem::new);
        item.setUser(user);
        item.setRoom(room);
        item.setCheckIn(request.checkIn());
        item.setCheckOut(request.checkOut());
        item.setNights(quote.nights());
        item.setPricePerNight(quote.pricePerNight());
        item.setTotalAmount(quote.total());
        cartItemRepository.save(item);
        return getCart(user);
    }

    @Transactional
    public CartResponse removeItem(User user, String roomPublicId) {
        Room room = roomQueryService.requireByPublicId(roomPublicId);
        cartItemRepository.deleteByUserAndRoom(user, room);
        return getCart(user);
    }

    @Transactional
    public void clear(User user) {
        cartItemRepository.deleteByUser(user);
    }

    @Transactional(readOnly = true)
    public List<CartItem> items(User user) {
        return cartItemRepository.findByUserWithRoom(user);
    }
}
