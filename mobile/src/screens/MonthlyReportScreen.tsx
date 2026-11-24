import React, { useState, useEffect, useCallback } from 'react';
import { View, StyleSheet, ScrollView, RefreshControl } from 'react-native';
import { Text, Surface, Divider, Button, Menu, ActivityIndicator } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { reportApi } from '../api/reports';
import { MonthlyReport } from '../types';
import { formatCurrency } from '../utils/format';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

export const MonthlyReportScreen: React.FC = () => {
  const now = new Date();
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [report, setReport] = useState<MonthlyReport | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [monthMenuVisible, setMonthMenuVisible] = useState(false);

  const loadReport = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await reportApi.getMonthlyReport(selectedMonth, selectedYear);
      setReport(data);
    } catch {
      // ignore
    } finally {
      setIsLoading(false);
    }
  }, [selectedMonth, selectedYear]);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={isLoading} onRefresh={loadReport} />}
    >
      {/* Month & Year Header Selector */}
      <View style={styles.headerRow}>
        <Menu
          visible={monthMenuVisible}
          onDismiss={() => setMonthMenuVisible(false)}
          anchor={
            <Button
              mode="contained-tonal"
              icon="calendar"
              onPress={() => setMonthMenuVisible(true)}
            >
              {MONTH_NAMES[selectedMonth - 1]} {selectedYear}
            </Button>
          }
        >
          {MONTH_NAMES.map((name, index) => (
            <Menu.Item
              key={name}
              title={`${name} ${selectedYear}`}
              onPress={() => {
                setSelectedMonth(index + 1);
                setMonthMenuVisible(false);
              }}
            />
          ))}
        </Menu>
      </View>

      {/* Main Net Result Card */}
      {report && (
        <>
          <Surface
            style={[
              styles.netCard,
              { borderLeftColor: report.netForMonth >= 0 ? '#4CAF50' : '#E53935' },
            ]}
            elevation={2}
          >
            <Text style={styles.netSubtitle}>Net for the Month (Cash Basis)</Text>
            <Text
              style={[
                styles.netAmount,
                { color: report.netForMonth >= 0 ? '#2E7D32' : '#C62828' },
              ]}
            >
              ₹{formatCurrency(report.netForMonth)}
            </Text>
            <Text style={styles.disclaimer}>
              Cash collected minus expenses and salaries paid in this calendar period.
            </Text>
          </Surface>

          {/* Breakdown Cards */}
          <Surface style={styles.card} elevation={1}>
            <Text variant="titleMedium" style={styles.sectionTitle}>
              Cash Inflow
            </Text>
            <View style={styles.row}>
              <View style={styles.rowLeft}>
                <MaterialCommunityIcons name="cash-plus" size={20} color="#4CAF50" />
                <Text style={styles.rowLabel}>Total Cash Collected</Text>
              </View>
              <Text style={[styles.rowVal, { color: '#2E7D32' }]}>
                +₹{formatCurrency(report.cashCollected)}
              </Text>
            </View>

            {report.itemSalesRevenue !== undefined && report.itemSalesRevenue > 0 && (
              <View style={[styles.row, { marginTop: 6 }]}>
                <View style={styles.rowLeft}>
                  <MaterialCommunityIcons name="book-check" size={18} color="#666" />
                  <Text style={styles.rowSubLabel}>Books/Uniforms Revenue</Text>
                </View>
                <Text style={styles.rowSubVal}>₹{formatCurrency(report.itemSalesRevenue)}</Text>
              </View>
            )}
          </Surface>

          <Surface style={styles.card} elevation={1}>
            <Text variant="titleMedium" style={styles.sectionTitle}>
              Cash Outflow
            </Text>
            <View style={styles.row}>
              <View style={styles.rowLeft}>
                <MaterialCommunityIcons name="cash-minus" size={20} color="#E53935" />
                <Text style={styles.rowLabel}>Operational Expenses</Text>
              </View>
              <Text style={[styles.rowVal, { color: '#C62828' }]}>
                -₹{formatCurrency(report.totalExpenses)}
              </Text>
            </View>

            <Divider style={styles.innerDivider} />

            <View style={styles.row}>
              <View style={styles.rowLeft}>
                <MaterialCommunityIcons name="account-cash" size={20} color="#FF9800" />
                <Text style={styles.rowLabel}>Staff Salaries Paid</Text>
              </View>
              <Text style={[styles.rowVal, { color: '#C62828' }]}>
                -₹{formatCurrency(report.totalSalaries)}
              </Text>
            </View>
          </Surface>

          {/* Expense categories breakdown */}
          {report.expensesByCategory && report.expensesByCategory.length > 0 && (
            <Surface style={styles.card} elevation={1}>
              <Text variant="titleMedium" style={styles.sectionTitle}>
                Expenses by Category
              </Text>
              {report.expensesByCategory.map((cat, idx) => (
                <View key={idx} style={styles.row}>
                  <Text style={styles.rowLabel}>{cat.name}</Text>
                  <Text style={styles.rowVal}>₹{formatCurrency(cat.total)}</Text>
                </View>
              ))}
            </Surface>
          )}
        </>
      )}
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
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    marginBottom: 16,
  },
  netCard: {
    padding: 18,
    borderRadius: 14,
    backgroundColor: '#fff',
    borderLeftWidth: 6,
    marginBottom: 16,
  },
  netSubtitle: {
    fontSize: 13,
    color: '#666',
  },
  netAmount: {
    fontSize: 30,
    fontWeight: '700',
    marginVertical: 6,
  },
  disclaimer: {
    fontSize: 11,
    color: '#888',
    fontStyle: 'italic',
  },
  card: {
    padding: 16,
    borderRadius: 12,
    backgroundColor: '#fff',
    marginBottom: 14,
  },
  sectionTitle: {
    fontWeight: '700',
    color: '#212121',
    marginBottom: 12,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginVertical: 4,
  },
  rowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  rowLabel: {
    fontSize: 14,
    color: '#424242',
    fontWeight: '500',
  },
  rowVal: {
    fontSize: 15,
    fontWeight: '700',
  },
  rowSubLabel: {
    fontSize: 12,
    color: '#777',
  },
  rowSubVal: {
    fontSize: 13,
    color: '#666',
  },
  innerDivider: {
    marginVertical: 8,
  },
});

export default MonthlyReportScreen;
