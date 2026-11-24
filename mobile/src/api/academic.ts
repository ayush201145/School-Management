import apiClient from './client';
import { AcademicYear, SchoolClass, Section } from '../types';

export const academicApi = {
  listYears: async (): Promise<AcademicYear[]> => {
    const res = await apiClient.get<AcademicYear[]>('/api/academic-years');
    return res.data;
  },

  listClasses: async (params?: { academicYearId?: string }): Promise<SchoolClass[]> => {
    const res = await apiClient.get<SchoolClass[]>('/api/classes', { params });
    return res.data;
  },

  listSections: async (params?: { classId?: string }): Promise<Section[]> => {
    const res = await apiClient.get<Section[]>('/api/sections', { params });
    return res.data;
  },
};
