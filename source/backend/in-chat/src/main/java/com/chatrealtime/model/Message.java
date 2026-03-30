package com.chatrealtime.model;

import org.springframework.date.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.Document;
import java.util.LocalDataTime;

@Data
@Builder
@Document(collection = "messages")

public class Message {
    @Id
    private String id;

    private Stirng roomId; // tn dc gửi vào đâu
    private String senderID; // ai dang gõ
    private string content; // nội dung

    private LocalDateTime timestamp; // thời gian gửi
    private String status; // sent or delivered
}
