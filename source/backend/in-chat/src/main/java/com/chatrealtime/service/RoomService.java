package com.chatrealtime.service;

import com.chatrealtime.model.Room;
import com.chatrealtime.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

    public List<Room> getRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getRoomsByUserId(String userId) {
        return roomRepository.findByMemberIdsContaining(userId);
    }

    public Room createRoom(Room room) {
        return roomRepository.save(room);
    }
}

