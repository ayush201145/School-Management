import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, ScrollView, Alert, TouchableOpacity } from 'react-native';
import { Text, Surface, Button, Chip, Divider, ActivityIndicator, Dialog, Portal, RadioButton, TextInput } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useRoute, RouteProp } from '@react-navigation/native';
import { RootStackParamList } from '../navigation/types';
import { studentApi } from '../api/students';
import { Student, StudentFee, WithdrawalReason, ReceiptData, Payment } from '../types';
import { useAuthStore } from '../stores/authStore';
import { formatCurrency, toNum } from '../utils/format';
import RecordPaymentModal from './RecordPaymentModal';
import RecordBulkPaymentModal from './RecordBulkPaymentModal';
import PurchaseItemModal from './PurchaseItemModal';
import ReceiptOptionsModal from './ReceiptOptionsModal';

type StudentDetailRouteProp = RouteProp<RootStackParamList, 'StudentDetail'>;

export const StudentDetailScreen: React.FC = () => {
  const route = useRoute<StudentDetailRouteProp>();
  const { studentId } = route.params;
  const { role } = useAuthStore();

  const [student, setStudent] = useState<Student | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Modals state
  const [paymentFee, setPaymentFee] = useState<StudentFee | null>(null);
  const [showPurchaseModal, setShowPurchaseModal] = useState(false);
  const [showBulkPaymentModal, setShowBulkPaymentModal] = useState(false);
  const [activeReceiptData, setActiveReceiptData] = useState<ReceiptData | null>(null);
  const [showWithdrawDialog, setShowWithdrawDialog] = useState(false);
  const [withdrawReason, setWithdrawReason] = useState<WithdrawalReason>('TRANSFERRED');
  const [withdrawNotes, setWithdrawNotes] = useState('');

  const loadStudent = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await studentApi.getById(studentId);
      setStudent(data);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to load student details');
    } finally {
      setIsLoading(false);
    }
  }, [studentId]);

  useEffect(() => {
    loadStudent();
  }, [loadStudent]);

  const handleWithdraw = async () => {
    try {
      await studentApi.withdraw(studentId, withdrawReason, withdrawNotes);
      setShowWithdrawDialog(false);
      Alert.alert('Student Withdrawn', 'Student has been marked as inactive/withdrawn.');
      loadStudent();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to withdraw student');
    }
  };

  const handleReinstate = async () => {
    Alert.alert(
      'Reinstate Student',
      'Are you sure you want to reinstate this student?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Reinstate',
          onPress: async () => {
            try {
              await studentApi.reinstate(studentId);
              Alert.alert('Success', 'Student has been reinstated.');
              loadStudent();
            } catch (err: any) {
              Alert.alert('Error', err.message || 'Failed to reinstate student');
            }
          },
        },
      ]
    );
  };

  if (isLoading || !student) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#3F51B5" />
      </View>
    );
  }

  const fees = student.studentFees || [];
  const totalAmount = fees.reduce(
    (sum, f) => sum + (toNum(f.amount) - (toNum(f.discount) || 0)),
    0
  );
  const totalPaid = fees.reduce(
    (sum, f) => sum + (f.payments?.reduce((pSum, p) => pSum + toNum(p.amount), 0) || 0),
    0
  );
  const totalDue = Math.max(0, totalAmount - totalPaid);

  const canManageFees = role === 'ADMIN' || role === 'ACCOUNTANT' || role === 'MASTER';
  const canAdmin = role === 'ADMIN' || role === 'MASTER';

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Profile Card */}
      <Surface style={styles.card} elevation={2}>
        <View style={styles.profileHeader}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {student.firstName.charAt(0)}
              {student.lastName.charAt(0)}
            </Text>
          </View>
          <View style={styles.profileInfo}>
            <Text variant="titleLarge" style={styles.fullName}>
              {student.firstName} {student.lastName}
            </Text>
            <Text style={styles.admNo}>Admission No: {student.admissionNo}</Text>
            <View style={styles.badgeRow}>
              <Chip icon="school" compact style={styles.chip}>
                {student.section?.schoolClass?.name || 'Class'} - {student.section?.name || 'Section'}
              </Chip>
              <Chip
                compact
                style={[
                  styles.chip,
                  student.isActive ? styles.activeChip : styles.inactiveChip,
                ]}
              >
                {student.isActive ? 'Active' : 'Withdrawn'}
              </Chip>
            </View>
          </View>
        </View>

        <Divider style={styles.divider} />

        {/* Contact info */}
        <View style={styles.infoRow}>
          <MaterialCommunityIcons name="account-child" size={18} color="#666" />
          <Text style={styles.infoLabel}>Guardian:</Text>
          <Text style={styles.infoValue}>{student.guardianName || 'N/A'}</Text>
        </View>
        <View style={styles.infoRow}>
          <MaterialCommunityIcons name="phone" size={18} color="#666" />
          <Text style={styles.infoLabel}>Phone:</Text>
          <Text style={styles.infoValue}>{student.guardianPhone || 'N/A'}</Text>
        </View>
        {student.address && (
          <View style={styles.infoRow}>
            <MaterialCommunityIcons name="map-marker" size={18} color="#666" />
            <Text style={styles.infoLabel}>Address:</Text>
            <Text style={styles.infoValue}>{student.address}</Text>
          </View>
        )}

        {/* Action Buttons for student state */}
        {canAdmin && (
          <View style={styles.studentActionRow}>
            {student.isActive ? (
              <Button
                mode="outlined"
                textColor="#D32F2F"
                onPress={() => setShowWithdrawDialog(true)}
                compact
              >
                Withdraw Student
              </Button>
            ) : (
              <Button
                mode="contained"
                buttonColor="#4CAF50"
                onPress={handleReinstate}
                compact
              >
                Reinstate Student
              </Button>
            )}
          </View>
        )}
      </Surface>

      {/* Financial Overview Card */}
      <Surface style={styles.card} elevation={2}>
        <View style={styles.feeHeader}>
          <Text variant="titleMedium" style={styles.sectionTitle}>
            Fee Summary
          </Text>
          {canManageFees && (
            <View style={{ flexDirection: 'row', gap: 8 }}>
              {totalDue > 0 && (
                <Button
                  mode="contained"
                  compact
                  onPress={() => setShowBulkPaymentModal(true)}
                  style={{ backgroundColor: '#4CAF50' }}
                >
                  Pay Bulk
                </Button>
              )}
              <Button
                mode="contained-tonal"
                icon="book-open"
                compact
                onPress={() => setShowPurchaseModal(true)}
              >
                Sell Item
              </Button>
            </View>
          )}
        </View>

        <View style={styles.summaryGrid}>
          <View style={styles.summaryBox}>
            <Text style={styles.summaryLabel}>Total Billed</Text>
            <Text style={styles.summaryAmount}>₹{formatCurrency(totalAmount)}</Text>
          </View>
          <View style={styles.summaryBox}>
            <Text style={styles.summaryLabel}>Total Paid</Text>
            <Text style={[styles.summaryAmount, { color: '#4CAF50' }]}>
              ₹{formatCurrency(totalPaid)}
            </Text>
          </View>
          <View style={styles.summaryBox}>
            <Text style={styles.summaryLabel}>Balance Due</Text>
            <Text
              style={[
                styles.summaryAmount,
                { color: totalDue > 0 ? '#E53935' : '#4CAF50' },
              ]}
            >
              ₹{formatCurrency(totalDue)}
            </Text>
          </View>
        </View>
      </Surface>

      {/* Fees List */}
      <Text variant="titleMedium" style={styles.listSectionTitle}>
        Assigned Fees & Purchases ({fees.length})
      </Text>

      {fees.length === 0 ? (
        <Surface style={styles.emptyCard} elevation={1}>
          <Text style={styles.emptyText}>No fee structures assigned</Text>
        </Surface>
      ) : (
        fees.map((fee) => {
          const paidForFee = fee.payments?.reduce((sum, p) => sum + toNum(p.amount), 0) || 0;
          const balance = Math.max(0, toNum(fee.amount) - (toNum(fee.discount) || 0) - paidForFee);
          const isPaid = fee.status === 'PAID' || balance <= 0;

          return (
            <Surface key={fee.id} style={styles.feeItemCard} elevation={1}>
              <View style={styles.feeTop}>
                <View style={{ flex: 1 }}>
                  <Text variant="titleSmall" style={styles.feeDesc}>
                    {fee.description}
                  </Text>
                  <Text style={styles.feeDue}>Due: {fee.dueDate ? fee.dueDate.substring(0, 10) : 'N/A'}</Text>
                </View>
                <Chip
                  compact
                  style={[
                    styles.feeStatusChip,
                    isPaid
                      ? styles.statusPaid
                      : fee.status === 'PARTIAL'
                      ? styles.statusPartial
                      : styles.statusUnpaid,
                  ]}
                >
                  {isPaid ? 'PAID' : fee.status}
                </Chip>
              </View>

              <View style={styles.feeNumbers}>
                <Text style={styles.numText}>Billed: ₹{formatCurrency(fee.amount)}</Text>
                <Text style={styles.numText}>Paid: ₹{formatCurrency(paidForFee)}</Text>
                <Text style={[styles.numText, { fontWeight: '700', color: balance > 0 ? '#D32F2F' : '#388E3C' }]}>
                  Balance: ₹{formatCurrency(balance)}
                </Text>
              </View>

              {/* Payments History for this fee */}
              {fee.payments && fee.payments.length > 0 && (
                <View style={styles.paymentHistoryBox}>
                  <Text style={styles.paymentHistoryHeader}>Payment Receipts:</Text>
                  {fee.payments.map((p) => (
                    <View key={p.id} style={styles.paymentHistoryRow}>
                      <View style={{ flex: 1 }}>
                        <Text style={styles.paymentHistoryText}>
                          ₹{formatCurrency(p.amount)} via {p.mode}
                          {p.referenceNo ? ` (Ref: ${p.referenceNo})` : ''}
                        </Text>
                        <Text style={styles.paymentHistoryDate}>
                          {p.paidAt ? p.paidAt.substring(0, 10) : ''}
                        </Text>
                      </View>
                      <Button
                        mode="outlined"
                        compact
                        icon="receipt"
                        onPress={() => {
                          const receipt: ReceiptData = {
                            receiptNo: p.referenceNo || `RCP-${p.id.slice(-6)}`,
                            paidAmount: toNum(p.amount),
                            mode: p.mode,
                            studentName: `${student.firstName} ${student.lastName}`,
                            admissionNo: student.admissionNo,
                            className: `${student.section?.schoolClass?.name || 'Class'} - ${
                              student.section?.name || 'Section'
                            }`,
                            items: [{ description: fee.description, amount: toNum(p.amount) }],
                            remainingDues: totalDue,
                            dateStr: p.paidAt ? p.paidAt.substring(0, 10) : undefined,
                          };
                          setActiveReceiptData(receipt);
                        }}
                      >
                        Receipt
                      </Button>
                    </View>
                  ))}
                </View>
              )}

              {canManageFees && balance > 0 && (
                <View style={styles.payBtnRow}>
                  <Button
                    mode="contained"
                    compact
                    onPress={() => setPaymentFee(fee)}
                    style={styles.payBtn}
                  >
                    Record Payment
                  </Button>
                </View>
              )}
            </Surface>
          );
        })
      )}

      {/* Record Payment Modal */}
      {paymentFee && (
        <RecordPaymentModal
          visible={!!paymentFee}
          studentFeeId={paymentFee.id}
          feeDescription={paymentFee.description}
          suggestedAmount={
            Math.max(
              0,
              toNum(paymentFee.amount) -
              (toNum(paymentFee.discount) || 0) -
              (paymentFee.payments?.reduce((s, p) => s + toNum(p.amount), 0) || 0)
            )
          }
          studentName={`${student.firstName} ${student.lastName}`}
          admissionNo={student.admissionNo}
          className={`${student.section?.schoolClass?.name || 'Class'} - ${student.section?.name || 'Section'}`}
          totalRemainingDues={totalDue}
          onDismiss={() => setPaymentFee(null)}
          onPaymentRecorded={(receipt) => {
            setPaymentFee(null);
            loadStudent();
            if (receipt) {
              setActiveReceiptData(receipt);
            }
          }}
        />
      )}

      {/* Bulk Payment Modal */}
      {showBulkPaymentModal && student && (
        <RecordBulkPaymentModal
          visible={showBulkPaymentModal}
          student={student}
          onDismiss={() => setShowBulkPaymentModal(false)}
          onPaymentRecorded={(receipt) => {
            setShowBulkPaymentModal(false);
            loadStudent();
            if (receipt) {
              setActiveReceiptData(receipt);
            }
          }}
        />
      )}

      {/* Receipt Options Modal */}
      {activeReceiptData && (
        <ReceiptOptionsModal
          visible={!!activeReceiptData}
          data={activeReceiptData}
          onDismiss={() => setActiveReceiptData(null)}
        />
      )}

      {/* Purchase Modal */}
      <PurchaseItemModal
        visible={showPurchaseModal}
        studentId={studentId}
        onDismiss={() => setShowPurchaseModal(false)}
        onPurchaseComplete={loadStudent}
      />

      {/* Withdraw Confirmation Dialog */}
      <Portal>
        <Dialog visible={showWithdrawDialog} onDismiss={() => setShowWithdrawDialog(false)}>
          <Dialog.Title>Withdraw Student</Dialog.Title>
          <Dialog.Content>
            <Text style={{ marginBottom: 12 }}>Select reason for withdrawal:</Text>
            {(['TRANSFERRED', 'GRADUATED', 'EXPELLED', 'OTHER'] as WithdrawalReason[]).map((r) => (
              <TouchableOpacity
                key={r}
                style={styles.radioRow}
                onPress={() => setWithdrawReason(r)}
              >
                <RadioButton
                  value={r}
                  status={withdrawReason === r ? 'checked' : 'unchecked'}
                  onPress={() => setWithdrawReason(r)}
                />
                <Text>{r}</Text>
              </TouchableOpacity>
            ))}
            <TextInput
              label="Notes (Optional)"
              value={withdrawNotes}
              onChangeText={setWithdrawNotes}
              mode="outlined"
              style={{ marginTop: 8 }}
            />
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setShowWithdrawDialog(false)}>Cancel</Button>
            <Button textColor="#D32F2F" onPress={handleWithdraw}>
              Confirm Withdraw
            </Button>
          </Dialog.Actions>
        </Dialog>
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
  profileHeader: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatar: {
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#E8EAF6',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  avatarText: {
    color: '#3F51B5',
    fontWeight: '700',
    fontSize: 20,
  },
  profileInfo: {
    flex: 1,
  },
  fullName: {
    fontWeight: '700',
    color: '#212121',
  },
  admNo: {
    color: '#666',
    fontSize: 13,
    marginTop: 2,
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 6,
    marginTop: 6,
  },
  chip: {
    height: 28,
  },
  activeChip: {
    backgroundColor: '#E8F5E9',
  },
  inactiveChip: {
    backgroundColor: '#FFEBEE',
  },
  divider: {
    marginVertical: 12,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
    gap: 8,
  },
  infoLabel: {
    fontSize: 13,
    color: '#666',
    width: 75,
  },
  infoValue: {
    fontSize: 13,
    color: '#212121',
    fontWeight: '500',
    flex: 1,
  },
  studentActionRow: {
    marginTop: 12,
    alignItems: 'flex-start',
  },
  feeHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: {
    fontWeight: '700',
    color: '#1A237E',
  },
  summaryGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    backgroundColor: '#F5F5F7',
    borderRadius: 8,
    padding: 12,
  },
  summaryBox: {
    alignItems: 'center',
  },
  summaryLabel: {
    fontSize: 12,
    color: '#666',
  },
  summaryAmount: {
    fontSize: 16,
    fontWeight: '700',
    color: '#212121',
    marginTop: 4,
  },
  listSectionTitle: {
    fontWeight: '700',
    color: '#424242',
    marginBottom: 10,
    marginTop: 8,
  },
  emptyCard: {
    padding: 24,
    backgroundColor: '#fff',
    borderRadius: 12,
    alignItems: 'center',
  },
  emptyText: {
    color: '#888',
  },
  feeItemCard: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 14,
    marginBottom: 10,
  },
  feeTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  feeDesc: {
    fontWeight: '600',
    color: '#212121',
  },
  feeDue: {
    fontSize: 12,
    color: '#888',
    marginTop: 2,
  },
  feeStatusChip: {
    height: 26,
  },
  statusPaid: {
    backgroundColor: '#E8F5E9',
  },
  statusPartial: {
    backgroundColor: '#FFF3E0',
  },
  statusUnpaid: {
    backgroundColor: '#FFEBEE',
  },
  feeNumbers: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#F0F0F0',
  },
  numText: {
    fontSize: 13,
    color: '#555',
  },
  payBtnRow: {
    marginTop: 10,
    alignItems: 'flex-end',
  },
  payBtn: {
    backgroundColor: '#3F51B5',
    borderRadius: 6,
  },
  radioRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 4,
  },
  paymentHistoryBox: {
    marginTop: 10,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#F0F0F0',
  },
  paymentHistoryHeader: {
    fontSize: 12,
    fontWeight: '700',
    color: '#666',
    marginBottom: 6,
  },
  paymentHistoryRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#FAFAFA',
    padding: 8,
    borderRadius: 6,
    marginBottom: 4,
  },
  paymentHistoryText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333',
  },
  paymentHistoryDate: {
    fontSize: 11,
    color: '#888',
    marginTop: 1,
  },
});

export default StudentDetailScreen;
