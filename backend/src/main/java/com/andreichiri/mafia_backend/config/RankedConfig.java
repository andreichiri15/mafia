package com.andreichiri.mafia_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ranked-mode rules loaded from application.properties at startup.
 *
 * To change the config: edit {@code ranked.*} in {@code application.properties}
 * (or override via env vars / external config file) and restart the backend.
 * There is no runtime endpoint to change these — by design.
 */
@Component
@ConfigurationProperties(prefix = "ranked")
public class RankedConfig {
    // private Integer playerCount = 10;
    // private Integer mafiaCount = 2;
    // private boolean includeSheriff = true;
    // private boolean includeDoctor = true;
    // private boolean includeJester = true;
    // private boolean includeMutilator = true;
    // private Integer doctorSelfSaveLimit = 1;
    // private Integer sheriffInvestigationDelay = 0;
    // private Integer nightDurationSeconds = 45;
    // private Integer dayDurationSeconds = 120;
    // private Integer votingDurationSeconds = 45;
    private Integer playerCount = 4;
    private Integer mafiaCount = 1;
    private boolean includeSheriff = false;
    private boolean includeDoctor = false;
    private boolean includeJester = false;
    private boolean includeMutilator = false;
    private Integer doctorSelfSaveLimit = 1;
    private Integer sheriffInvestigationDelay = 0;
    private Integer nightDurationSeconds = 15;
    private Integer dayDurationSeconds = 20;
    private Integer votingDurationSeconds = 15;

    public Integer getPlayerCount() { return playerCount; }
    public void setPlayerCount(Integer playerCount) { this.playerCount = playerCount; }

    public Integer getMafiaCount() { return mafiaCount; }
    public void setMafiaCount(Integer mafiaCount) { this.mafiaCount = mafiaCount; }

    public boolean isIncludeSheriff() { return includeSheriff; }
    public void setIncludeSheriff(boolean includeSheriff) { this.includeSheriff = includeSheriff; }

    public boolean isIncludeDoctor() { return includeDoctor; }
    public void setIncludeDoctor(boolean includeDoctor) { this.includeDoctor = includeDoctor; }

    public boolean isIncludeJester() { return includeJester; }
    public void setIncludeJester(boolean includeJester) { this.includeJester = includeJester; }

    public boolean isIncludeMutilator() { return includeMutilator; }
    public void setIncludeMutilator(boolean includeMutilator) { this.includeMutilator = includeMutilator; }

    public Integer getDoctorSelfSaveLimit() { return doctorSelfSaveLimit; }
    public void setDoctorSelfSaveLimit(Integer doctorSelfSaveLimit) { this.doctorSelfSaveLimit = doctorSelfSaveLimit; }

    public Integer getSheriffInvestigationDelay() { return sheriffInvestigationDelay; }
    public void setSheriffInvestigationDelay(Integer sheriffInvestigationDelay) { this.sheriffInvestigationDelay = sheriffInvestigationDelay; }

    public Integer getNightDurationSeconds() { return nightDurationSeconds; }
    public void setNightDurationSeconds(Integer nightDurationSeconds) { this.nightDurationSeconds = nightDurationSeconds; }

    public Integer getDayDurationSeconds() { return dayDurationSeconds; }
    public void setDayDurationSeconds(Integer dayDurationSeconds) { this.dayDurationSeconds = dayDurationSeconds; }

    public Integer getVotingDurationSeconds() { return votingDurationSeconds; }
    public void setVotingDurationSeconds(Integer votingDurationSeconds) { this.votingDurationSeconds = votingDurationSeconds; }
}
