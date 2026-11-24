import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, Alert, TouchableOpacity, RefreshControl } from 'react-native';
import { Text, Surface, Button, Chip, Divider, Menu, ActivityIndicator } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { attendanceApi } from '../api/attendance';
import { studentApi } from '../api/students';
import { useAcademicStore } from '../stores/academicStore';
import { AttendanceStatus, Student } from '../types';

const STATUS_OPTIONS: AttendanceStatus[] = ['PRESENT', 'ABSENT', 'LATE', 'LEAVE'];

export const AttendanceScreen: React.FC = () => {
  const { classes, sections, fetchClasses, fetchSections } = useAcademicStore();

  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [selectedSectionId, setSelectedSectionId] = useState<string | null>(null);
  const [date, setDate] = useState(new Date().toISOString().substring(0, 10));

  const [students, setStudents] = useState<Student[]>([]);
  const [attendanceMap, setAttendanceMap] = useState<Record<string, AttendanceStatus>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const [classMenuVisible, setClassMenuVisible] = useState(false);
  const [sectionMenuVisible, setSectionMenuVisible] = useState(false);

  useEffect(() => {
    fetchClasses();
  }, []);

  useEffect(() => {
    if (selectedClassId) {
      fetchSections(selectedClassId);
    }
  }, [selectedClassId]);

  const loadAttendance = useCallback(async () => {
    if (!selectedSectionId) return;

    try {
      setIsLoading(true);
      const [allStudents, records] = await Promise.all([
        studentApi.list({ sectionId: selectedSectionId, isActive: true }),
        attendanceApi.getAttendance(selectedSectionId, date),
      ]);

      setStudents(allStudents);

      const map: Record<string, AttendanceStatus> = {};
      // Default to PRESENT for all students
      allStudents.forEach((s) => {
        map[s.id] = 'PRESENT';
      });
      // Override with recorded attendance
      records.forEach((r) => {
        map[r.studentId] = r.status;
      });

      setAttendanceMap(map);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedSectionId, date]);

  useEffect(() => {
    loadAttendance();
  }, [loadAttendance]);

  const setStatus = (studentId: string, status: AttendanceStatus) => {
    setAttendanceMap((prev) => ({
      ...prev,
      [studentId]: status,
    }));
  };

  const markAll = (status: AttendanceStatus) => {
    const map: Record<string, AttendanceStatus> = {};
    students.forEach((s) => {
      map[s.id] = status;
    });
    setAttendanceMap(map);
  };

  const handleSave = async () => {
    if (!selectedSectionId) {
      Alert.alert('Required', 'Please select a section');
      return;
    }

    try {
      setIsSaving(true);
      const records = Object.keys(attendanceMap).map((studentId) => ({
        studentId,
        status: attendanceMap[studentId],
      }));

      await attendanceApi.saveAttendance(selectedSectionId, date, records);
      Alert.alert('Saved', 'Attendance recorded successfully');
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to save attendance');
    } finally {
      setIsSaving(false);
    }
  };

  const selectedClassName = classes.find((c) => c.id === selectedClassId)?.name || 'Select Class';
  const selectedSectionName = sections.find((s) => s.id === selectedSectionId)?.name || 'Select Section';

  const presentCount = Object.values(attendanceMap).filter((s) => s === 'PRESENT').length;
  const absentCount = Object.values(attendanceMap).filter((s) => s === 'ABSENT').length;

  return (
    <View style={styles.container}>
      {/* Pickers Header */}
      <Surface style={styles.headerCard} elevation={1}>
        <View style={styles.pickerRow}>
          <Menu
            visible={classMenuVisible}
            onDismiss={() => setClassMenuVisible(false)}
            anchor={
              <Button
                mode="outlined"
                onPress={() => setClassMenuVisible(true)}
                style={styles.pickerBtn}
              >
                {selectedClassName}
              </Button>
            }
          >
            {classes.map((cls) => (
              <Menu.Item
                key={cls.id}
                title={cls.name}
                onPress={() => {
                  setSelectedClassId(cls.id);
                  setSelectedSectionId(null);
                  setClassMenuVisible(false);
                }}
              />
            ))}
          </Menu>

          <Menu
            visible={sectionMenuVisible}
            onDismiss={() => setSectionMenuVisible(false)}
            anchor={
              <Button
                mode="outlined"
                onPress={() => setSectionMenuVisible(true)}
                disabled={!selectedClassId}
                style={styles.pickerBtn}
              >
                {selectedSectionName}
              </Button>
            }
          >
            {sections.map((sec) => (
              <Menu.Item
                key={sec.id}
                title={sec.name}
                onPress={() => {
                  setSelectedSectionId(sec.id);
                  setSectionMenuVisible(false);
                }}
              />
            ))}
          </Menu>
        </View>

        {selectedSectionId && (
          <View style={styles.summaryBar}>
            <Text style={styles.summaryText}>
              Date: <Text style={{ fontWeight: '700' }}>{date}</Text> | Present:{' '}
              <Text style={{ color: '#2E7D32', fontWeight: '700' }}>{presentCount}</Text> | Absent:{' '}
              <Text style={{ color: '#D32F2F', fontWeight: '700' }}>{absentCount}</Text>
            </Text>
            <View style={styles.quickActions}>
              <Button compact mode="text" onPress={() => markAll('PRESENT')}>
                All Present
              </Button>
              <Button compact mode="text" textColor="#D32F2F" onPress={() => markAll('ABSENT')}>
                All Absent
              </Button>
            </View>
          </View>
        )}
      </Surface>

      {/* Student List */}
      <FlatList
        data={students}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadAttendance} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="calendar-question" size={48} color="#999" />
              <Text style={styles.emptyText}>
                {selectedSectionId ? 'No students enrolled in this section' : 'Select a Class and Section'}
              </Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => {
          const currentStatus = attendanceMap[item.id] || 'PRESENT';
          return (
            <Surface style={styles.itemCard} elevation={1}>
              <View style={styles.studentInfo}>
                <Text variant="titleMedium" style={styles.studentName}>
                  {item.firstName} {item.lastName}
                </Text>
                <Text style={styles.rollText}>
                  Roll: {item.rollNo || 'N/A'} | Adm: {item.admissionNo}
                </Text>
              </View>

              <View style={styles.statusButtons}>
                {STATUS_OPTIONS.map((st) => {
                  const isSelected = currentStatus === st;
                  const bg =
                    st === 'PRESENT'
                      ? '#4CAF50'
                      : st === 'ABSENT'
                      ? '#E53935'
                      : st === 'LATE'
                      ? '#FF9800'
                      : '#2196F3';
                  return (
                    <TouchableOpacity
                      key={st}
                      style={[
                        styles.statusBtn,
                        isSelected ? { backgroundColor: bg } : styles.statusBtnUnselected,
                      ]}
                      onPress={() => setStatus(item.id, st)}
                    >
                      <Text
                        style={[
                          styles.statusBtnText,
                          isSelected ? { color: '#fff', fontWeight: '700' } : { color: '#666' },
                        ]}
                      >
                        {st.charAt(0)}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </Surface>
          );
        }}
      />

      {/* Bottom Save Bar */}
      {students.length > 0 && (
        <Surface style={styles.bottomBar} elevation={4}>
          <Button
            mode="contained"
            icon="content-save"
            onPress={handleSave}
            loading={isSaving}
            disabled={isSaving}
            style={styles.saveBtn}
            contentStyle={{ height: 48 }}
          >
            Save Attendance
          </Button>
        </Surface>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  headerCard: {
    padding: 12,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#EEEEEE',
  },
  pickerRow: {
    flexDirection: 'row',
    gap: 10,
  },
  pickerBtn: {
    flex: 1,
  },
  summaryBar: {
    marginTop: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  summaryText: {
    fontSize: 12,
    color: '#555',
  },
  quickActions: {
    flexDirection: 'row',
  },
  listContent: {
    padding: 12,
    paddingBottom: 90,
  },
  divider: {
    height: 8,
    backgroundColor: 'transparent',
  },
  itemCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 12,
    backgroundColor: '#fff',
    borderRadius: 10,
  },
  studentInfo: {
    flex: 1,
  },
  studentName: {
    fontWeight: '600',
    color: '#212121',
  },
  rollText: {
    fontSize: 12,
    color: '#777',
    marginTop: 2,
  },
  statusButtons: {
    flexDirection: 'row',
    gap: 6,
  },
  statusBtn: {
    width: 34,
    height: 34,
    borderRadius: 17,
    justifyContent: 'center',
    alignItems: 'center',
  },
  statusBtnUnselected: {
    backgroundColor: '#F0F0F0',
  },
  statusBtnText: {
    fontSize: 13,
  },
  emptyContainer: {
    alignItems: 'center',
    paddingTop: 60,
  },
  emptyText: {
    marginTop: 10,
    fontSize: 15,
    color: '#999',
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 14,
    backgroundColor: '#fff',
  },
  saveBtn: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
  },
});

export default AttendanceScreen;
