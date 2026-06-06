package com.auraia.backend.models.dto.response;

import com.auraia.backend.models.enums.AchievementCode;
import java.time.Instant;
import java.util.List;

public final class AchievementResponses {

    private AchievementResponses() {
    }

    public static class AchievementListResponse {
        private int total;
        private int unlocked;
        private List<AchievementResponse> achievements;

        public AchievementListResponse() {
        }

        public AchievementListResponse(int total, int unlocked, List<AchievementResponse> achievements) {
            this.total = total;
            this.unlocked = unlocked;
            this.achievements = achievements;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public int getUnlocked() {
            return unlocked;
        }

        public void setUnlocked(int unlocked) {
            this.unlocked = unlocked;
        }

        public List<AchievementResponse> getAchievements() {
            return achievements;
        }

        public void setAchievements(List<AchievementResponse> achievements) {
            this.achievements = achievements;
        }
    }

    public static class AchievementResponse {
        private AchievementCode code;
        private String title;
        private String description;
        private String category;
        private String accent;
        private int progress;
        private int target;
        private boolean unlocked;
        private Instant unlockedAt;
        private String progressLabel;

        public AchievementResponse() {
        }

        public AchievementResponse(AchievementCode code, String title, String description,
                                   String category, String accent, int progress, int target,
                                   boolean unlocked, Instant unlockedAt, String progressLabel) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.category = category;
            this.accent = accent;
            this.progress = progress;
            this.target = target;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
            this.progressLabel = progressLabel;
        }

        public AchievementCode getCode() {
            return code;
        }

        public void setCode(AchievementCode code) {
            this.code = code;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getAccent() {
            return accent;
        }

        public void setAccent(String accent) {
            this.accent = accent;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public int getTarget() {
            return target;
        }

        public void setTarget(int target) {
            this.target = target;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public void setUnlocked(boolean unlocked) {
            this.unlocked = unlocked;
        }

        public Instant getUnlockedAt() {
            return unlockedAt;
        }

        public void setUnlockedAt(Instant unlockedAt) {
            this.unlockedAt = unlockedAt;
        }

        public String getProgressLabel() {
            return progressLabel;
        }

        public void setProgressLabel(String progressLabel) {
            this.progressLabel = progressLabel;
        }
    }
}
