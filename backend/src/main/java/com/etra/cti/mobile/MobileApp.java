package com.etra.cti.mobile;

import com.etra.cti.common.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mobile_apps")
public class MobileApp extends BaseEntity {

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MobilePlatform platform;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "version_name", length = 100)
    private String versionName;

    @Column(name = "version_code", length = 50)
    private String versionCode;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    private Boolean signed;

    @Column(name = "min_sdk", length = 20)
    private String minSdk;

    @Column(name = "target_sdk", length = 20)
    private String targetSdk;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> permissions;

    @Column(name = "created_by")
    private UUID createdBy;

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public MobilePlatform getPlatform() { return platform; }
    public void setPlatform(MobilePlatform platform) { this.platform = platform; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public String getVersionCode() { return versionCode; }
    public void setVersionCode(String versionCode) { this.versionCode = versionCode; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public Boolean getSigned() { return signed; }
    public void setSigned(Boolean signed) { this.signed = signed; }

    public String getMinSdk() { return minSdk; }
    public void setMinSdk(String minSdk) { this.minSdk = minSdk; }

    public String getTargetSdk() { return targetSdk; }
    public void setTargetSdk(String targetSdk) { this.targetSdk = targetSdk; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
