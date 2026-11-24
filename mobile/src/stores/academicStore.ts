import { create } from 'zustand';
import { AcademicYear, SchoolClass, Section } from '../types';
import { academicApi } from '../api/academic';

interface AcademicState {
  academicYears: AcademicYear[];
  selectedYearId: string | null;
  classes: SchoolClass[];
  sections: Section[];
  isLoading: boolean;
  error: string | null;
  fetchAcademicYears: () => Promise<void>;
  selectYear: (id: string | null) => void;
  fetchClasses: (academicYearId?: string) => Promise<void>;
  fetchSections: (classId?: string) => Promise<void>;
}

export const useAcademicStore = create<AcademicState>((set, get) => ({
  academicYears: [],
  selectedYearId: null,
  classes: [],
  sections: [],
  isLoading: false,
  error: null,

  fetchAcademicYears: async () => {
    try {
      set({ isLoading: true, error: null });
      const years = await academicApi.listYears();
      const currentYear = years.find((y) => y.isCurrent) || years[0];
      set({
        academicYears: years,
        selectedYearId: get().selectedYearId || (currentYear ? currentYear.id : null),
        isLoading: false,
      });
    } catch (err: any) {
      set({ error: err.message || 'Failed to fetch academic years', isLoading: false });
    }
  },

  selectYear: (id: string | null) => {
    set({ selectedYearId: id });
  },

  fetchClasses: async (academicYearId?: string) => {
    try {
      set({ isLoading: true, error: null });
      const yearId = academicYearId || get().selectedYearId || undefined;
      const classes = await academicApi.listClasses({ academicYearId: yearId });
      set({ classes, isLoading: false });
    } catch (err: any) {
      set({ error: err.message || 'Failed to fetch classes', isLoading: false });
    }
  },

  fetchSections: async (classId?: string) => {
    try {
      set({ isLoading: true, error: null });
      const sections = await academicApi.listSections({ classId });
      set({ sections, isLoading: false });
    } catch (err: any) {
      set({ error: err.message || 'Failed to fetch sections', isLoading: false });
    }
  },
}));
