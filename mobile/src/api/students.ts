import apiClient from './client';
import { Student, CreateStudentInput, UpdateStudentInput, WithdrawalReason } from '../types';

export const studentApi = {
  list: async (params?: { search?: string; sectionId?: string; isActive?: boolean }): Promise<Student[]> => {
    const res = await apiClient.get<Student[]>('/api/students', { params });
    return res.data;
  },

  getById: async (id: string): Promise<Student> => {
    const res = await apiClient.get<Student>(`/api/students/${id}`);
    return res.data;
  },

  create: async (student: CreateStudentInput): Promise<Student> => {
    const res = await apiClient.post<Student>('/api/students', student);
    return res.data;
  },

  update: async (id: string, updates: UpdateStudentInput): Promise<Student> => {
    const res = await apiClient.patch<Student>(`/api/students/${id}`, updates);
    return res.data;
  },

  withdraw: async (id: string, reason: WithdrawalReason, notes?: string, withdrawnDate?: string): Promise<Student> => {
    const res = await apiClient.post<Student>(`/api/students/${id}/withdraw`, {
      reason,
      notes,
      withdrawnDate,
    });
    return res.data;
  },

  reinstate: async (id: string): Promise<Student> => {
    const res = await apiClient.post<Student>(`/api/students/${id}/reinstate`);
    return res.data;
  },
};
