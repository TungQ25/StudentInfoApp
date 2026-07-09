package com.example.taskmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "habit_completions")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HabitCompletion {

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @NotBlank
    @Column(name = "habit_id", nullable = false, length = 36)
    private String habitId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @NotBlank
    @Column(name = "period_key", nullable = false, length = 32)
    private String periodKey;

    @Column(name = "completed_at", nullable = false)
    private long completedAt = 0L;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt = 0L;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Long deletedAt;

    @Column(name = "version", nullable = false)
    private long version = 1L;

    @Column(name = "last_modified_device_id", length = 80)
    private String lastModifiedDeviceId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getHabitId() { return habitId; }
    public void setHabitId(String habitId) { this.habitId = habitId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(String periodKey) { this.periodKey = periodKey; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Long deletedAt) { this.deletedAt = deletedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getLastModifiedDeviceId() { return lastModifiedDeviceId; }
    public void setLastModifiedDeviceId(String lastModifiedDeviceId) { this.lastModifiedDeviceId = lastModifiedDeviceId; }
}
