import apiClient from './client';
import { Staff, SalaryPayment, StaffType, PaymentMode } from '../types';

export const staffApi = {
  list: async (params?: { type?: StaffType; isActive?: boolean }): Promise<Staff[]> => {
    const res = await apiClient.get<Staff[]>('/api/staff', { params });
    return res.data;
  },

  getById: async (id: string): Promise<Staff> => {
    const res = await apiClient.get<Staff>(`/api/staff/${id}`);
    return res.data;
  },

  create: async (data: {
    name: string;
    type: StaffType;
    teacherId?: string;
    phone?: string;
    monthlySalary?: number;
  }): Promise<Staff> => {
    const res = await apiClient.post<Staff>('/api/staff', data);
    return res.data;
  },

  update: async (
    id: string,
    data: { name?: string; type?: StaffType; phone?: string; monthlySalary?: number; isActive?: boolean }
  ): Promise<Staff> => {
    const res = await apiClient.patch<Staff>(`/api/staff/${id}`, data);
    return res.data;
  },

  recordSalaryPayment: async (
    staffId: string,
    data: {
      amount: number;
      forMonth?: number;
      forYear?: number;
      month?: number;
      year?: number;
      mode: PaymentMode;
      notes?: string;
    }
  ): Promise<SalaryPayment> => {
    const forMonth = data.forMonth ?? data.month;
    const forYear = data.forYear ?? data.year;
    const payload = {
      ...data,
      forMonth,
      forYear,
      month: forMonth,
      year: forYear,
    };
    const res = await apiClient.post<SalaryPayment>(`/api/staff/${staffId}/salary-payments`, payload);
    return res.data;
  },

  listSalaryPayments: async (staffId: string): Promise<SalaryPayment[]> => {
    const res = await apiClient.get<SalaryPayment[]>(`/api/staff/${staffId}/salary-payments`);
    return res.data;
  },
};
