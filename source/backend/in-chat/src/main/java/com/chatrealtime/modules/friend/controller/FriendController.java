package com.chatrealtime.modules.friend.controller;

import com.chatrealtime.modules.friend.dto.CreateFriendRequestRequest;
import com.chatrealtime.modules.friend.dto.response.FriendRequestResponse;
import com.chatrealtime.modules.friend.dto.response.FriendshipResponse;
import com.chatrealtime.modules.friend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    @PostMapping("/requests")
    public FriendRequestResponse sendFriendRequest(@Valid @RequestBody CreateFriendRequestRequest request) {
        return friendService.sendFriendRequest(request);
    }

    @GetMapping("/requests/incoming")
    public List<FriendRequestResponse> getIncomingRequests() {
        return friendService.getIncomingRequests();
    }

    @GetMapping("/requests/outgoing")
    public List<FriendRequestResponse> getOutgoingRequests() {
        return friendService.getOutgoingRequests();
    }

    @PostMapping("/requests/{id}/accept")
    public FriendRequestResponse acceptRequest(@PathVariable String id) {
        return friendService.acceptRequest(id);
    }

    @PostMapping("/requests/{id}/reject")
    public FriendRequestResponse rejectRequest(@PathVariable String id) {
        return friendService.rejectRequest(id);
    }

    @DeleteMapping("/requests/{id}")
    public FriendRequestResponse cancelRequest(@PathVariable String id) {
        return friendService.cancelRequest(id);
    }

    @GetMapping
    public List<FriendshipResponse> getFriends() {
        return friendService.getFriends();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable String friendId) {
        friendService.removeFriend(friendId);
        return ResponseEntity.noContent().build();
    }
}
