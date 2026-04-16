package com.chatrealtime.repository;

import com.chatrealtime.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findByMemberIdsContaining(String userId);
}



