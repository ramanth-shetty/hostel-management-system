package com.hostel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hostel.entity.Room;

import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, String> {

    /** Row-locks the room so two simultaneous registrations cannot both take the last bed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.roomNumber = :roomNumber")
    Optional<Room> findForUpdate(@Param("roomNumber") String roomNumber);

    List<Room> findAllByOrderByRoomNumberAsc();
}
