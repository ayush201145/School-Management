import { create } from 'zustand';
import * as SecureStore from 'expo-secure-store';
import { User, UserRole } from '../types';
import { authApi } from '../api/auth';
import { setClientToken } from '../api/client';

interface AuthState {
  token: string | null;
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  role: UserRole | null;
  error: string | null;
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,
  isLoading: true,
  isAuthenticated: false,
  role: null,
  error: null,

  login: async (username: string, password: string): Promise<boolean> => {
    try {
      set({ isLoading: true, error: null });
      const res = await authApi.login(username, password);
      await SecureStore.setItemAsync('auth_token', res.token);
      await SecureStore.setItemAsync('user_data', JSON.stringify(res.user));
      setClientToken(res.token);
      set({
        token: res.token,
        user: res.user,
        role: res.user.role,
        isAuthenticated: true,
        isLoading: false,
      });
      return true;
    } catch (err: any) {
      set({
        error: err.message || 'Login failed',
        isLoading: false,
      });
      return false;
    }
  },

  logout: async () => {
    try {
      await SecureStore.deleteItemAsync('auth_token');
      await SecureStore.deleteItemAsync('user_data');
    } catch {
      // ignore
    }
    setClientToken(null);
    set({
      token: null,
      user: null,
      role: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,
    });
  },

  checkAuth: async () => {
    try {
      set({ isLoading: true });
      const storedToken = await SecureStore.getItemAsync('auth_token');
      const storedUser = await SecureStore.getItemAsync('user_data');

      if (storedToken && storedUser) {
        setClientToken(storedToken);
        const parsedUser = JSON.parse(storedUser);
        set({
          token: storedToken,
          user: parsedUser,
          role: parsedUser.role,
          isAuthenticated: true,
          isLoading: false,
        });

        // Background refresh user profile
        authApi.getMe().then((me) => {
          set({ user: me, role: me.role });
          SecureStore.setItemAsync('user_data', JSON.stringify(me));
        }).catch(() => {
          // keep cached user
        });
      } else {
        set({
          token: null,
          user: null,
          role: null,
          isAuthenticated: false,
          isLoading: false,
        });
      }
    } catch {
      set({
        token: null,
        user: null,
        role: null,
        isAuthenticated: false,
        isLoading: false,
      });
    }
  },
}));
