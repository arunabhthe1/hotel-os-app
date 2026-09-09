package com.hotelos.repository;

import com.hotelos.domain.CartItem;
import com.hotelos.domain.Room;
import com.hotelos.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select c from CartItem c join fetch c.room r left join fetch r.amenities where c.user = :user")
    List<CartItem> findByUserWithRoom(@Param("user") User user);

    Optional<CartItem> findByUserAndRoom(User user, Room room);

    void deleteByUser(User user);

    void deleteByUserAndRoom(User user, Room room);
}
