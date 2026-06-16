package com.andreichiri.mafia_backend.service;

import com.andreichiri.mafia_backend.config.RankedConfig;
import com.andreichiri.mafia_backend.dto.RankedDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper around the {@link RankedConfig} properties bean. Exposes both
 * the raw config (for internal services like MatchmakingService) and a DTO
 * shape for the frontend.
 */
@Service
public class RankedConfigService {

    @Autowired
    private RankedConfig rankedConfig;

    public RankedConfig getConfig() {
        return rankedConfig;
    }

    public RankedDTO.RankedConfigDTO getConfigDTO() {
        RankedConfig c = rankedConfig;
        return new RankedDTO.RankedConfigDTO(
                c.getPlayerCount(),
                c.getMafiaCount(),
                c.isIncludeSheriff(),
                c.isIncludeDoctor(),
                c.isIncludeJester(),
                c.isIncludeMutilator(),
                c.getDoctorSelfSaveLimit(),
                c.getSheriffInvestigationDelay(),
                c.getNightDurationSeconds(),
                c.getDayDurationSeconds(),
                c.getVotingDurationSeconds()
        );
    }
}
