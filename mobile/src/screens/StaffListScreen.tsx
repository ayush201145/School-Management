import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, Alert, TouchableOpacity } from 'react-native';
import { Text, Surface, TextInput, FAB, Divider, Modal, Portal, Button, HelperText, Menu, Chip } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/types';
import { staffApi } from '../api/staff';
import { teacherApi } from '../api/teachers';
import { Staff, StaffType, Teacher } from '../types';
import { useAuthStore } from '../stores/authStore';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'StaffList'>;

const STAFF_TYPES: StaffType[] = ['TEACHER', 'ADMIN', 'ACCOUNTANT_STAFF', 'PEON', 'DRIVER', 'OTHER'];

export const StaffListScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { role } = useAuthStore();

  const [staffList, setStaffList] = useState<Staff[]>([]);
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [selectedType, setSelectedType] = useState<StaffType | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Add Staff Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState<StaffType>('OTHER');
  const [teacherId, setTeacherId] = useState<string | null>(null);
  const [phone, setPhone] = useState('');
  const [monthlySalary, setMonthlySalary] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const [typeMenuVisible, setTypeMenuVisible] = useState(false);
  const [teacherMenuVisible, setTeacherMenuVisible] = useState(false);

  useEffect(() => {
    teacherApi.list().then(setTeachers).catch(() => {});
  }, []);

  const loadStaff = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await staffApi.list({
        type: selectedType || undefined,
        isActive: true,
      });
      setStaffList(res || []);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedType]);

  useEffect(() => {
    loadStaff();
  }, [loadStaff]);

  const handleCreateStaff = async () => {
    if (!name.trim()) {
      setModalError('Staff name is required');
      return;
    }
    const salary = monthlySalary.trim() ? parseFloat(monthlySalary) : undefined;

    try {
      setIsSubmitting(true);
      setModalError(null);
      await staffApi.create({
        name: name.trim(),
        type,
        teacherId: type === 'TEACHER' ? teacherId || undefined : undefined,
        phone: phone.trim() || undefined,
        monthlySalary: salary,
      });

      Alert.alert('Success', 'Staff member added');
      setShowAddModal(false);
      setName('');
      setType('OTHER');
      setTeacherId(null);
      setPhone('');
      setMonthlySalary('');
      loadStaff();
    } catch (err: any) {
      setModalError(err.message || 'Failed to add staff member');
    } finally {
      setIsSubmitting(false);
    }
  };

  const canManage = role === 'ADMIN' || role === 'MASTER';

  return (
    <View style={styles.container}>
      {/* Type Filters */}
      <View style={styles.filterRow}>
        <Chip
          selected={selectedType === null}
          onPress={() => setSelectedType(null)}
          style={styles.filterChip}
        >
          All Staff
        </Chip>
        {STAFF_TYPES.map((t) => (
          <Chip
            key={t}
            selected={selectedType === t}
            onPress={() => setSelectedType(selectedType === t ? null : t)}
            style={styles.filterChip}
          >
            {t}
          </Chip>
        ))}
      </View>

      {/* List */}
      <FlatList
        data={staffList}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadStaff} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="account-group" size={48} color="#999" />
              <Text style={styles.emptyText}>No staff records found</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={() => navigation.navigate('StaffDetail', { staffId: item.id })}
          >
            <Surface style={styles.itemCard} elevation={1}>
              <View style={styles.avatar}>
                <Text style={styles.avatarText}>{item.name.charAt(0)}</Text>
              </View>
              <View style={styles.infoCol}>
                <Text variant="titleMedium" style={styles.name}>
                  {item.name}
                </Text>
                <Text style={styles.typeBadge}>Type: {item.type}</Text>
                {item.monthlySalary !== null && item.monthlySalary !== undefined && (
                  <Text style={styles.salaryText}>
                    Monthly Salary: ₹{Number(item.monthlySalary).toFixed(2)}
                  </Text>
                )}
                {item.phone && <Text style={styles.phoneText}>Ph: {item.phone}</Text>}
              </View>
              <MaterialCommunityIcons name="chevron-right" size={24} color="#9E9E9E" />
            </Surface>
          </TouchableOpacity>
        )}
      />

      {canManage && (
        <FAB
          icon="plus"
          label="Add Staff"
          style={styles.fab}
          onPress={() => setShowAddModal(true)}
        />
      )}

      {/* Add Staff Modal */}
      <Portal>
        <Modal
          visible={showAddModal}
          onDismiss={() => setShowAddModal(false)}
          contentContainerStyle={styles.modalContent}
        >
          <Text variant="titleLarge" style={styles.modalTitle}>
            Add Staff Member
          </Text>

          {modalError && <HelperText type="error" visible={true}>{modalError}</HelperText>}

          <TextInput
            label="Full Name *"
            value={name}
            onChangeText={setName}
            mode="outlined"
            style={styles.input}
          />

          {/* Type Picker */}
          <Menu
            visible={typeMenuVisible}
            onDismiss={() => setTypeMenuVisible(false)}
            anchor={
              <Button
                mode="outlined"
                onPress={() => setTypeMenuVisible(true)}
                style={styles.pickerBtn}
              >
                Staff Type: {type}
              </Button>
            }
          >
            {STAFF_TYPES.map((t) => (
              <Menu.Item
                key={t}
                title={t}
                onPress={() => {
                  setType(t);
                  setTypeMenuVisible(false);
                }}
              />
            ))}
          </Menu>

          {/* Teacher link if type is TEACHER */}
          {type === 'TEACHER' && (
            <Menu
              visible={teacherMenuVisible}
              onDismiss={() => setTeacherMenuVisible(false)}
              anchor={
                <Button
                  mode="outlined"
                  onPress={() => setTeacherMenuVisible(true)}
                  style={styles.pickerBtn}
                >
                  {teachers.find((t) => t.id === teacherId)
                    ? `Linked: ${teachers.find((t) => t.id === teacherId)?.firstName}`
                    : 'Link to Teacher Profile (Optional)'}
                </Button>
              }
            >
              {teachers.map((t) => (
                <Menu.Item
                  key={t.id}
                  title={`${t.firstName} ${t.lastName} (${t.employeeNo})`}
                  onPress={() => {
                    setTeacherId(t.id);
                    setName(`${t.firstName} ${t.lastName}`);
                    setPhone(t.phone || '');
                    setTeacherMenuVisible(false);
                  }}
                />
              ))}
            </Menu>
          )}

          <TextInput
            label="Monthly Salary (₹)"
            value={monthlySalary}
            onChangeText={setMonthlySalary}
            keyboardType="decimal-pad"
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Phone"
            value={phone}
            onChangeText={setPhone}
            keyboardType="phone-pad"
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.modalActions}>
            <Button mode="text" onPress={() => setShowAddModal(false)}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleCreateStaff}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={{ backgroundColor: '#795548' }}
            >
              Add Staff
            </Button>
          </View>
        </Modal>
      </Portal>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 10,
    flexWrap: 'wrap',
    gap: 6,
  },
  filterChip: {
    marginRight: 4,
  },
  listContent: {
    padding: 16,
    paddingTop: 4,
    paddingBottom: 80,
  },
  divider: {
    height: 8,
    backgroundColor: 'transparent',
  },
  itemCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 12,
  },
  avatar: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#EFEBE9',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  avatarText: {
    color: '#795548',
    fontWeight: '700',
    fontSize: 16,
  },
  infoCol: {
    flex: 1,
  },
  name: {
    fontWeight: '600',
    color: '#212121',
  },
  typeBadge: {
    fontSize: 12,
    color: '#8D6E63',
    fontWeight: '600',
    marginTop: 2,
  },
  salaryText: {
    fontSize: 13,
    color: '#388E3C',
    fontWeight: '600',
    marginTop: 2,
  },
  phoneText: {
    fontSize: 12,
    color: '#777',
    marginTop: 2,
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
  fab: {
    position: 'absolute',
    right: 16,
    bottom: 24,
    backgroundColor: '#795548',
  },
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    padding: 20,
  },
  modalTitle: {
    fontWeight: '700',
    color: '#4E342E',
    marginBottom: 12,
  },
  pickerBtn: {
    marginBottom: 10,
  },
  input: {
    marginBottom: 10,
    backgroundColor: '#fff',
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 12,
  },
});

export default StaffListScreen;
