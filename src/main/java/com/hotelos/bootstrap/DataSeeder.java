package com.hotelos.bootstrap;

import com.hotelos.domain.Amenity;
import com.hotelos.domain.Booking;
import com.hotelos.domain.BookingEvent;
import com.hotelos.domain.Order;
import com.hotelos.domain.Payment;
import com.hotelos.domain.Room;
import com.hotelos.domain.RoomBlock;
import com.hotelos.domain.User;
import com.hotelos.domain.enums.BookingEventType;
import com.hotelos.domain.enums.BookingSource;
import com.hotelos.domain.enums.BookingStatus;
import com.hotelos.domain.enums.OccupancySourceType;
import com.hotelos.domain.enums.OrderStatus;
import com.hotelos.domain.enums.PaymentProvider;
import com.hotelos.domain.enums.PaymentStatus;
import com.hotelos.domain.enums.RoomType;
import com.hotelos.domain.enums.UserRole;
import com.hotelos.repository.AmenityRepository;
import com.hotelos.repository.BookingEventRepository;
import com.hotelos.repository.BookingRepository;
import com.hotelos.repository.OrderRepository;
import com.hotelos.repository.PaymentRepository;
import com.hotelos.repository.RoomBlockRepository;
import com.hotelos.repository.RoomRepository;
import com.hotelos.repository.UserRepository;
import com.hotelos.service.OccupancyService;
import com.hotelos.util.PublicIds;
import com.hotelos.util.StayDates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventRepository bookingEventRepository;
    private final RoomBlockRepository roomBlockRepository;
    private final OccupancyService occupancyService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        User customer = user("Aisha Mehta", "customer@hotel.com", "customer123", "+91 98765 43210", UserRole.customer);
        User manager = user("Rohan Kapoor", "manager@hotel.com", "manager123", "+91 99887 11001", UserRole.manager);
        user("Priya Sharma", "admin@hotel.com", "admin123", "+91 98111 22002", UserRole.admin);

        Map<String, Amenity> amenities = Map.ofEntries(
                amenity("wifi", "Wi-Fi"),
                amenity("ac", "AC"),
                amenity("work_desk", "Work desk"),
                amenity("tea_kettle", "Tea kettle"),
                amenity("smart_tv", "Smart TV"),
                amenity("mini_fridge", "Mini fridge"),
                amenity("balcony", "Balcony"),
                amenity("rain_shower", "Rain shower"),
                amenity("mini_bar", "Mini bar"),
                amenity("lounge_chair", "Lounge chair"),
                amenity("bathrobe", "Bathrobe"),
                amenity("coffee_machine", "Coffee machine"),
                amenity("living_room", "Living room"),
                amenity("kitchenette", "Kitchenette"),
                amenity("bathtub", "Bathtub"),
                amenity("dining_area", "Dining area"),
                amenity("butler", "Butler service"),
                amenity("private_lounge", "Private lounge")
        );

        Room garden = room(
                "101",
                "Garden Standard",
                RoomType.standard,
                "Quiet courtyard view with crisp linens and a workspace for short stays.",
                2,
                "https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&w=1200&q=80",
                "4500",
                amenities,
                List.of("wifi", "ac", "work_desk", "tea_kettle")
        );
        room(
                "102",
                "City Standard",
                RoomType.standard,
                "Street-facing room with soft lighting and a compact lounge corner.",
                2,
                "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=80",
                "4800",
                amenities,
                List.of("wifi", "ac", "smart_tv", "mini_fridge")
        );
        Room palm = room(
                "201",
                "Palm Deluxe",
                RoomType.deluxe,
                "Spacious deluxe with balcony seating and rainfall shower.",
                3,
                "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80",
                "7200",
                amenities,
                List.of("wifi", "balcony", "rain_shower", "mini_bar")
        );
        room(
                "202",
                "Harbour Deluxe",
                RoomType.deluxe,
                "Corner deluxe with seating alcove and curated local amenities.",
                3,
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80",
                "7800",
                amenities,
                List.of("wifi", "lounge_chair", "bathrobe", "coffee_machine")
        );
        room(
                "301",
                "Lotus Suite",
                RoomType.suite,
                "Separate living area, king bed, and panoramic evening light.",
                4,
                "https://images.unsplash.com/photo-1578683010236-d716f9a3f461?auto=format&fit=crop&w=1200&q=80",
                "12500",
                amenities,
                List.of("wifi", "living_room", "kitchenette", "bathtub")
        );
        Room aurora = room(
                "401",
                "Aurora Presidential",
                RoomType.presidential,
                "Top-floor residence with dining space and private concierge access.",
                5,
                "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1200&q=80",
                "28000",
                amenities,
                List.of("wifi", "dining_area", "butler", "private_lounge")
        );

        LocalDate today = LocalDate.now();
        seedBooking(
                customer,
                garden,
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                today.plusDays(3),
                today.plusDays(5),
                new BigDecimal("4500.00"),
                new BigDecimal("9000.00"),
                BookingSource.online,
                PaymentProvider.razorpay,
                null
        );
        seedBooking(
                null,
                palm,
                "Vikram Nair",
                "vikram@example.com",
                "+91 99887 66554",
                today.plusDays(7),
                today.plusDays(10),
                new BigDecimal("7200.00"),
                new BigDecimal("21600.00"),
                BookingSource.walk_in,
                PaymentProvider.cash,
                "Arriving late evening"
        );

        RoomBlock block = new RoomBlock();
        block.setPublicId(PublicIds.next());
        block.setRoom(aurora);
        block.setStartDate(today.plusDays(12));
        block.setEndDate(today.plusDays(14));
        block.setReason("Maintenance — HVAC service");
        block.setCreatedBy(manager);
        roomBlockRepository.save(block);
        occupancyService.occupy(
                aurora,
                OccupancySourceType.block,
                block.getId(),
                StayDates.inclusiveNights(block.getStartDate(), block.getEndDate())
        );

        log.info("Seeded Hotel OS demo users, rooms, bookings, and blocks");
    }

    private User user(String name, String email, String password, String phone, UserRole role) {
        User user = new User();
        user.setPublicId(PublicIds.next());
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setRole(role);
        user.setActive(true);
        return userRepository.save(user);
    }

    private Map.Entry<String, Amenity> amenity(String code, String label) {
        Amenity amenity = new Amenity();
        amenity.setCode(code);
        amenity.setLabel(label);
        return Map.entry(code, amenityRepository.save(amenity));
    }

    private Room room(
            String number,
            String name,
            RoomType type,
            String description,
            int capacity,
            String imageUrl,
            String basePrice,
            Map<String, Amenity> amenities,
            List<String> amenityCodes
    ) {
        Room room = new Room();
        room.setPublicId(PublicIds.next());
        room.setRoomNumber(number);
        room.setName(name);
        room.setRoomType(type);
        room.setDescription(description);
        room.setCapacity(capacity);
        room.setImageUrl(imageUrl);
        room.setBasePrice(new BigDecimal(basePrice));
        room.setActive(true);
        amenityCodes.forEach(code -> room.getAmenities().add(amenities.get(code)));
        return roomRepository.save(room);
    }

    private void seedBooking(
            User customer,
            Room room,
            String guestName,
            String guestEmail,
            String guestPhone,
            LocalDate checkIn,
            LocalDate checkOut,
            BigDecimal pricePerNight,
            BigDecimal total,
            BookingSource source,
            PaymentProvider provider,
            String notes
    ) {
        Order order = new Order();
        order.setPublicId(PublicIds.next());
        order.setUser(customer);
        order.setGuestName(guestName);
        order.setGuestEmail(guestEmail);
        order.setGuestPhone(guestPhone);
        order.setCurrency("INR");
        order.setSubtotalAmount(total);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.paid);
        order.setSource(source);
        order.setNotes(notes);
        orderRepository.save(order);

        Payment payment = new Payment();
        payment.setPublicId(PublicIds.next());
        payment.setOrder(order);
        payment.setProvider(provider);
        payment.setAmount(total);
        payment.setCurrency("INR");
        payment.setStatus(PaymentStatus.captured);
        payment.setPaidAt(Instant.now());
        if (provider == PaymentProvider.razorpay) {
            payment.setRazorpayPaymentId("pay_demo_" + order.getPublicId());
        }
        paymentRepository.save(payment);

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
        booking.setNights(StayDates.stayNights(checkIn, checkOut).size());
        booking.setPricePerNight(pricePerNight);
        booking.setTotalAmount(total);
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

        BookingEvent event = new BookingEvent();
        event.setBooking(booking);
        event.setActor(customer);
        event.setEventType(BookingEventType.created);
        event.setAfterJson(Map.of(
                "publicId", booking.getPublicId(),
                "roomId", room.getPublicId(),
                "status", booking.getStatus().name()
        ));
        event.setNote("Seed booking");
        bookingEventRepository.save(event);
    }
}
