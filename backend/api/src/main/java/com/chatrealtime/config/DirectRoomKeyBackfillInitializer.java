package com.chatrealtime.config;

import com.chatrealtime.domain.Room;
import com.chatrealtime.util.UserIdPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DirectRoomKeyBackfillInitializer implements ApplicationRunner {

    private static final String ROOM_TYPE_DIRECT = "direct";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Room> directRooms = mongoTemplate.find(
                Query.query(Criteria.where("type").is(ROOM_TYPE_DIRECT)),
                Room.class
        );
        Map<String, List<Room>> roomsByExpectedKey = new HashMap<>();

        for (Room room : directRooms) {
            String expectedKey = expectedDirectKey(room);
            if (hasText(room.getDirectKey()) && !room.getDirectKey().equals(expectedKey)) {
                throw new IllegalStateException("Direct room has a directKey that does not match its members: " + room.getId());
            }
            roomsByExpectedKey.computeIfAbsent(expectedKey, ignored -> new ArrayList<>()).add(room);
        }

        roomsByExpectedKey.values().stream()
                .filter(rooms -> rooms.size() > 1)
                .findFirst()
                .ifPresent(duplicates -> {
                    throw new IllegalStateException(
                            "Duplicate direct rooms exist for the same member pair; resolve before startup: "
                                    + duplicates.stream().map(Room::getId).toList()
                    );
                });

        int backfilled = 0;
        for (Room room : directRooms) {
            if (!hasText(room.getDirectKey())) {
                room.setDirectKey(expectedDirectKey(room));
                mongoTemplate.save(room);
                backfilled++;
            }
        }
        if (backfilled > 0) {
            log.info("Backfilled directKey for {} direct rooms", backfilled);
        }
    }

    private String expectedDirectKey(Room room) {
        List<String> memberIds = room.getMemberIds() == null
                ? List.of()
                : room.getMemberIds().stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (memberIds.size() != 2) {
            throw new IllegalStateException("Direct room must have exactly 2 members before directKey backfill: " + room.getId());
        }
        UserIdPair.Ordered pair = UserIdPair.order(memberIds.get(0), memberIds.get(1));
        return pair.userIdA() + ":" + pair.userIdB();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
