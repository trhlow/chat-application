package com.chatrealtime.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.*;

@Data // auto sinh set get
@Builder // tạo object
@Document(collection = "users") // tạo tủ chứa users

public class User {
    @Id
    private String id;

    private String username;
    private String password;
    private String email;
    private String avatar;
    private boolean isOnline;
}
