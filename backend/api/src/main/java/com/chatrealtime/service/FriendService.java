package com.chatrealtime.service;

import com.chatrealtime.dto.request.CreateFriendRequestRequest;
import com.chatrealtime.dto.response.FriendRequestResponse;
import com.chatrealtime.dto.response.FriendshipResponse;

import java.util.List;

public interface FriendService {
    FriendRequestResponse sendFriendRequest(CreateFriendRequestRequest request);

    List<FriendRequestResponse> getIncomingRequests();

    List<FriendRequestResponse> getOutgoingRequests();

    FriendRequestResponse acceptRequest(String requestId);

    FriendRequestResponse rejectRequest(String requestId);

    FriendRequestResponse cancelRequest(String requestId);

    List<FriendshipResponse> getFriends();

    void removeFriend(String friendId);
}
