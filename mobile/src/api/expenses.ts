import apiClient from './client';
import {
  Expense,
  ExpenseCategory,
  ExpensesResponse,
  PaymentMode,
  RecurringExpenseTemplate,
} from '../types';

export const expenseApi = {
  listCategories: async (): Promise<ExpenseCategory[]> => {
    const res = await apiClient.get<ExpenseCategory[]>('/api/expense-categories');
    return res.data;
  },

  createCategory: async (data: { name: string; description?: string }): Promise<ExpenseCategory> => {
    const res = await apiClient.post<ExpenseCategory>('/api/expense-categories', data);
    return res.data;
  },

  listExpenses: async (params?: {
    from?: string;
    to?: string;
    expenseCategoryId?: string;
  }): Promise<ExpensesResponse> => {
    const res = await apiClient.get<ExpensesResponse>('/api/expenses', { params });
    return res.data;
  },

  createExpense: async (data: {
    expenseCategoryId: string;
    description: string;
    amount: number;
    mode: PaymentMode;
    referenceNo?: string;
    spentAt?: string;
  }): Promise<Expense> => {
    const res = await apiClient.post<Expense>('/api/expenses', data);
    return res.data;
  },

  listRecurringTemplates: async (): Promise<RecurringExpenseTemplate[]> => {
    const res = await apiClient.get<RecurringExpenseTemplate[]>('/api/recurring-expenses');
    return res.data;
  },

  createRecurringTemplate: async (data: {
    expenseCategoryId: string;
    name: string;
    amount: number;
    dayOfMonth: number;
  }): Promise<RecurringExpenseTemplate> => {
    const res = await apiClient.post<RecurringExpenseTemplate>('/api/recurring-expenses', data);
    return res.data;
  },

  generateRecurringExpense: async (templateId: string): Promise<Expense> => {
    const res = await apiClient.post<Expense>(`/api/recurring-expenses/${templateId}/generate`);
    return res.data;
  },
};
