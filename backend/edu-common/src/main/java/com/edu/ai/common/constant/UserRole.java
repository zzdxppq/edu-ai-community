package com.edu.ai.common.constant;

/**
 * User role (AC1 BR-1.2). 6 roles exposed to the registration UI.
 * The {@code displayName} is the single source of truth for Chinese labels —
 * consumed by the frontend RoleSelector cards and any future backend rendering.
 */
public enum UserRole {

    REGION_ADMIN("区域教育管理人员"),
    CONSORTIUM_LEAD("校共体核心牵头校人员"),
    MEMBER_URBAN("校共体成员校人员（城镇）"),
    MEMBER_RURAL("校共体成员校人员（乡村）"),
    RESEARCHER("研究人员"),
    OTHER("其他人员");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
