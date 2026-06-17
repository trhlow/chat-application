package com.chatrealtime.web;

import com.chatrealtime.dto.request.CreateRoomRequest;
import com.chatrealtime.dto.request.UpdateRoomNameRequest;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.service.MessageService;
import com.chatrealtime.service.RoomAvatarDownloadService;
import com.chatrealtime.service.RoomService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
class RoomControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private AuthContextService authContextService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserPrincipalService userPrincipalService;

    @MockitoBean
    private RoomAvatarDownloadService roomAvatarDownloadService;

    @Test
    void getRoom_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/rooms/r1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRoom_notMember_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(roomService.getRoomById("r1")).thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/api/rooms/r1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoom_missingType_returns400() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);

        CreateRoomRequest req = new CreateRoomRequest("group_name", null, List.of("u2", "u3"));
        mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoom_invalidBody_returns400() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);

        mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty body, missing required fields
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeMember_notAdmin_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(roomService.removeMember(eq("r1"), eq("u2"))).thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(delete("/api/rooms/r1/members/u2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRoomName_notAdmin_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(roomService.updateRoomName(eq("r1"), any())).thenThrow(new AccessDeniedException("Forbidden"));

        UpdateRoomNameRequest req = new UpdateRoomNameRequest("new name");
        mockMvc.perform(patch("/api/rooms/r1/name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
