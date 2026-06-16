package com.andreichiri.mafia_backend.service;

import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LiveKitService {

    @Value("${livekit.api-key:}")
    private String apiKey;

    @Value("${livekit.api-secret:}")
    private String apiSecret;

    @Value("${livekit.url:}")
    private String url;

    public String getUrl() {
        return url;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }

    /**
     * Mints a LiveKit JWT for the given participant + room.
     * Identity must be unique within the room — we use the userId.
     */
    public String generateToken(
            String identity,
            String displayName,
            String roomName,
            boolean canPublish,
            boolean canSubscribe
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("LiveKit is not configured (missing API key/secret)");
        }

        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        token.setName(displayName);

        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(canPublish),
                new CanSubscribe(canSubscribe)
        );

        return token.toJwt();
    }
}
