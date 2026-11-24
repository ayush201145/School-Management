import apiClient from './client';
import { Teacher, CreateTeacherInput } from '../types';

export const teacherApi = {
  list: async (params?: { search?: string; isActive?: boolean }): Promise<Teacher[]> => {
    const res = await apiClient.get<Teacher[]>('/api/teachers', { params });
    return res.data;
  },

  getById: async (id: string): Promise<Teacher> => {
    const res = await apiClient.get<Teacher>(`/api/teachers/${id}`);
    return res.data;
  },

  create: async (data: CreateTeacherInput): Promise<Teacher> => {
    const res = await apiClient.post<Teacher>('/api/teachers', data);
    return res.data;
  },

  update: async (id: string, data: Partial<CreateTeacherInput> & { isActive?: boolean }): Promise<Teacher> => {
    const res = await apiClient.patch<Teacher>(`/api/teachers/${id}`, data);
    return res.data;
  },
};
