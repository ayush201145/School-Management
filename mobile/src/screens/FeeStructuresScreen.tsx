import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, Alert } from 'react-native';
import { Text, Surface, Button, Chip, Divider, FAB, Modal, Portal, TextInput, HelperText, Menu } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { feeApi } from '../api/fees';
import { useAcademicStore } from '../stores/academicStore';
import { FeeCategory, FeeStructure } from '../types';
import { useAuthStore } from '../stores/authStore';
import { formatCurrency } from '../utils/format';

export const FeeStructuresScreen: React.FC = () => {
  const { role } = useAuthStore();
  const { classes, academicYears, selectedYearId, fetchClasses } = useAcademicStore();

  const [categories, setCategories] = useState<FeeCategory[]>([]);
  const [structures, setStructures] = useState<FeeStructure[]>([]);
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Create Fee Structure Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [modalCatId, setModalCatId] = useState('');
  const [modalClassId, setModalClassId] = useState('');
  const [modalAmount, setModalAmount] = useState('');
  const [modalDueDate, setModalDueDate] = useState('');
  const [modalDesc, setModalDesc] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const [catMenuVisible, setCatMenuVisible] = useState(false);
  const [clsMenuVisible, setClsMenuVisible] = useState(false);

  useEffect(() => {
    fetchClasses();
    feeApi.listCategories().then(setCategories).catch(() => {});
  }, []);

  const loadStructures = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await feeApi.listStructures({
        classId: selectedClassId || undefined,
        academicYearId: selectedYearId || undefined,
      });
      setStructures(res || []);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedClassId, selectedYearId]);

  useEffect(() => {
    loadStructures();
  }, [loadStructures]);

  const handleCreateStructure = async () => {
    const numAmount = parseFloat(modalAmount);
    if (!modalCatId || !modalClassId || isNaN(numAmount) || numAmount <= 0 || !modalDueDate) {
      setModalError('Please fill all required fields (*) with valid values');
      return;
    }

    try {
      setIsSubmitting(true);
      setModalError(null);
      await feeApi.createStructure({
        feeCategoryId: modalCatId,
        classId: modalClassId,
        academicYearId: selectedYearId || academicYears[0]?.id || '',
        amount: numAmount,
        dueDate: modalDueDate.trim(),
        description: modalDesc.trim() || undefined,
      });

      Alert.alert('Success', 'Fee structure created successfully');
      setShowAddModal(false);
      setModalCatId('');
      setModalClassId('');
      setModalAmount('');
      setModalDueDate('');
      setModalDesc('');
      loadStructures();
    } catch (err: any) {
      setModalError(err.message || 'Failed to create fee structure');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBulkAssign = (structure: FeeStructure) => {
    Alert.alert(
      'Assign to Students',
      `Assign fee of ₹${structure.amount} to all enrolled students in this class?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Assign',
          onPress: async () => {
            try {
              const res = await feeApi.assignStructure(structure.id);
              Alert.alert(
                'Fee Assigned',
                `Created: ${res.created} fee bills\nSkipped (already billed): ${res.skipped}\nTotal students: ${res.totalStudentsInScope}`
              );
            } catch (err: any) {
              Alert.alert('Error', err.message || 'Failed to assign fee');
            }
          },
        },
      ]
    );
  };

  const canManage = role === 'ADMIN' || role === 'MASTER';

  const modalCatName = categories.find((c) => c.id === modalCatId)?.name || 'Select Category *';
  const modalClsName = classes.find((c) => c.id === modalClassId)?.name || 'Select Class *';

  return (
    <View style={styles.container}>
      {/* Class Filters */}
      <View style={styles.filterRow}>
        <Chip
          selected={selectedClassId === null}
          onPress={() => setSelectedClassId(null)}
          style={styles.filterChip}
        >
          All Classes
        </Chip>
        {classes.map((cls) => (
          <Chip
            key={cls.id}
            selected={selectedClassId === cls.id}
            onPress={() => setSelectedClassId(selectedClassId === cls.id ? null : cls.id)}
            style={styles.filterChip}
          >
            {cls.name}
          </Chip>
        ))}
      </View>

      {/* List */}
      <FlatList
        data={structures}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadStructures} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="bank" size={48} color="#999" />
              <Text style={styles.emptyText}>No fee structures created yet</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <Surface style={styles.itemCard} elevation={1}>
            <View style={styles.cardHeader}>
              <View style={{ flex: 1 }}>
                <Text variant="titleMedium" style={styles.catName}>
                  {item.feeCategory?.name || 'Fee'}
                </Text>
                <Text style={styles.className}>
                  Class: {item.schoolClass?.name || 'General'}
                </Text>
                {item.description && (
                  <Text style={styles.descText}>{item.description}</Text>
                )}
                <Text style={styles.dueDate}>Due Date: {item.dueDate.substring(0, 10)}</Text>
              </View>

              <View style={styles.amountCol}>
                <Text style={styles.amountText}>₹{formatCurrency(item.amount)}</Text>
                {canManage && (
                  <Button
                    mode="contained"
                    compact
                    onPress={() => handleBulkAssign(item)}
                    style={styles.assignBtn}
                  >
                    Bulk Assign
                  </Button>
                )}
              </View>
            </View>
          </Surface>
        )}
      />

      {canManage && (
        <FAB
          icon="plus"
          label="New Structure"
          style={styles.fab}
          onPress={() => setShowAddModal(true)}
        />
      )}

      {/* Create Modal */}
      <Portal>
        <Modal
          visible={showAddModal}
          onDismiss={() => setShowAddModal(false)}
          contentContainerStyle={styles.modalContent}
        >
          <Text variant="titleLarge" style={styles.modalTitle}>
            New Fee Structure
          </Text>

          {modalError && <HelperText type="error" visible={true}>{modalError}</HelperText>}

          {/* Category Dropdown */}
          <Menu
            visible={catMenuVisible}
            onDismiss={() => setCatMenuVisible(false)}
            anchor={
              <Button
                mode="outlined"
                onPress={() => setCatMenuVisible(true)}
                style={styles.pickerBtn}
              >
                {modalCatName}
              </Button>
            }
          >
            {categories.map((c) => (
              <Menu.Item
                key={c.id}
                title={c.name}
                onPress={() => {
                  setModalCatId(c.id);
                  setCatMenuVisible(false);
                }}
              />
            ))}
          </Menu>

          {/* Class Dropdown */}
          <Menu
            visible={clsMenuVisible}
            onDismiss={() => setClsMenuVisible(false)}
            anchor={
              <Button
                mode="outlined"
                onPress={() => setClsMenuVisible(true)}
                style={styles.pickerBtn}
              >
                {modalClsName}
              </Button>
            }
          >
            {classes.map((cls) => (
              <Menu.Item
                key={cls.id}
                title={cls.name}
                onPress={() => {
                  setModalClassId(cls.id);
                  setClsMenuVisible(false);
                }}
              />
            ))}
          </Menu>

          <TextInput
            label="Amount (₹) *"
            value={modalAmount}
            onChangeText={setModalAmount}
            keyboardType="decimal-pad"
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Due Date (YYYY-MM-DD) *"
            placeholder="2026-06-30"
            value={modalDueDate}
            onChangeText={setModalDueDate}
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Description (e.g. Term 1 Tuition)"
            value={modalDesc}
            onChangeText={setModalDesc}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.modalActions}>
            <Button mode="text" onPress={() => setShowAddModal(false)}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleCreateStructure}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={{ backgroundColor: '#673AB7' }}
            >
              Create Structure
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
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  catName: {
    fontWeight: '700',
    color: '#212121',
  },
  className: {
    fontSize: 13,
    color: '#673AB7',
    fontWeight: '600',
    marginTop: 2,
  },
  descText: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  dueDate: {
    fontSize: 11,
    color: '#888',
    marginTop: 4,
  },
  amountCol: {
    alignItems: 'flex-end',
    gap: 8,
  },
  amountText: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1A237E',
  },
  assignBtn: {
    backgroundColor: '#673AB7',
    borderRadius: 6,
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
    backgroundColor: '#673AB7',
  },
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    padding: 20,
  },
  modalTitle: {
    fontWeight: '700',
    color: '#311B92',
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

export default FeeStructuresScreen;
