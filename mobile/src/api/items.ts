import apiClient from './client';
import {
  ItemCategory,
  ItemVariant,
  InventoryResponse,
  InventoryItem,
  PurchaseItemResponse,
  ItemType,
} from '../types';

export const itemApi = {
  listCategories: async (): Promise<ItemCategory[]> => {
    const res = await apiClient.get<ItemCategory[]>('/api/item-categories');
    return res.data;
  },

  createCategory: async (data: { name: string; type: ItemType; description?: string }): Promise<ItemCategory> => {
    const res = await apiClient.post<ItemCategory>('/api/item-categories', data);
    return res.data;
  },

  listVariants: async (params?: { itemCategoryId?: string; classId?: string }): Promise<ItemVariant[]> => {
    const res = await apiClient.get<ItemVariant[]>('/api/item-variants', { params });
    return res.data;
  },

  createVariant: async (variant: {
    itemCategoryId: string;
    label: string;
    classId?: string;
    size?: string;
    price: number;
    costPrice?: number;
    stockQuantity?: number;
  }): Promise<ItemVariant> => {
    const res = await apiClient.post<ItemVariant>('/api/item-variants', variant);
    return res.data;
  },

  updateVariant: async (
    id: string,
    updates: { label?: string; price?: number; costPrice?: number; isActive?: boolean }
  ): Promise<ItemVariant> => {
    const res = await apiClient.patch<ItemVariant>(`/api/item-variants/${id}`, updates);
    return res.data;
  },

  purchaseItem: async (
    studentId: string,
    data: { itemVariantId: string; quantity: number; dueDate?: string }
  ): Promise<PurchaseItemResponse> => {
    const res = await apiClient.post<PurchaseItemResponse>(`/api/students/${studentId}/purchases`, data);
    return res.data;
  },

  restock: async (variantId: string, quantity: number, note?: string): Promise<InventoryItem> => {
    const res = await apiClient.post<InventoryItem>(`/api/item-variants/${variantId}/restock`, { quantity, note });
    return res.data;
  },

  adjustStock: async (
    variantId: string,
    quantity: number,
    direction: 'increase' | 'decrease',
    note: string
  ): Promise<InventoryItem> => {
    const res = await apiClient.post<InventoryItem>(`/api/item-variants/${variantId}/adjust`, {
      quantity,
      direction,
      note,
    });
    return res.data;
  },

  listInventory: async (params?: { itemCategoryId?: string; lowStockBelow?: number }): Promise<InventoryResponse> => {
    const res = await apiClient.get<InventoryResponse>('/api/inventory', { params });
    return res.data;
  },
};
