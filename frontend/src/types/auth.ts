export type UserRole =
  | 'REGION_ADMIN'
  | 'CONSORTIUM_LEAD'
  | 'MEMBER_URBAN'
  | 'MEMBER_RURAL'
  | 'RESEARCHER'
  | 'OTHER';

export interface RoleOption {
  value: UserRole;
  label: string;
  description: string;
}

/**
 * Single source of truth for the 6 registration role cards (AC2 BR-1.2).
 * Mirrors `com.edu.ai.common.constant.UserRole#displayName` on the backend —
 * keep them aligned whenever either side changes.
 */
export const ROLE_OPTIONS: RoleOption[] = [
  {
    value: 'REGION_ADMIN',
    label: '区域教育管理人员',
    description: '负责区域教育统筹与政策落地',
  },
  {
    value: 'CONSORTIUM_LEAD',
    label: '校共体核心牵头校人员',
    description: '承担校共体组织协调与引领',
  },
  {
    value: 'MEMBER_URBAN',
    label: '校共体成员校人员（城镇）',
    description: '城镇学校教师与管理人员',
  },
  {
    value: 'MEMBER_RURAL',
    label: '校共体成员校人员（乡村）',
    description: '乡村学校教师与管理人员',
  },
  {
    value: 'RESEARCHER',
    label: '研究人员',
    description: '关注区域教育研究与数据分析',
  },
  {
    value: 'OTHER',
    label: '其他人员',
    description: '其他与平台相关的参与者',
  },
];

export interface SendSmsRequest {
  phone: string;
}

export interface RegisterRequest {
  phone: string;
  smsCode: string;
  username: string;
  role: UserRole;
}

export interface UserDto {
  id: string;
  phone: string;
  username: string;
  role: UserRole;
  userType: 'USER' | 'ADMIN';
  createdAt: string;
}

export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T | null;
}
