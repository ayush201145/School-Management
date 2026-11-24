import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl } from 'react-native';
import { Text, Surface, Divider, Chip } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { feeApi } from '../api/fees';
import { Payment, PaymentMode } from '../types';
import { formatCurrency } from '../utils/format';

const MODES: PaymentMode[] = ['CASH', 'UPI', 'CHEQUE', 'BANK_TRANSFER', 'CARD', 'OTHER'];

export const TransactionsScreen: React.FC = () => {
  const [transactions, setTransactions] = useState<Payment[]>([]);
  const [totalAmount, setTotalAmount] = useState(0);
  const [selectedMode, setSelectedMode] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const loadTransactions = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await feeApi.listTransactions({
        mode: selectedMode || undefined,
      });
      setTransactions(res.transactions || []);
      setTotalAmount(res.totalAmount || 0);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedMode]);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  return (
    <View style={styles.container}>
      {/* Summary Card */}
      <Surface style={styles.card} elevation={2}>
        <Text style={styles.cardSubtitle}>Total Collections</Text>
        <Text style={styles.totalAmount}>₹{formatCurrency(totalAmount)}</Text>
        <Text style={styles.countText}>{transactions.length} recorded transaction(s)</Text>
      </Surface>

      {/* Mode Filters */}
      <View style={styles.filterRow}>
        <Chip
          selected={selectedMode === null}
          onPress={() => setSelectedMode(null)}
          style={styles.filterChip}
        >
          All Modes
        </Chip>
        {MODES.map((m) => (
          <Chip
            key={m}
            selected={selectedMode === m}
            onPress={() => setSelectedMode(selectedMode === m ? null : m)}
            style={styles.filterChip}
          >
            {m}
          </Chip>
        ))}
      </View>

      {/* Transactions List */}
      <FlatList
        data={transactions}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadTransactions} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="receipt" size={48} color="#999" />
              <Text style={styles.emptyText}>No transactions recorded</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <Surface style={styles.itemCard} elevation={1}>
            <View style={styles.itemRow}>
              <View style={styles.modeIcon}>
                <MaterialCommunityIcons
                  name={item.mode === 'UPI' ? 'cellphone' : item.mode === 'CASH' ? 'cash' : 'bank'}
                  size={24}
                  color="#009688"
                />
              </View>
              <View style={styles.detailsGroup}>
                <Text variant="titleMedium" style={styles.studentName}>
                  {item.studentFee?.student
                    ? `${item.studentFee.student.firstName} ${item.studentFee.student.lastName}`
                    : 'Payment'}
                </Text>
                {item.studentFee?.description && (
                  <Text style={styles.feeDesc}>{item.studentFee.description}</Text>
                )}
                <View style={styles.metaRow}>
                  <Text style={styles.modeBadge}>{item.mode}</Text>
                  {item.referenceNo && (
                    <Text style={styles.refText}>Ref: {item.referenceNo}</Text>
                  )}
                  <Text style={styles.dateText}>
                    {item.paidAt ? item.paidAt.substring(0, 10) : ''}
                  </Text>
                </View>
              </View>
              <Text style={styles.amount}>+₹{formatCurrency(item.amount)}</Text>
            </View>
          </Surface>
        )}
      />
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
    borderLeftColor: '#009688',
  },
  cardSubtitle: {
    fontSize: 13,
    color: '#666',
  },
  totalAmount: {
    fontSize: 26,
    fontWeight: '700',
    color: '#00796B',
    marginVertical: 4,
  },
  countText: {
    fontSize: 12,
    color: '#888',
  },
  filterRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 8,
    flexWrap: 'wrap',
    gap: 6,
  },
  filterChip: {
    marginRight: 4,
  },
  listContent: {
    padding: 16,
    paddingTop: 8,
    paddingBottom: 40,
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
    alignItems: 'center',
  },
  modeIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#E0F2F1',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  detailsGroup: {
    flex: 1,
  },
  studentName: {
    fontWeight: '600',
    color: '#212121',
  },
  feeDesc: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginTop: 4,
  },
  modeBadge: {
    fontSize: 11,
    fontWeight: '700',
    color: '#00796B',
    backgroundColor: '#E0F2F1',
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 4,
  },
  refText: {
    fontSize: 11,
    color: '#777',
  },
  dateText: {
    fontSize: 11,
    color: '#999',
  },
  amount: {
    fontSize: 16,
    fontWeight: '700',
    color: '#388E3C',
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
});

export default TransactionsScreen;
