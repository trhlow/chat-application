package com.chatrealtime.web;

import com.chatrealtime.controller.MessageAttachmentController;
import com.chatrealtime.exception.GlobalExceptionHandler;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.security.AuthContextService;
import com.chatrealtime.security.AuthUserPrincipal;
import com.chatrealtime.security.JwtTokenService;
import com.chatrealtime.security.UserPrincipalService;
import com.chatrealtime.service.AttachmentDeliveryResult;
import com.chatrealtime.service.MessageAttachmentDownloadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for controller wiring and HTTP mapping. Authorization rules for attachment download are covered by
 * {@link com.chatrealtime.integration.PrivacySecurityIntegrationTest} against real repositories and JWT.
 */
@WebMvcTest(controllers = MessageAttachmentController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageAttachmentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthContextService authContextService;

    @MockitoBean
    private MessageAttachmentDownloadService messageAttachmentDownloadService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserPrincipalService userPrincipalService;

    @Test
    void download_withoutAuthentication_returns401() throws Exception {
        when(authContextService.requireCurrentUser()).thenThrow(new InvalidCredentialsException("Unauthorized"));

        mockMvc.perform(get("/api/messages/m1/attachments/a1/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void download_whenServiceDenies_returns403() throws Exception {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        when(messageAttachmentDownloadService.resolveDelivery(any(), eq("m1"), eq("a1"), isNull()))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get("/api/messages/m1/attachments/a1/download"))
                .andExpect(status().isForbidden());
    }

    @Test
    void download_whenAuthorized_returns200() throws Exception {
        when(authContextService.requireCurrentUser()).thenReturn(new AuthUserPrincipal("u1", "alice", "pw", 0));
        ByteArrayResource body = new ByteArrayResource("hello".getBytes());
        when(messageAttachmentDownloadService.resolveDelivery(any(), eq("m1"), eq("a1"), isNull()))
                .thenReturn(new AttachmentDeliveryResult(
                        AttachmentDeliveryResult.Type.LOCAL_RESOURCE,
                        body,
                        null,
                        MediaType.TEXT_PLAIN,
                        "note.txt"
                ));

        mockMvc.perform(get("/api/messages/m1/attachments/a1/download"))
                .andExpect(status().isOk());
    }
}
