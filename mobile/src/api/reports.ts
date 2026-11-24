import apiClient from './client';
import { MonthlyReport } from '../types';

export const reportApi = {
  getMonthlyReport: async (month: number, year: number): Promise<MonthlyReport> => {
    const res = await apiClient.get<MonthlyReport>('/api/reports/monthly', {
      params: { month, year },
    });
    return res.data;
  },
};
