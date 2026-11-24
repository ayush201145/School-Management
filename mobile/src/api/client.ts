import axios from 'axios';
import * as SecureStore from 'expo-secure-store';

const BASE_URL = 'https://school-management-yvcj.onrender.com';

const apiClient = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

let authToken: string | null = null;

export const setClientToken = (token: string | null) => {
  authToken = token;
  if (token) {
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  } else {
    delete apiClient.defaults.headers.common['Authorization'];
  }
};

apiClient.interceptors.request.use(async (config) => {
  if (!authToken) {
    try {
      const stored = await SecureStore.getItemAsync('auth_token');
      if (stored) {
        authToken = stored;
        config.headers.Authorization = `Bearer ${stored}`;
      }
    } catch {
      // ignore storage read error
    }
  } else {
    config.headers.Authorization = `Bearer ${authToken}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.error ||
      error.response?.data?.message ||
      error.message ||
      'An unexpected error occurred';
    return Promise.reject(new Error(message));
  }
);

export default apiClient;
