import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, TouchableOpacity } from 'react-native';
import { Text, Surface, TextInput, FAB, IconButton, Chip, ActivityIndicator, Divider } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/types';
import { studentApi } from '../api/students';
import { useAcademicStore } from '../stores/academicStore';
import { useAuthStore } from '../stores/authStore';
import { Student } from '../types';
import AddStudentModal from './AddStudentModal';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'StudentList'>;

export const StudentListScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { role } = useAuthStore();
  const { classes, fetchClasses } = useAcademicStore();

  const [students, setStudents] = useState<Student[]>([]);
  const [search, setSearch] = useState('');
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [isActiveFilter, setIsActiveFilter] = useState<boolean | undefined>(true);
  const [isLoading, setIsLoading] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);

  useEffect(() => {
    fetchClasses();
  }, []);

  const loadStudents = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await studentApi.list({
        search: search.trim() || undefined,
        isActive: isActiveFilter,
      });

      let filtered = data;
      if (selectedClassId) {
        filtered = data.filter((s) => s.section?.schoolClass?.id === selectedClassId);
      }
      setStudents(filtered);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [search, selectedClassId, isActiveFilter]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadStudents();
    }, 300);
    return () => clearTimeout(timer);
  }, [loadStudents]);

  const canAddStudent = role === 'ADMIN' || role === 'MASTER';

  return (
    <View style={styles.container}>
      {/* Search Bar */}
      <TextInput
        placeholder="Search student, adm no, or phone..."
        value={search}
        onChangeText={setSearch}
        mode="outlined"
        style={styles.searchBar}
        left={<TextInput.Icon icon="magnify" />}
        right={search ? <TextInput.Icon icon="close" onPress={() => setSearch('')} /> : null}
      />

      {/* Filter Row */}
      <View style={styles.filterRow}>
        <Chip
          selected={selectedClassId === null}
          onPress={() => setSelectedClassId(null)}
          style={styles.filterChip}
        >
          All Classes
        </Chip>
        {classes.map((c) => (
          <Chip
            key={c.id}
            selected={selectedClassId === c.id}
            onPress={() => setSelectedClassId(selectedClassId === c.id ? null : c.id)}
            style={styles.filterChip}
          >
            {c.name}
          </Chip>
        ))}
      </View>

      {/* List */}
      <FlatList
        data={students}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadStudents} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="account-search" size={48} color="#999" />
              <Text style={styles.emptyText}>No students found</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={() => navigation.navigate('StudentDetail', { studentId: item.id })}
          >
            <Surface style={styles.studentItem} elevation={1}>
              <View style={styles.avatar}>
                <Text style={styles.avatarText}>
                  {item.firstName.charAt(0)}
                  {item.lastName.charAt(0)}
                </Text>
              </View>
              <View style={styles.studentInfo}>
                <View style={styles.nameRow}>
                  <Text variant="titleMedium" style={styles.studentName}>
                    {item.firstName} {item.lastName}
                  </Text>
                  {!item.isActive && (
                    <View style={styles.withdrawnBadge}>
                      <Text style={styles.withdrawnText}>Withdrawn</Text>
                    </View>
                  )}
                </View>
                <Text style={styles.studentSub}>
                  Adm: {item.admissionNo}
                  {item.rollNo ? ` | Roll: ${item.rollNo}` : ''}
                </Text>
                <Text style={styles.studentClass}>
                  Class: {item.section?.schoolClass?.name || 'N/A'} - Section: {item.section?.name || 'N/A'}
                </Text>
              </View>
              <MaterialCommunityIcons name="chevron-right" size={24} color="#9E9E9E" />
            </Surface>
          </TouchableOpacity>
        )}
      />

      {/* FAB */}
      {canAddStudent && (
        <FAB
          icon="plus"
          label="Admit Student"
          style={styles.fab}
          onPress={() => setShowAddModal(true)}
        />
      )}

      {/* Modal */}
      <AddStudentModal
        visible={showAddModal}
        onDismiss={() => setShowAddModal(false)}
        onStudentAdded={loadStudents}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  searchBar: {
    margin: 12,
    marginBottom: 6,
    backgroundColor: '#fff',
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: 12,
    paddingBottom: 8,
    flexWrap: 'wrap',
    gap: 6,
  },
  filterChip: {
    marginRight: 4,
  },
  listContent: {
    padding: 12,
    paddingBottom: 80,
  },
  divider: {
    height: 8,
    backgroundColor: 'transparent',
  },
  studentItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    borderRadius: 12,
    backgroundColor: '#fff',
  },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#E8EAF6',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  avatarText: {
    color: '#3F51B5',
    fontWeight: '700',
    fontSize: 16,
  },
  studentInfo: {
    flex: 1,
  },
  nameRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  studentName: {
    fontWeight: '600',
    color: '#212121',
  },
  withdrawnBadge: {
    backgroundColor: '#FFEBEE',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  withdrawnText: {
    color: '#D32F2F',
    fontSize: 11,
    fontWeight: '700',
  },
  studentSub: {
    fontSize: 13,
    color: '#666',
    marginTop: 2,
  },
  studentClass: {
    fontSize: 12,
    color: '#888',
    marginTop: 2,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 60,
  },
  emptyText: {
    color: '#999',
    marginTop: 10,
    fontSize: 15,
  },
  fab: {
    position: 'absolute',
    right: 16,
    bottom: 24,
    backgroundColor: '#3F51B5',
  },
});

export default StudentListScreen;
