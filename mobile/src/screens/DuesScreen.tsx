import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, FlatList, RefreshControl, TouchableOpacity } from 'react-native';
import { Text, Surface, Chip, Divider, ActivityIndicator } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/types';
import { feeApi } from '../api/fees';
import { useAcademicStore } from '../stores/academicStore';
import { DueItem } from '../types';
import { formatCurrency } from '../utils/format';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'DuesReport'>;

export const DuesScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { classes, fetchClasses } = useAcademicStore();

  const [dues, setDues] = useState<DueItem[]>([]);
  const [totalOutstanding, setTotalOutstanding] = useState(0);
  const [selectedClassId, setSelectedClassId] = useState<string | null>(null);
  const [overdueOnly, setOverdueOnly] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    fetchClasses();
  }, []);

  const loadDues = useCallback(async () => {
    try {
      setIsLoading(true);
      const res = await feeApi.listDues({
        classId: selectedClassId || undefined,
        overdueOnly: overdueOnly || undefined,
      });
      setDues(res.dues || []);
      setTotalOutstanding(res.totalOutstanding || 0);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedClassId, overdueOnly]);

  useEffect(() => {
    loadDues();
  }, [loadDues]);

  return (
    <View style={styles.container}>
      {/* Overview Card */}
      <Surface style={styles.card} elevation={2}>
        <Text style={styles.cardSubtitle}>Total Outstanding Dues</Text>
        <Text style={styles.totalAmount}>₹{formatCurrency(totalOutstanding)}</Text>
        <Text style={styles.countText}>{dues.length} pending fee bill(s)</Text>
      </Surface>

      {/* Filter Row */}
      <View style={styles.filterRow}>
        <Chip
          selected={overdueOnly}
          onPress={() => setOverdueOnly(!overdueOnly)}
          style={[styles.filterChip, overdueOnly ? styles.overdueActiveChip : null]}
        >
          Overdue Only
        </Chip>
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
        data={dues}
        keyExtractor={(item) => item.id}
        refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadDues} />}
        contentContainerStyle={styles.listContent}
        ItemSeparatorComponent={() => <Divider style={styles.divider} />}
        ListEmptyComponent={
          !isLoading ? (
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="check-circle-outline" size={48} color="#4CAF50" />
              <Text style={styles.emptyText}>No pending dues found!</Text>
            </View>
          ) : undefined
        }
        renderItem={({ item }) => (
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={() => navigation.navigate('StudentDetail', { studentId: item.student.id })}
          >
            <Surface style={styles.itemCard} elevation={1}>
              <View style={styles.itemHeader}>
                <View style={{ flex: 1 }}>
                  <Text variant="titleMedium" style={styles.studentName}>
                    {item.student.firstName} {item.student.lastName}
                  </Text>
                  <Text style={styles.admNo}>Adm: {item.student.admissionNo}</Text>
                </View>
                <View style={styles.balanceBadge}>
                  <Text style={styles.balanceText}>₹{formatCurrency(item.balance)}</Text>
                </View>
              </View>

              <Divider style={{ marginVertical: 8 }} />

              <View style={styles.feeDetails}>
                <Text style={styles.feeDesc}>{item.description}</Text>
                <Text style={styles.dueDate}>Due: {item.dueDate ? item.dueDate.substring(0, 10) : 'N/A'}</Text>
              </View>

              <View style={styles.itemFooter}>
                <Text style={styles.footerNumber}>Billed: ₹{formatCurrency(item.amount)}</Text>
                <Text style={styles.footerNumber}>Paid: ₹{formatCurrency(item.paid)}</Text>
                <Text style={[styles.statusText, { color: item.status === 'PARTIAL' ? '#FB8C00' : '#E53935' }]}>
                  {item.status}
                </Text>
              </View>
            </Surface>
          </TouchableOpacity>
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
    borderLeftColor: '#FF5722',
  },
  cardSubtitle: {
    fontSize: 13,
    color: '#666',
  },
  totalAmount: {
    fontSize: 26,
    fontWeight: '700',
    color: '#D84315',
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
  overdueActiveChip: {
    backgroundColor: '#FFEBEE',
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
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  studentName: {
    fontWeight: '700',
    color: '#212121',
  },
  admNo: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  balanceBadge: {
    backgroundColor: '#FFEBEE',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 6,
  },
  balanceText: {
    color: '#D32F2F',
    fontWeight: '700',
    fontSize: 15,
  },
  feeDetails: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  feeDesc: {
    color: '#424242',
    fontWeight: '500',
    flex: 1,
  },
  dueDate: {
    fontSize: 12,
    color: '#888',
  },
  itemFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 8,
    paddingTop: 6,
    borderTopWidth: 1,
    borderTopColor: '#F5F5F5',
  },
  footerNumber: {
    fontSize: 12,
    color: '#666',
  },
  statusText: {
    fontSize: 12,
    fontWeight: '700',
  },
  emptyContainer: {
    alignItems: 'center',
    paddingTop: 60,
  },
  emptyText: {
    marginTop: 10,
    fontSize: 15,
    color: '#4CAF50',
    fontWeight: '600',
  },
});

export default DuesScreen;
