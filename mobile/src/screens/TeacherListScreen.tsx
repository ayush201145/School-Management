import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, Alert } from 'react-native';
import { Text, Surface, TextInput, FAB, IconButton, Divider, Modal, Portal, Button, HelperText } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { teacherApi } from '../api/teachers';
import { Teacher } from '../types';
import { useAuthStore } from '../stores/authStore';

export const TeacherListScreen: React.FC = () => {
  const { role } = useAuthStore();
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [search, setSearch] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Add Teacher Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [employeeNo, setEmployeeNo] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [qualification, setQualification] = useState('');
  const [address, setAddress] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const loadTeachers = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await teacherApi.list({ search: search.trim() || undefined });
      setTeachers(data);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [search]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadTeachers();
    }, 300);
    return () => clearTimeout(timer);
  }, [loadTeachers]);

  const handleCreateTeacher = async () => {
    if (!employeeNo.trim() || !firstName.trim() || !lastName.trim()) {
      setModalError('Employee No, First Name and Last Name are required');
      return;
    }

    try {
      setIsSubmitting(true);
      setModalError(null);
      await teacherApi.create({
        employeeNo: employeeNo.trim(),
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: phone.trim() || undefined,
        email: email.trim() || undefined,
        qualification: qualification.trim() || undefined,
        address: address.trim() || undefined,
      });

      Alert.alert('Success', 'Teacher added successfully');
      setShowAddModal(false);
      setEmployeeNo('');
      setFirstName('');
      setLastName('');
      setPhone('');
      setEmail('');
      setQualification('');
      setAddress('');
      loadTeachers();
    } catch (err: any) {
      setModalError(err.message || 'Failed to add teacher');
    } finally {
      setIsSubmitting(false);
    }
  };

  const canAdd = role === 'ADMIN' || role === 'MASTER';

  return (
    <View style={styles.container}>
      <TextInput
        placeholder="Search teachers by name or employee no..."
        value={search}
        onChangeText={setSearch}
        mode="outlined"
        style={styles.searchBar}
        left={<TextInput.Icon icon="magnify" />}
        right={search ? <TextInput.Icon icon="close" onPress={() => setSearch('')} /> : null}
      />

      <FlatList
        data={teachers}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadTeachers} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="account-search" size={48} color="#999" />
              <Text style={styles.emptyText}>No teachers found</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <Surface style={styles.card} elevation={1}>
            <View style={styles.avatar}>
              <Text style={styles.avatarText}>
                {item.firstName.charAt(0)}
                {item.lastName.charAt(0)}
              </Text>
            </View>
            <View style={styles.info}>
              <Text variant="titleMedium" style={styles.name}>
                {item.firstName} {item.lastName}
              </Text>
              <Text style={styles.subText}>Emp No: {item.employeeNo}</Text>
              {item.qualification && (
                <Text style={styles.qualText}>Qual: {item.qualification}</Text>
              )}
              {item.phone && (
                <View style={styles.contactRow}>
                  <MaterialCommunityIcons name="phone" size={14} color="#666" />
                  <Text style={styles.contactText}>{item.phone}</Text>
                </View>
              )}
            </View>
          </Surface>
        )}
      />

      {canAdd && (
        <FAB
          icon="plus"
          label="Add Teacher"
          style={styles.fab}
          onPress={() => setShowAddModal(true)}
        />
      )}

      {/* Add Teacher Modal */}
      <Portal>
        <Modal
          visible={showAddModal}
          onDismiss={() => setShowAddModal(false)}
          contentContainerStyle={styles.modalContent}
        >
          <Text variant="titleLarge" style={styles.modalTitle}>
            Add New Teacher
          </Text>

          {modalError && <HelperText type="error" visible={true}>{modalError}</HelperText>}

          <TextInput
            label="Employee No *"
            value={employeeNo}
            onChangeText={setEmployeeNo}
            mode="outlined"
            style={styles.input}
          />
          <View style={styles.row}>
            <TextInput
              label="First Name *"
              value={firstName}
              onChangeText={setFirstName}
              mode="outlined"
              style={[styles.input, styles.half]}
            />
            <TextInput
              label="Last Name *"
              value={lastName}
              onChangeText={setLastName}
              mode="outlined"
              style={[styles.input, styles.half]}
            />
          </View>
          <TextInput
            label="Qualification (e.g. B.Ed, M.Sc)"
            value={qualification}
            onChangeText={setQualification}
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
          <TextInput
            label="Email"
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            mode="outlined"
            style={styles.input}
          />
          <TextInput
            label="Address"
            value={address}
            onChangeText={setAddress}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.modalActions}>
            <Button mode="text" onPress={() => setShowAddModal(false)}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleCreateTeacher}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={{ backgroundColor: '#9C27B0' }}
            >
              Add Teacher
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
  searchBar: {
    margin: 12,
    backgroundColor: '#fff',
  },
  listContent: {
    padding: 12,
    paddingBottom: 80,
  },
  divider: {
    height: 8,
    backgroundColor: 'transparent',
  },
  card: {
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
    backgroundColor: '#F3E5F5',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  avatarText: {
    color: '#9C27B0',
    fontWeight: '700',
    fontSize: 16,
  },
  info: {
    flex: 1,
  },
  name: {
    fontWeight: '600',
    color: '#212121',
  },
  subText: {
    fontSize: 13,
    color: '#666',
    marginTop: 2,
  },
  qualText: {
    fontSize: 12,
    color: '#7B1FA2',
    marginTop: 2,
  },
  contactRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 4,
  },
  contactText: {
    fontSize: 12,
    color: '#666',
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
    backgroundColor: '#9C27B0',
  },
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    padding: 20,
  },
  modalTitle: {
    fontWeight: '700',
    color: '#4A148C',
    marginBottom: 14,
  },
  row: {
    flexDirection: 'row',
    gap: 10,
  },
  half: {
    flex: 1,
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

export default TeacherListScreen;
