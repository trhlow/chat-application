package com.chatrealtime.web;

import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = true)
class MessageControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private AuthContextService authContextService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserPrincipalService userPrincipalService;

    @Test
    void getMessages_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/messages?roomId=r1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMessages_missingRoomId_returns400() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);

        mockMvc.perform(get("/api/messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMessages_notMember_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        when(messageService.getMessagesByRoomId(eq("r1"), any(), any())).thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/api/messages?roomId=r1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMessage_noAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/messages/m1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMessage_otherUser_returns403() throws Exception {
        AuthUserPrincipal principal = new AuthUserPrincipal("u1", "user1", "pw", 0);
        when(userPrincipalService.loadByUserId("u1")).thenReturn(principal);
        when(authContextService.requireCurrentUser()).thenReturn(principal);
        doThrow(new AccessDeniedException("Forbidden")).when(messageService).deleteMessageForCurrentUser("m1");

        mockMvc.perform(delete("/api/messages/m1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtTokenService.generateToken(principal)))
                .andExpect(status().isForbidden());
    }
}
