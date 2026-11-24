import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, ScrollView, Alert, FlatList } from 'react-native';
import { Text, Surface, Button, Divider, ActivityIndicator, Modal, Portal, TextInput, HelperText } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useRoute, RouteProp } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/types';
import { staffApi } from '../api/staff';
import { Staff, SalaryPayment, PaymentMode } from '../types';
import { useAuthStore } from '../stores/authStore';
import { formatCurrency } from '../utils/format';

type StaffDetailRouteProp = RouteProp<RootStackParamList, 'StaffDetail'>;

const PAYMENT_MODES: PaymentMode[] = ['CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'CARD', 'OTHER'];

export const StaffDetailScreen: React.FC = () => {
  const route = useRoute<StaffDetailRouteProp>();
  const { staffId } = route.params;
  const { role } = useAuthStore();

  const [staff, setStaff] = useState<Staff | null>(null);
  const [payments, setPayments] = useState<SalaryPayment[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Pay Salary Modal
  const [showPayModal, setShowPayModal] = useState(false);
  const [salaryAmount, setSalaryAmount] = useState('');
  const [payMonth, setPayMonth] = useState((new Date().getMonth() + 1).toString());
  const [payYear, setPayYear] = useState(new Date().getFullYear().toString());
  const [payMode, setPayMode] = useState<PaymentMode>('BANK_TRANSFER');
  const [payNotes, setPayNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const loadDetails = useCallback(async () => {
    try {
      setIsLoading(true);
      const [staffData, salaryData] = await Promise.all([
        staffApi.getById(staffId),
        staffApi.listSalaryPayments(staffId),
      ]);
      setStaff(staffData);
      setPayments(salaryData || []);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to load staff details');
    } finally {
      setIsLoading(false);
    }
  }, [staffId]);

  useEffect(() => {
    loadDetails();
  }, [loadDetails]);

  const handlePaySalary = async () => {
    const amt = parseFloat(salaryAmount);
    const m = parseInt(payMonth, 10);
    const y = parseInt(payYear, 10);

    if (isNaN(amt) || amt <= 0 || isNaN(m) || m < 1 || m > 12 || isNaN(y)) {
      setModalError('Please enter a valid amount and month/year');
      return;
    }

    try {
      setIsSubmitting(true);
      setModalError(null);
      await staffApi.recordSalaryPayment(staffId, {
        amount: amt,
        forMonth: m,
        forYear: y,
        month: m,
        year: y,
        mode: payMode,
        notes: payNotes.trim() || undefined,
      });

      Alert.alert('Salary Recorded', `Paid ₹${amt.toFixed(2)} for ${m}/${y}`);
      setShowPayModal(false);
      setSalaryAmount('');
      setPayNotes('');
      loadDetails();
    } catch (err: any) {
      setModalError(err.message || 'Failed to record salary payment');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading || !staff) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#795548" />
      </View>
    );
  }

  const canManage = role === 'ADMIN' || role === 'ACCOUNTANT' || role === 'MASTER';

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Profile Card */}
      <Surface style={styles.card} elevation={2}>
        <View style={styles.headerRow}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>{staff.name.charAt(0)}</Text>
          </View>
          <View style={styles.headerInfo}>
            <Text variant="titleLarge" style={styles.staffName}>
              {staff.name}
            </Text>
            <Text style={styles.staffType}>Role: {staff.type}</Text>
            {staff.phone && <Text style={styles.phoneText}>Ph: {staff.phone}</Text>}
          </View>
        </View>

        <Divider style={styles.divider} />

        <View style={styles.salaryRow}>
          <Text style={styles.salaryLabel}>Agreed Monthly Salary:</Text>
          <Text style={styles.salaryVal}>
            ₹{staff.monthlySalary ? Number(staff.monthlySalary).toFixed(2) : '0.00'}
          </Text>
        </View>

        {canManage && (
          <Button
            mode="contained"
            icon="cash-check"
            onPress={() => {
              setSalaryAmount(staff.monthlySalary ? staff.monthlySalary.toString() : '');
              setShowPayModal(true);
            }}
            style={styles.payBtn}
          >
            Record Salary Payment
          </Button>
        )}
      </Surface>

      {/* Salary History */}
      <Text variant="titleMedium" style={styles.historyTitle}>
        Payment History ({payments.length})
      </Text>

      {payments.length === 0 ? (
        <Surface style={styles.emptyCard} elevation={1}>
          <Text style={styles.emptyText}>No salary payments recorded yet</Text>
        </Surface>
      ) : (
        payments.map((p) => (
          <Surface key={p.id} style={styles.paymentCard} elevation={1}>
            <View style={styles.payRow}>
              <View>
                <Text style={styles.payMonth}>
                  Month: {p.forMonth ?? p.month}/{p.forYear ?? p.year}
                </Text>
                <Text style={styles.payDate}>
                  Paid: {p.paidAt ? p.paidAt.substring(0, 10) : ''} ({p.mode})
                </Text>
                {p.notes && <Text style={styles.payNotes}>Note: {p.notes}</Text>}
              </View>
              <Text style={styles.payAmount}>₹{formatCurrency(p.amount)}</Text>
            </View>
          </Surface>
        ))
      )}

      {/* Salary Modal */}
      <Portal>
        <Modal
          visible={showPayModal}
          onDismiss={() => setShowPayModal(false)}
          contentContainerStyle={styles.modalContent}
        >
          <Text variant="titleLarge" style={styles.modalTitle}>
            Record Salary Payment
          </Text>

          {modalError && <HelperText type="error" visible={true}>{modalError}</HelperText>}

          <TextInput
            label="Amount (₹) *"
            value={salaryAmount}
            onChangeText={setSalaryAmount}
            keyboardType="decimal-pad"
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.row}>
            <TextInput
              label="Month (1-12) *"
              value={payMonth}
              onChangeText={setPayMonth}
              keyboardType="number-pad"
              mode="outlined"
              style={[styles.input, styles.half]}
            />
            <TextInput
              label="Year *"
              value={payYear}
              onChangeText={setPayYear}
              keyboardType="number-pad"
              mode="outlined"
              style={[styles.input, styles.half]}
            />
          </View>

          <Text style={styles.label}>Payment Mode</Text>
          <View style={styles.modeGrid}>
            {PAYMENT_MODES.map((m) => (
              <Button
                key={m}
                mode={payMode === m ? 'contained' : 'outlined'}
                onPress={() => setPayMode(m)}
                compact
              >
                {m}
              </Button>
            ))}
          </View>

          <TextInput
            label="Notes / Transaction ID"
            value={payNotes}
            onChangeText={setPayNotes}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.modalActions}>
            <Button mode="text" onPress={() => setShowPayModal(false)}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handlePaySalary}
              loading={isSubmitting}
              disabled={isSubmitting}
              style={{ backgroundColor: '#4CAF50' }}
            >
              Confirm Salary
            </Button>
          </View>
        </Modal>
      </Portal>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  content: {
    padding: 16,
    paddingBottom: 40,
  },
  center: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  card: {
    backgroundColor: '#fff',
    borderRadius: 14,
    padding: 16,
    marginBottom: 16,
  },
  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#EFEBE9',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  avatarText: {
    color: '#795548',
    fontWeight: '700',
    fontSize: 22,
  },
  headerInfo: {
    flex: 1,
  },
  staffName: {
    fontWeight: '700',
    color: '#212121',
  },
  staffType: {
    color: '#8D6E63',
    fontWeight: '600',
    fontSize: 13,
    marginTop: 2,
  },
  phoneText: {
    color: '#666',
    fontSize: 12,
    marginTop: 2,
  },
  divider: {
    marginVertical: 14,
  },
  salaryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  salaryLabel: {
    fontSize: 14,
    color: '#666',
  },
  salaryVal: {
    fontSize: 18,
    fontWeight: '700',
    color: '#2E7D32',
  },
  payBtn: {
    backgroundColor: '#795548',
    borderRadius: 8,
  },
  historyTitle: {
    fontWeight: '700',
    color: '#424242',
    marginBottom: 10,
  },
  emptyCard: {
    padding: 24,
    backgroundColor: '#fff',
    borderRadius: 12,
    alignItems: 'center',
  },
  emptyText: {
    color: '#999',
  },
  paymentCard: {
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 10,
    marginBottom: 8,
  },
  payRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  payMonth: {
    fontWeight: '700',
    color: '#212121',
    fontSize: 14,
  },
  payDate: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  payNotes: {
    fontSize: 11,
    color: '#888',
    marginTop: 2,
  },
  payAmount: {
    fontSize: 16,
    fontWeight: '700',
    color: '#388E3C',
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
  row: {
    flexDirection: 'row',
    gap: 10,
  },
  half: {
    flex: 1,
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

export default StaffDetailScreen;
