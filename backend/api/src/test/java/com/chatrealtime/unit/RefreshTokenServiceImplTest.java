package com.chatrealtime.unit;

import com.chatrealtime.domain.RefreshToken;
import com.chatrealtime.exception.InvalidCredentialsException;
import com.chatrealtime.repository.RefreshTokenRepository;
import com.chatrealtime.security.JwtProperties;
import com.chatrealtime.service.RefreshRotationResult;
import com.chatrealtime.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    void rotateRefreshToken_whenTokenActive_revokesAtomicallyAndPersistsReplacement() {
        when(jwtProperties.refreshExpirationMs()).thenReturn(604800000L);
        RefreshToken revokedDoc = RefreshToken.builder()
                .id("rt1")
                .userId("user-a")
                .tokenHash("hashed-old")
                .build();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RefreshToken.class)))
                .thenReturn(revokedDoc);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshRotationResult result = refreshTokenService.rotateRefreshToken("presented-raw-token");

        assertThat(result.userId()).isEqualTo("user-a");
        assertThat(result.newRefreshToken()).isNotBlank();

        ArgumentCaptor<RefreshToken> newTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(newTokenCaptor.capture());
        assertThat(newTokenCaptor.getValue().getUserId()).isEqualTo("user-a");
        assertThat(newTokenCaptor.getValue().getTokenHash()).isNotBlank();
    }

    @Test
    void rotateRefreshToken_whenAlreadyRevokedOrMissing_throwsAndDoesNotInsertNew() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RefreshToken.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("stale-token"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_whenBlank_throws() {
        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("  "))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(RefreshToken.class));
    }
}
