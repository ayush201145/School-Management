import React, { useState } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Modal, Portal, Text, TextInput, Button, HelperText } from 'react-native-paper';
import { feeApi } from '../api/fees';
import { Student, StudentFee, PaymentMode, ReceiptData, ReceiptItem } from '../types';
import { formatCurrency, toNum } from '../utils/format';

interface Props {
  visible: boolean;
  student: Student;
  onDismiss: () => void;
  onPaymentRecorded: (receipt: ReceiptData) => void;
}

const PAYMENT_MODES: PaymentMode[] = ['CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'CARD', 'OTHER'];

export const RecordBulkPaymentModal: React.FC<Props> = ({
  visible,
  student,
  onDismiss,
  onPaymentRecorded,
}) => {
  const fees = student.studentFees || [];

  // Calculate unpaid fees and total outstanding
  const unpaidFees = fees
    .map((f) => {
      const alreadyPaid = f.payments?.reduce((sum, p) => sum + toNum(p.amount), 0) || 0;
      const payable = toNum(f.amount) - (toNum(f.discount) || 0);
      const balance = Math.max(0, payable - alreadyPaid);
      return { fee: f, balance, payable, alreadyPaid };
    })
    .filter((f) => f.balance > 0)
    .sort((a, b) => new Date(a.fee.dueDate).getTime() - new Date(b.fee.dueDate).getTime()); // FIFO (oldest first)

  const totalOutstanding = unpaidFees.reduce((sum, f) => sum + f.balance, 0);

  const [amount, setAmount] = useState(totalOutstanding > 0 ? totalOutstanding.toString() : '');
  const [mode, setMode] = useState<PaymentMode>('CASH');
  const [referenceNo, setReferenceNo] = useState('');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  React.useEffect(() => {
    if (visible) {
      setAmount(totalOutstanding > 0 ? totalOutstanding.toString() : '');
      setMode('CASH');
      setReferenceNo('');
      setNotes('');
      setError(null);
    }
  }, [visible, totalOutstanding]);

  const handleSubmit = async () => {
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid amount greater than 0');
      return;
    }

    if (numAmount > totalOutstanding + 0.01) {
      setError(`Amount exceeds total outstanding balance of ₹${formatCurrency(totalOutstanding)}`);
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);

      let remainingToAllocate = numAmount;
      const itemsPaid: ReceiptItem[] = [];

      // Allocate in FIFO order
      for (const item of unpaidFees) {
        if (remainingToAllocate <= 0.001) break;

        const allocation = Math.min(item.balance, remainingToAllocate);
        await feeApi.recordPayment(item.fee.id, {
          amount: allocation,
          mode,
          referenceNo: referenceNo.trim() || undefined,
          notes: notes.trim() || 'Bulk payment',
        });

        itemsPaid.push({
          description: item.fee.description,
          amount: allocation,
        });

        remainingToAllocate -= allocation;
      }

      const receiptNo = referenceNo.trim() || `RCP-${Date.now().toString().slice(-6)}`;
      const remainingDues = Math.max(0, totalOutstanding - numAmount);

      const receipt: ReceiptData = {
        receiptNo,
        paidAmount: numAmount,
        mode,
        studentName: `${student.firstName} ${student.lastName}`,
        admissionNo: student.admissionNo,
        className: `${student.section?.schoolClass?.name || 'Class'} - ${student.section?.name || 'Section'}`,
        items: itemsPaid,
        remainingDues,
      };

      onDismiss();
      onPaymentRecorded(receipt);
    } catch (err: any) {
      setError(err.message || 'Failed to record bulk payment');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Portal>
      <Modal visible={visible} onDismiss={onDismiss} contentContainerStyle={styles.modalContent}>
        <ScrollView>
          <Text variant="titleLarge" style={styles.title}>
            Pay Multiple Fees (Bulk)
          </Text>
          <Text style={styles.subtitle}>
            Payments are automatically allocated to the oldest unpaid fees first (FIFO order).
          </Text>

          <View style={styles.dueSummary}>
            <Text style={styles.dueLabel}>Total Outstanding Dues:</Text>
            <Text style={styles.dueAmount}>₹{formatCurrency(totalOutstanding)}</Text>
          </View>

          {error && <HelperText type="error" visible={true}>{error}</HelperText>}

          <TextInput
            label="Total Amount Paid (₹) *"
            value={amount}
            onChangeText={setAmount}
            keyboardType="decimal-pad"
            mode="outlined"
            style={styles.input}
          />

          <Text style={styles.label}>Payment Mode</Text>
          <View style={styles.modeContainer}>
            {PAYMENT_MODES.map((m) => (
              <Button
                key={m}
                mode={mode === m ? 'contained' : 'outlined'}
                onPress={() => setMode(m)}
                style={styles.modeBtn}
                compact
              >
                {m}
              </Button>
            ))}
          </View>

          <TextInput
            label="Reference No / Cheque No / UPI Ref"
            value={referenceNo}
            onChangeText={setReferenceNo}
            mode="outlined"
            style={styles.input}
          />

          <TextInput
            label="Notes"
            value={notes}
            onChangeText={setNotes}
            mode="outlined"
            style={styles.input}
          />

          <View style={styles.actions}>
            <Button mode="text" onPress={onDismiss}>
              Cancel
            </Button>
            <Button
              mode="contained"
              onPress={handleSubmit}
              loading={isSubmitting}
              disabled={isSubmitting || totalOutstanding <= 0}
              style={{ backgroundColor: '#4CAF50' }}
            >
              Confirm Bulk Payment
            </Button>
          </View>
        </ScrollView>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 14,
    padding: 20,
    maxHeight: '85%',
  },
  title: {
    fontWeight: '700',
    color: '#1A237E',
  },
  subtitle: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
    marginBottom: 12,
  },
  dueSummary: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#FFEBEE',
    padding: 10,
    borderRadius: 8,
    marginBottom: 12,
  },
  dueLabel: {
    fontSize: 13,
    color: '#C62828',
    fontWeight: '600',
  },
  dueAmount: {
    fontSize: 16,
    color: '#C62828',
    fontWeight: '700',
  },
  label: {
    fontSize: 12,
    color: '#666',
    marginBottom: 6,
    marginTop: 4,
  },
  modeContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginBottom: 12,
  },
  modeBtn: {
    borderRadius: 6,
  },
  input: {
    marginBottom: 10,
    backgroundColor: '#fff',
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 14,
  },
});

export default RecordBulkPaymentModal;
