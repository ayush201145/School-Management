import React, { useState } from 'react';
import { View, StyleSheet, Alert } from 'react-native';
import { Modal, Portal, Text, Button, Divider, ActivityIndicator } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { ReceiptData } from '../types';
import { generateAndShareReceiptPdf, printReceiptDirect } from '../utils/receiptHelper';
import { formatCurrency } from '../utils/format';

interface Props {
  visible: boolean;
  data: ReceiptData | null;
  onDismiss: () => void;
}

export const ReceiptOptionsModal: React.FC<Props> = ({ visible, data, onDismiss }) => {
  const [isSharing, setIsSharing] = useState(false);
  const [isPrinting, setIsPrinting] = useState(false);

  if (!data) return null;

  const handleShare = async () => {
    try {
      setIsSharing(true);
      await generateAndShareReceiptPdf(data);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to generate PDF');
    } finally {
      setIsSharing(false);
    }
  };

  const handlePrint = async () => {
    try {
      setIsPrinting(true);
      await printReceiptDirect(data);
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Failed to print receipt');
    } finally {
      setIsPrinting(false);
    }
  };

  return (
    <Portal>
      <Modal visible={visible} onDismiss={onDismiss} contentContainerStyle={styles.modalContent}>
        <View style={styles.header}>
          <MaterialCommunityIcons name="check-circle" size={48} color="#4CAF50" />
          <Text variant="titleLarge" style={styles.title}>
            Payment Recorded
          </Text>
          <Text style={styles.receiptNo}>Receipt #{data.receiptNo}</Text>
        </View>

        <Divider style={styles.divider} />

        <View style={styles.detailsBox}>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Student:</Text>
            <Text style={styles.val}>{data.studentName}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Admission No:</Text>
            <Text style={styles.val}>{data.admissionNo}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Class:</Text>
            <Text style={styles.val}>{data.className}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Amount Paid:</Text>
            <Text style={[styles.val, styles.amount]}>₹{formatCurrency(data.paidAmount)}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Payment Mode:</Text>
            <Text style={styles.val}>{data.mode}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Remaining Dues:</Text>
            <Text style={[styles.val, { color: data.remainingDues > 0 ? '#D32F2F' : '#388E3C' }]}>
              ₹{formatCurrency(data.remainingDues)}
            </Text>
          </View>
        </View>

        <View style={styles.actions}>
          <Button
            mode="contained"
            icon="share-variant"
            onPress={handleShare}
            loading={isSharing}
            disabled={isSharing || isPrinting}
            style={[styles.btn, { backgroundColor: '#3F51B5' }]}
            contentStyle={styles.btnContent}
          >
            Share / Save PDF
          </Button>

          <Button
            mode="contained-tonal"
            icon="printer"
            onPress={handlePrint}
            loading={isPrinting}
            disabled={isSharing || isPrinting}
            style={styles.btn}
            contentStyle={styles.btnContent}
          >
            Print Receipt
          </Button>

          <Button mode="text" onPress={onDismiss} style={styles.closeBtn}>
            Done
          </Button>
        </View>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  modalContent: {
    backgroundColor: '#fff',
    margin: 20,
    borderRadius: 16,
    padding: 24,
  },
  header: {
    alignItems: 'center',
    marginBottom: 12,
  },
  title: {
    fontWeight: '700',
    color: '#212121',
    marginTop: 8,
  },
  receiptNo: {
    fontSize: 13,
    color: '#757575',
    marginTop: 2,
  },
  divider: {
    marginVertical: 12,
  },
  detailsBox: {
    backgroundColor: '#F8F9FA',
    borderRadius: 10,
    padding: 12,
    marginBottom: 20,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginVertical: 3,
  },
  label: {
    fontSize: 13,
    color: '#666',
  },
  val: {
    fontSize: 13,
    fontWeight: '600',
    color: '#212121',
  },
  amount: {
    color: '#2E7D32',
    fontSize: 15,
  },
  actions: {
    gap: 10,
  },
  btn: {
    borderRadius: 8,
  },
  btnContent: {
    height: 46,
  },
  closeBtn: {
    marginTop: 4,
  },
});

export default ReceiptOptionsModal;
