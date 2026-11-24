import apiClient, { setClientToken } from './client';
import { LoginResponse, User } from '../types';

export const authApi = {
  login: async (username: string, password: string): Promise<LoginResponse> => {
    const res = await apiClient.post<LoginResponse>('/api/auth/login', { username, password });
    if (res.data.token) {
      setClientToken(res.data.token);
    }
    return res.data;
  },

  getMe: async (): Promise<User> => {
    const res = await apiClient.get<User>('/api/auth/me');
    return res.data;
  },
};
