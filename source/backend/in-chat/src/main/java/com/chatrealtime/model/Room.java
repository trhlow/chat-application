package com.chatrealtime.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
@Document(collection = "rooms")

public class Room {
    @Id
    private String id;

    private String name; //chat 1-1 thì không cần name
    private String type; //chat 1-1 hay chat nhóm

    // nhận dạng user
    private List<String> memberIs;
}
