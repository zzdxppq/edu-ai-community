import { defineStore } from 'pinia';

/**
 * Auth store skeleton for Story 1.1. Only initial state is defined here —
 * login / logout / token refresh are added by Story 1.3.
 */
export interface AuthUser {
  id: string;
  phone: string;
  role: 'student' | 'teacher' | 'parent' | 'admin';
}

export interface AuthState {
  user: AuthUser | null;
  accessToken: string;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    accessToken: '',
  }),
});
