package com.hotelos.service;

import com.hotelos.domain.Booking;
import com.hotelos.domain.BookingEvent;
import com.hotelos.domain.Order;
import com.hotelos.domain.Payment;
import com.hotelos.domain.Room;
import com.hotelos.domain.User;
import com.hotelos.domain.enums.BookingEventType;
import com.hotelos.domain.enums.BookingSource;
import com.hotelos.domain.enums.BookingStatus;
import com.hotelos.domain.enums.OccupancySourceType;
import com.hotelos.domain.enums.OrderStatus;
import com.hotelos.domain.enums.PaymentProvider;
import com.hotelos.domain.enums.PaymentStatus;
import com.hotelos.repository.BookingEventRepository;
import com.hotelos.repository.BookingRepository;
import com.hotelos.repository.OrderRepository;
import com.hotelos.repository.PaymentRepository;
import com.hotelos.util.PublicIds;
import com.hotelos.util.StayDates;
import com.hotelos.web.advice.ApiException;
import com.hotelos.web.dto.BookingDtos.BookingResponse;
import com.hotelos.web.dto.BookingDtos.ModifyBookingRequest;
import com.hotelos.web.dto.BookingDtos.WalkInRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingEventRepository bookingEventRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RoomQueryService roomQueryService;
    private final PricingService pricingService;
    private final OccupancyService occupancyService;

    @Transactional(readOnly = true)
    public List<BookingResponse> forCustomer(User user) {
        return bookingRepository.findByUser(user).stream()
                .map(booking -> BookingResponse.from(booking, paymentFor(booking.getOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> forManager() {
        return bookingRepository.findActive(BookingStatus.cancelled).stream()
                .map(booking -> BookingResponse.from(booking, paymentFor(booking.getOrder())))
                .toList();
    }

    @Transactional
    public BookingResponse createWalkIn(User manager, WalkInRequest request) {
        Room room = occupancyService.lockRoom(roomQueryService.requireByPublicId(request.roomId()).getId());
        LocalDate checkIn = request.checkIn();
        LocalDate checkOut = request.checkOut();
        assertBookable(room, checkIn, checkOut);

        PricingService.StayQuote quote = pricingService.quote(room, checkIn, checkOut);
        quote.requireAvailable();

        String guestEmail = request.guestEmail() == null || request.guestEmail().isBlank()
                ? walkInEmail(request.guestName())
                : request.guestEmail().trim().toLowerCase();

        Order order = new Order();
        order.setPublicId(PublicIds.next());
        order.setUser(null);
        order.setGuestName(request.guestName().trim());
        order.setGuestEmail(guestEmail);
        order.setGuestPhone(request.guestPhone().trim());
        order.setCurrency("INR");
        order.setSubtotalAmount(quote.total());
        order.setTotalAmount(quote.total());
        order.setStatus(OrderStatus.paid);
        order.setSource(BookingSource.walk_in);
        order.setNotes(request.notes());
        order.setCreatedBy(manager);
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setPublicId(PublicIds.next());
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.cash);
        payment.setAmount(quote.total());
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.captured);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);

        Booking booking = persistStay(
                order,
                room,
                null,
                request.guestName().trim(),
                guestEmail,
                request.guestPhone().trim(),
                checkIn,
                checkOut,
                quote,
                BookingSource.walk_in,
                request.notes(),
                manager
        );
        return BookingResponse.from(booking, payment);
    }

    @Transactional
    public Booking persistStay(
            Order order,
            Room room,
            User customer,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            PricingService.StayQuote quote,
            BookingSource source,
            String notes,
            User actor
    ) {
        Booking booking = new Booking();
        booking.setPublicId(PublicIds.next());
        booking.setOrder(order);
        booking.setRoom(room);
        booking.setUser(customer);
        booking.setGuestName(guestName);
        booking.setGuestEmail(guestEmail);
        booking.setGuestPhone(guestPhone);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setNights(quote.nights());
        booking.setPricePerNight(quote.pricePerNight());
        booking.setTotalAmount(quote.total());
        booking.setStatus(BookingStatus.confirmed);
        booking.setSource(source);
        booking.setNotes(notes);
        bookingRepository.save(booking);

        occupancyService.occupy(
                room,
                OccupancySourceType.booking,
                booking.getId(),
                StayDates.stayNights(checkIn, checkOut)
        );
        recordEvent(booking, actor, BookingEventType.created, null, snapshot(booking), "Booking created");
        return booking;
    }

    @Transactional
    public BookingResponse modify(User manager, String publicId, ModifyBookingRequest request) {
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (booking.getStatus() == BookingStatus.cancelled) {
            throw ApiException.badRequest("Cannot modify a cancelled booking");
        }

        Map<String, Object> before = snapshot(booking);
        Room nextRoom = request.roomId() == null
                ? booking.getRoom()
                : roomQueryService.requireByPublicId(request.roomId());
        LocalDate nextCheckIn = request.checkIn() == null ? booking.getCheckIn() : request.checkIn();
        LocalDate nextCheckOut = request.checkOut() == null ? booking.getCheckOut() : request.checkOut();

        occupancyService.release(OccupancySourceType.booking, booking.getId());
        Room locked = occupancyService.lockRoom(nextRoom.getId());
        assertBookable(locked, nextCheckIn, nextCheckOut);

        PricingService.StayQuote quote = pricingService.quote(locked, nextCheckIn, nextCheckOut);
        quote.requireAvailable();

        booking.setRoom(locked);
        booking.setCheckIn(nextCheckIn);
        booking.setCheckOut(nextCheckOut);
        booking.setNights(quote.nights());
        booking.setPricePerNight(quote.pricePerNight());
        booking.setTotalAmount(quote.total());
        if (request.guestName() != null && !request.guestName().isBlank()) {
            booking.setGuestName(request.guestName().trim());
        }
        if (request.guestPhone() != null && !request.guestPhone().isBlank()) {
            booking.setGuestPhone(request.guestPhone().trim());
        }
        if (request.notes() != null) {
            booking.setNotes(request.notes());
        }
        booking.setStatus(BookingStatus.modified);
        bookingRepository.save(booking);

        occupancyService.occupy(
                locked,
                OccupancySourceType.booking,
                booking.getId(),
                StayDates.stayNights(nextCheckIn, nextCheckOut)
        );
        recordEvent(booking, manager, BookingEventType.modified, before, snapshot(booking), "Booking modified");
        return BookingResponse.from(booking, paymentFor(booking.getOrder()));
    }

    @Transactional
    public BookingResponse cancel(User manager, String publicId) {
        Booking booking = bookingRepository.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (booking.getStatus() == BookingStatus.cancelled) {
            throw ApiException.badRequest("Booking is already cancelled");
        }

        Map<String, Object> before = snapshot(booking);
        occupancyService.release(OccupancySourceType.booking, booking.getId());
        booking.setStatus(BookingStatus.cancelled);
        booking.setCancelledAt(Instant.now());
        booking.setCancelledBy(manager);
        bookingRepository.save(booking);
        recordEvent(booking, manager, BookingEventType.cancelled, before, snapshot(booking), "Booking cancelled");
        return BookingResponse.from(booking, paymentFor(booking.getOrder()));
    }

    public void assertBookable(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (!roomQueryService.isBookable(room, checkIn, checkOut)) {
            throw ApiException.conflict("Room is not available for those dates");
        }
    }

    private Payment paymentFor(Order order) {
        return paymentRepository.findByOrder(order).orElse(null);
    }

    private void recordEvent(
            Booking booking,
            User actor,
            BookingEventType type,
            Map<String, Object> before,
            Map<String, Object> after,
            String note
    ) {
        BookingEvent event = new BookingEvent();
        event.setBooking(booking);
        event.setActor(actor);
        event.setEventType(type);
        event.setBeforeJson(before);
        event.setAfterJson(after);
        event.setNote(note);
        bookingEventRepository.save(event);
    }

    private Map<String, Object> snapshot(Booking booking) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("publicId", booking.getPublicId());
        data.put("roomId", booking.getRoom().getPublicId());
        data.put("guestName", booking.getGuestName());
        data.put("guestEmail", booking.getGuestEmail());
        data.put("guestPhone", booking.getGuestPhone());
        data.put("checkIn", booking.getCheckIn().toString());
        data.put("checkOut", booking.getCheckOut().toString());
        data.put("nights", booking.getNights());
        data.put("totalAmount", booking.getTotalAmount());
        data.put("status", booking.getStatus().name());
        return data;
    }

    private static String walkInEmail(String guestName) {
        String slug = guestName.trim().toLowerCase().replaceAll("[^a-z0-9]+", ".");
        if (slug.isBlank()) {
            slug = "guest";
        }
        return slug + "@walkin.local";
    }
}
