import apiClient from './client';
import { AttendanceRecord, AttendanceStatus } from '../types';

export const attendanceApi = {
  getAttendance: async (sectionId: string, date: string): Promise<AttendanceRecord[]> => {
    const res = await apiClient.get<AttendanceRecord[]>('/api/attendance', {
      params: { sectionId, date },
    });
    return res.data;
  },

  saveAttendance: async (
    sectionId: string,
    date: string,
    records: Array<{ studentId: string; status: AttendanceStatus }>
  ): Promise<{ message: string; count: number }> => {
    const res = await apiClient.post('/api/attendance', { sectionId, date, records });
    return res.data;
  },
};
