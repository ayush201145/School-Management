import apiClient from './client';
import { InvoiceSettings } from '../types';

export const settingsApi = {
  getInvoiceSettings: async (): Promise<InvoiceSettings> => {
    const res = await apiClient.get<InvoiceSettings>('/api/invoice-settings');
    return res.data;
  },

  updateInvoiceSettings: async (settings: Partial<InvoiceSettings>): Promise<InvoiceSettings> => {
    const res = await apiClient.patch<InvoiceSettings>('/api/invoice-settings', settings);
    return res.data;
  },
};
