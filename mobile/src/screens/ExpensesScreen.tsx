import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, Alert } from 'react-native';
import { Text, Surface, FAB, Divider, Modal, Portal, TextInput, Button, HelperText, Menu, Chip } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { expenseApi } from '../api/expenses';
import { Expense, ExpenseCategory, PaymentMode, RecurringExpenseTemplate } from '../types';
import { useAuthStore } from '../stores/authStore';
import { formatCurrency } from '../utils/format';

const PAYMENT_MODES: PaymentMode[] = ['CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'CARD', 'OTHER'];

export const ExpensesScreen: React.FC = () => {
  const { role } = useAuthStore();

  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [totalAmount, setTotalAmount] = useState(0);
  const [categories, setCategories] = useState<ExpenseCategory[]>([]);
  const [selectedCatId, setSelectedCatId] = useState<string | null>(null);
  const [templates, setTemplates] = useState<RecurringExpenseTemplate[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Add Expense Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [expenseCatId, setExpenseCatId] = useState('');
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [mode, setMode] = useState<PaymentMode>('CASH');
  const [referenceNo, setReferenceNo] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [catMenuVisible, setCatMenuVisible] = useState(false);

  useEffect(() => {
    expenseApi.listCategories().then(setCategories).catch(() => {});
    expenseApi.listRecurringTemplates().then(setTemplates).catch(() => {});
  }, []);

  const loadExpenses = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await expenseApi.listExpenses({
        expenseCategoryId: selectedCatId || undefined,
      });
      setExpenses(res.expenses || []);
      setTotalAmount(res.totalAmount || 0);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedCatId]);

  useEffect(() => {
    loadExpenses();
  }, [loadExpenses]);

  const handleCreateExpense = async () => {
    const numAmount = parseFloat(amount);
    if (!expenseCatId || !description.trim() || isNaN(numAmount) || numAmount <= 0) {
      setModalError('Please select category and enter valid description and amount');
      return;
    }

    try {
      setIsSubmitting(true);
      setModalError(null);
      await expenseApi.createExpense({
        expenseCategoryId: expenseCatId,
        description: description.trim(),
        amount: numAmount,
        mode,
        referenceNo: referenceNo.trim() || undefined,
      });

      Alert.alert('Success', 'Expense recorded');
      setShowAddModal(false);
      setExpenseCatId('');
      setDescription('');
      setAmount('');
      setReferenceNo('');
      loadExpenses();
    } catch (err: any) {
      setModalError(err.message || 'Failed to record expense');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGenerateRecurring = async (tmpl: RecurringExpenseTemplate) => {
    Alert.alert(
      'Generate Recurring Expense',
      `Generate ₹${tmpl.amount} for "${tmpl.name}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Generate',
          onPress: async () => {
            try {
              await expenseApi.generateRecurringExpense(tmpl.id);
              Alert.alert('Success', 'Recurring expense created for this month');
              loadExpenses();
            } catch (err: any) {
              Alert.alert('Error', err.message || 'Failed to generate recurring expense');
            }
          },
        },
      ]
    );
  };

  const canManage = role === 'ADMIN' || role === 'ACCOUNTANT' || role === 'MASTER';
  const selectedCatName = categories.find((c) => c.id === expenseCatId)?.name || 'Select Category *';

  return (
    <View style={styles.container}>
      {/* Overview Card */}
      <Surface style={styles.card} elevation={2}>
        <Text style={styles.cardSubtitle}>Total Expenses</Text>
        <Text style={styles.totalAmount}>₹{formatCurrency(totalAmount)}</Text>
        <Text style={styles.countText}>{expenses.length} expense item(s)</Text>
      </Surface>

      {/* Category Filter */}
      <View style={styles.filterRow}>
        <Chip
          selected={selectedCatId === null}
          onPress={() => setSelectedCatId(null)}
          style={styles.filterChip}
        >
          All Categories
        </Chip>
        {categories.map((c) => (
          <Chip
            key={c.id}
            selected={selectedCatId === c.id}
            onPress={() => setSelectedCatId(selectedCatId === c.id ? null : c.id)}
            style={styles.filterChip}
          >
            {c.name}
          </Chip>
        ))}
      </View>

      {/* Recurring Templates Quick Bar (if any) */}
      {templates.length > 0 && canManage && (
        <View style={styles.templateSection}>
          <Text style={styles.templateTitle}>Recurring Bills:</Text>
          <View style={styles.templateRow}>
            {templates.map((t) => (
              <Chip
                key={t.id}
                icon="autorenew"
                onPress={() => handleGenerateRecurring(t)}
                style={styles.templateChip}
              >
                {t.name} (₹{t.amount})
              </Chip>
            ))}
          </View>
        </View>
      )}

      {/* Expenses List */}
      <FlatList
        data={expenses}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadExpenses} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="receipt" size={48} color="#999" />
              <Text style={styles.emptyText}>No expenses recorded</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <Surface style={styles.itemCard} elevation={1}>
            <View style={styles.itemRow}>
              <View style={styles.infoCol}>
                <Text variant="titleMedium" style={styles.desc}>
                  {item.description}
                </Text>
                <Text style={styles.catText}>
                  {item.expenseCategory?.name || 'General Expense'}
                </Text>
                <Text style={styles.metaText}>
                  {item.spentAt ? item.spentAt.substring(0, 10) : ''} | Mode: {item.mode}
                  {item.referenceNo ? ` | Ref: ${item.referenceNo}` : ''}
                </Text>
              </View>
              <Text style={styles.amountText}>-₹{formatCurrency(item.amount)}</Text>
            </View>
          </Surface>
        )}
      />

      {canManage && (
        <FAB
          icon="plus"
          label="New Expense"
          style={styles.fab}
          onPress={() => setShowAddModal(true)}
        />
      )}

      {/* Add Modal */}
      <Portal>
        <Modal
          visible={showAddModal}
          onDismiss={() => setShowAddModal(false)}
          contentContainerStyle={styles.modalContent}
        >
          <Text variant="titleLarge" style={styles.modalTitle}>
            Record Expense
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
                {selectedCatName}
              </Button>
            }
          >
            {categories.map((c) => (
              <Menu.Item
                key={c.id}
                title={c.name}
                onPress={() => {
                  setExpenseCatId(c.id);
                  setCatMenuVisible(false);
                }}
              />
            ))}
          </Menu>

          <TextInput
            label="Description (e.g. Printer paper, electricity bill) *"
            value={description}
            onChangeText={setDescription}
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Amount (₹) *"
            value={amount}
            onChangeText={setAmount}
            keyboardType="decimal-pad"
            mode="outlined"
            style={styles.input}
          />

          <Text style={styles.label}>Payment Mode</Text>
          <View style={styles.modeGrid}>
            {PAYMENT_MODES.map((m) => (
              <Button
                key={m}
                mode={mode === m ? 'contained' : 'outlined'}
                onPress={() => setMode(m)}
                compact
              >
                {m}
              </Button>
            ))}
          </View>

          <TextInput
            label="Reference No (Receipt # / UPI ID)"
            value={referenceNo}
            onChangeText={setReferenceNo}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.modalActions}>
            <Button mode="text" onPress={() => setShowAddModal(false)}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleCreateExpense}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={{ backgroundColor: '#E53935' }}
            >
              Record Expense
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
  card: {
    margin: 16,
    marginBottom: 8,
    padding: 16,
    backgroundColor: '#fff',
    borderRadius: 12,
    borderLeftWidth: 5,
    borderLeftColor: '#E53935',
  },
  cardSubtitle: {
    fontSize: 13,
    color: '#666',
  },
  totalAmount: {
    fontSize: 26,
    fontWeight: '700',
    color: '#C62828',
    marginVertical: 4,
  },
  countText: {
    fontSize: 12,
    color: '#888',
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 6,
    flexWrap: 'wrap',
    gap: 6,
  },
  filterChip: {
    marginRight: 4,
  },
  templateSection: {
    paddingHorizontal: 16,
    paddingVertical: 6,
  },
  templateTitle: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  templateRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  templateChip: {
    backgroundColor: '#FFF3E0',
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
  itemRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  infoCol: {
    flex: 1,
  },
  desc: {
    fontWeight: '600',
    color: '#212121',
  },
  catText: {
    fontSize: 12,
    color: '#D81B60',
    fontWeight: '600',
    marginTop: 2,
  },
  metaText: {
    fontSize: 11,
    color: '#888',
    marginTop: 4,
  },
  amountText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#E53935',
    marginLeft: 8,
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
    backgroundColor: '#E53935',
  },
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 12,
    padding: 20,
  },
  modalTitle: {
    fontWeight: '700',
    color: '#B71C1C',
    marginBottom: 12,
  },
  pickerBtn: {
    marginBottom: 10,
  },
  label: {
    color: '#666',
    marginBottom: 6,
    marginTop: 4,
  },
  modeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: 12,
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

export default ExpensesScreen;
