package com.chatrealtime.web;

import com.chatrealtime.dto.request.CreateFriendRequestRequest;
import com.chatrealtime.exception.BadRequestException;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.service.FriendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
class FriendControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FriendService friendService;

    @MockitoBean
    private AuthContextService authContextService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserPrincipalService userPrincipalService;

    @Test
    void sendFriendRequest_noAuth_returns401() throws Exception {
        CreateFriendRequestRequest req = new CreateFriendRequestRequest("u2");
        mockMvc.perform(post("/api/friends/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFriends_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/friends"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendFriendRequest_toSelf_returns400() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(friendService.sendFriendRequest(any())).thenThrow(new BadRequestException("Cannot send a friend request to yourself"));

        CreateFriendRequestRequest req = new CreateFriendRequestRequest("u1");
        mockMvc.perform(post("/api/friends/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptRequest_notReceiver_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(friendService.acceptRequest(eq("req1"))).thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(post("/api/friends/requests/req1/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelRequest_notSenderOrReceiver_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(friendService.cancelRequest(eq("req1"))).thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(delete("/api/friends/requests/req1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isForbidden());
    }
}
