import React, { useState } from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Modal, Portal, Text, TextInput, Button, SegmentedButtons, HelperText } from 'react-native-paper';
import { feeApi } from '../api/fees';
import { PaymentMode, ReceiptData } from '../types';

interface Props {
  visible: boolean;
  studentFeeId: string;
  feeDescription: string;
  suggestedAmount: number;
  studentName?: string;
  admissionNo?: string;
  className?: string;
  totalRemainingDues?: number;
  onDismiss: () => void;
  onPaymentRecorded: (receipt?: ReceiptData) => void;
}

const PAYMENT_MODES: PaymentMode[] = ['CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'CARD', 'OTHER'];

export const RecordPaymentModal: React.FC<Props> = ({
  visible,
  studentFeeId,
  feeDescription,
  suggestedAmount,
  studentName,
  admissionNo,
  className,
  totalRemainingDues,
  onDismiss,
  onPaymentRecorded,
}) => {
  const [amount, setAmount] = useState(suggestedAmount > 0 ? suggestedAmount.toString() : '');
  const [mode, setMode] = useState<PaymentMode>('CASH');
  const [referenceNo, setReferenceNo] = useState('');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  React.useEffect(() => {
    if (visible) {
      setAmount(suggestedAmount > 0 ? suggestedAmount.toString() : '');
      setMode('CASH');
      setReferenceNo('');
      setNotes('');
      setError(null);
    }
  }, [visible, suggestedAmount]);

  const handleSubmit = async () => {
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid amount');
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);
      const res = await feeApi.recordPayment(studentFeeId, {
        amount: numAmount,
        mode,
        referenceNo: referenceNo.trim() || undefined,
        notes: notes.trim() || undefined,
      });

      const receiptNo = referenceNo.trim() || `RCP-${Date.now().toString().slice(-6)}`;
      const receipt: ReceiptData = {
        receiptNo,
        paidAmount: numAmount,
        mode,
        studentName: studentName || 'Student',
        admissionNo: admissionNo || '',
        className: className || '',
        items: [{ description: feeDescription, amount: numAmount }],
        remainingDues: Math.max(0, (totalRemainingDues || 0) - numAmount),
      };

      onDismiss();
      onPaymentRecorded(receipt);
    } catch (err: any) {
      setError(err.message || 'Failed to record payment');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Portal>
      <Modal visible={visible} onDismiss={onDismiss} contentContainerStyle={styles.modalContent}>
        <ScrollView>
          <Text variant="titleLarge" style={styles.title}>
            Record Payment
          </Text>
          <Text variant="bodyMedium" style={styles.feeDesc}>
            {feeDescription}
          </Text>

          {error && <HelperText type="error" visible={true}>{error}</HelperText>}

          <TextInput
            label="Amount (₹) *"
            value={amount}
            onChangeText={setAmount}
            keyboardType="decimal-pad"
            mode="outlined"
            style={styles.input}
          />

          <Text variant="labelMedium" style={styles.label}>
            Payment Mode
          </Text>
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
            multiline
            numberOfLines={2}
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
              disabled={isSubmitting}
              style={{ backgroundColor: '#4CAF50' }}
            >
              Confirm Payment
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
    borderRadius: 12,
    maxHeight: '85%',
    padding: 20,
  },
  title: {
    fontWeight: '700',
    color: '#1A237E',
  },
  feeDesc: {
    color: '#666',
    marginTop: 4,
    marginBottom: 16,
  },
  label: {
    color: '#555',
    marginBottom: 6,
    marginTop: 6,
  },
  modeContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 16,
  },
  modeBtn: {
    borderRadius: 6,
  },
  input: {
    marginBottom: 12,
    backgroundColor: '#fff',
  },
  actions: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginTop: 16,
  },
});

export default RecordPaymentModal;
