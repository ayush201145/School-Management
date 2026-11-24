import apiClient from './client';
import {
  FeeCategory,
  FeeStructure,
  StudentFee,
  Payment,
  PaymentMode,
  DuesResponse,
  TransactionsResponse,
} from '../types';

export const feeApi = {
  listCategories: async (): Promise<FeeCategory[]> => {
    const res = await apiClient.get<FeeCategory[]>('/api/fee-categories');
    return res.data;
  },

  createCategory: async (category: { name: string; description?: string }): Promise<FeeCategory> => {
    const res = await apiClient.post<FeeCategory>('/api/fee-categories', category);
    return res.data;
  },

  listStructures: async (params?: { classId?: string; academicYearId?: string }): Promise<FeeStructure[]> => {
    const res = await apiClient.get<FeeStructure[]>('/api/fee-structures', { params });
    return res.data;
  },

  createStructure: async (data: {
    feeCategoryId: string;
    classId: string;
    academicYearId: string;
    amount: number;
    dueDate: string;
    description?: string;
  }): Promise<FeeStructure> => {
    const res = await apiClient.post<FeeStructure>('/api/fee-structures', data);
    return res.data;
  },

  assignStructure: async (
    structureId: string,
    sectionId?: string
  ): Promise<{ created: number; skipped: number; totalStudentsInScope: number; message?: string }> => {
    const res = await apiClient.post(`/api/fee-structures/${structureId}/assign`, { sectionId });
    return res.data;
  },

  listPaymentsForFee: async (studentFeeId: string): Promise<Payment[]> => {
    const res = await apiClient.get<Payment[]>(`/api/student-fees/${studentFeeId}/payments`);
    return res.data;
  },

  recordPayment: async (
    studentFeeId: string,
    data: {
      amount: number;
      mode: PaymentMode;
      referenceNo?: string;
      notes?: string;
      paidAt?: string;
    }
  ): Promise<{ payment: Payment; studentFee: StudentFee }> => {
    const res = await apiClient.post(`/api/student-fees/${studentFeeId}/payments`, data);
    return res.data;
  },

  listTransactions: async (params?: {
    from?: string;
    to?: string;
    studentId?: string;
    mode?: string;
  }): Promise<TransactionsResponse> => {
    const res = await apiClient.get<TransactionsResponse>('/api/transactions', { params });
    return res.data;
  },

  listDues: async (params?: {
    sectionId?: string;
    classId?: string;
    overdueOnly?: boolean;
  }): Promise<DuesResponse> => {
    const res = await apiClient.get<DuesResponse>('/api/dues', { params });
    return res.data;
  },
};
