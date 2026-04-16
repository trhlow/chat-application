package com.chatrealtime.mapper;

import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.domain.Message;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageResponse toResponse(Message message);
}


