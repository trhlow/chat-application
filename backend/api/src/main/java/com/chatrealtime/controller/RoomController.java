package com.chatrealtime.controller;

import com.chatrealtime.dto.request.AddRoomMembersRequest;
import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.dto.response.MessagePageResponse;
import com.chatrealtime.dto.response.MessageResponse;
import com.chatrealtime.dto.response.RoomUnreadCountResponse;
import com.chatrealtime.dto.response.RoomResponse;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.RoomAvatarDownloadService;
import com.chatrealtime.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final MessageService messageService;
    private final AuthContextService authContextService;
    private final RoomAvatarDownloadService roomAvatarDownloadService;

    @GetMapping
    public List<RoomResponse> getRooms() {
        return roomService.getCurrentUserRooms();
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoom(@PathVariable String roomId) {
        return roomService.getRoomById(roomId);
    }

    @GetMapping("/{roomId}/messages")
    public MessagePageResponse getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) LocalDateTime before
    ) {
        return messageService.getMessagesByRoomId(roomId, limit == null ? size : limit, before);
    }

    @GetMapping("/{roomId}/messages/search")
    public MessagePageResponse searchRoomMessages(
            @PathVariable String roomId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return messageService.searchMessages(roomId, keyword, page, size);
    }

    @PutMapping("/{roomId}/read")
    public ResponseEntity<Void> markRoomAsRead(@PathVariable String roomId) {
        messageService.markRoomAsRead(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roomId}/unread-count")
    public RoomUnreadCountResponse getRoomUnreadCount(@PathVariable String roomId) {
        return messageService.getUnreadCount(roomId);
    }

    @PostMapping
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return roomService.createRoom(request);
    }

    @PostMapping(value = "/{roomId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageResponse createRoomMessageWithAttachment(
            @PathVariable String roomId,
            @RequestParam(required = false) String content,
            @RequestParam("file") MultipartFile file
    ) {
        return messageService.createMessageWithAttachment(roomId, content, file);
    }

    @PostMapping("/{roomId}/members")
    public RoomResponse addMembers(
            @PathVariable String roomId,
            @Valid @RequestBody AddRoomMembersRequest request
    ) {
        return roomService.addMembers(roomId, request);
    }

    @DeleteMapping("/{roomId}/members/{memberId}")
    public RoomResponse removeMember(@PathVariable String roomId, @PathVariable String memberId) {
        return roomService.removeMember(roomId, memberId);
    }

    @PatchMapping("/{roomId}/name")
    public RoomResponse updateRoomName(
            @PathVariable String roomId,
            @Valid @RequestBody UpdateRoomNameRequest request
    ) {
        return roomService.updateRoomName(roomId, request);
    }

    @GetMapping("/{roomId}/avatar")
    public ResponseEntity<?> getRoomAvatar(@PathVariable String roomId) {
        var principal = authContextService.requireCurrentUser();
        RoomAvatarDownloadService.RoomAvatarDownloadResult result =
                roomAvatarDownloadService.resolve(roomId, principal.getId());
        if (result instanceof RoomAvatarDownloadService.RoomAvatarDownloadResult.Redirect redirect) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(redirect.location())
                    .build();
        }
        if (result instanceof RoomAvatarDownloadService.RoomAvatarDownloadResult.File file) {
            return ResponseEntity.ok()
                    .contentType(file.mediaType())
                    .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                    .body(file.body());
        }
        throw new IllegalStateException("Unexpected room avatar result");
    }

    @PostMapping(value = "/{roomId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoomResponse updateRoomAvatar(
            @PathVariable String roomId,
            @RequestParam("file") MultipartFile file
    ) {
        return roomService.updateRoomAvatar(roomId, file);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String roomId) {
        roomService.leaveRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> dissolveRoom(@PathVariable String roomId) {
        roomService.dissolveRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}



