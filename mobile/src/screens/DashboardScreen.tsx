import React, { useEffect, useLayoutEffect } from 'react';
import { View, StyleSheet, ScrollView, TouchableOpacity, Dimensions } from 'react-native';
import { Text, Surface, IconButton, Menu, Divider } from 'react-native-paper';
import { LinearGradient } from 'expo-linear-gradient';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../navigation/types';
import { useAuthStore } from '../stores/authStore';
import { useAcademicStore } from '../stores/academicStore';
import { UserRole } from '../types';

type NavigationProp = NativeStackNavigationProp<RootStackParamList, 'Dashboard'>;

interface DashboardTileConfig {
  label: string;
  icon: keyof typeof MaterialCommunityIcons.glyphMap;
  visibleTo: UserRole[];
  gradient: [string, string];
  screen: keyof RootStackParamList;
}

const ALL_TILES: DashboardTileConfig[] = [
  {
    label: 'Students',
    icon: 'account-group',
    visibleTo: ['ADMIN', 'ACCOUNTANT', 'TEACHER', 'MASTER'],
    gradient: ['#3F51B5', '#2196F3'],
    screen: 'StudentList',
  },
  {
    label: 'Teachers',
    icon: 'account-tie',
    visibleTo: ['ADMIN', 'MASTER'],
    gradient: ['#9C27B0', '#E91E63'],
    screen: 'TeacherList',
  },
  {
    label: 'Fee Dues',
    icon: 'format-list-checks',
    visibleTo: ['ADMIN', 'ACCOUNTANT', 'MASTER'],
    gradient: ['#FF5722', '#FF9800'],
    screen: 'DuesReport',
  },
  {
    label: 'Transactions',
    icon: 'receipt',
    visibleTo: ['ADMIN', 'ACCOUNTANT', 'MASTER'],
    gradient: ['#009688', '#4CAF50'],
    screen: 'Transactions',
  },
  {
    label: 'Inventory',
    icon: 'package-variant-closed',
    visibleTo: ['ADMIN', 'ACCOUNTANT', 'MASTER'],
    gradient: ['#607D8B', '#90A4AE'],
    screen: 'Inventory',
  },
  {
    label: 'Fee Structures',
    icon: 'bank',
    visibleTo: ['ADMIN', 'MASTER'],
    gradient: ['#673AB7', '#3F51B5'],
    screen: 'FeeStructures',
  },
  {
    label: 'Staff',
    icon: 'card-account-details',
    visibleTo: ['ADMIN', 'MASTER'],
    gradient: ['#795548', '#A1887F'],
    screen: 'StaffList',
  },
  {
    label: 'Expenses',
    icon: 'cash-minus',
    visibleTo: ['ADMIN', 'ACCOUNTANT', 'MASTER'],
    gradient: ['#E53935', '#D81B60'],
    screen: 'Expenses',
  },
  {
    label: 'Monthly Report',
    icon: 'chart-bar',
    visibleTo: ['ADMIN', 'ACCOUNTANT', 'MASTER'],
    gradient: ['#00ACC1', '#00838F'],
    screen: 'MonthlyReport',
  },
  {
    label: 'Attendance',
    icon: 'calendar-check',
    visibleTo: ['ADMIN', 'TEACHER', 'MASTER'],
    gradient: ['#4CAF50', '#8BC34A'],
    screen: 'Attendance',
  },
];

export const DashboardScreen: React.FC = () => {
  const navigation = useNavigation<NavigationProp>();
  const { user, role, logout } = useAuthStore();
  const { academicYears, selectedYearId, fetchAcademicYears, selectYear } = useAcademicStore();
  const [yearMenuVisible, setYearMenuVisible] = React.useState(false);

  useEffect(() => {
    fetchAcademicYears();
  }, []);

  useLayoutEffect(() => {
    navigation.setOptions({
      headerRight: () => (
        <View style={styles.headerRight}>
          <IconButton
            icon="logout"
            iconColor="#fff"
            size={22}
            onPress={logout}
          />
        </View>
      ),
    });
  }, [navigation, logout]);

  const activeYear =
    academicYears.find((y) => y.id === selectedYearId) ||
    academicYears.find((y) => y.isCurrent) ||
    academicYears[0];

  const visibleTiles = ALL_TILES.filter(
    (tile) => role && tile.visibleTo.includes(role)
  );

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      {/* Welcome Banner */}
      <Surface style={styles.banner} elevation={2}>
        <View style={styles.bannerRow}>
          <View style={styles.welcomeTextGroup}>
            <Text variant="titleMedium" style={styles.welcomeTitle}>
              Welcome back, {user?.username || 'User'}!
            </Text>
            <Text variant="bodySmall" style={styles.roleBadge}>
              Role: {role || 'Staff'}
            </Text>
          </View>

          {/* Academic Year Selector */}
          {academicYears.length > 0 && (
            <Menu
              visible={yearMenuVisible}
              onDismiss={() => setYearMenuVisible(false)}
              anchor={
                <TouchableOpacity
                  style={styles.yearButton}
                  onPress={() => setYearMenuVisible(true)}
                >
                  <Text style={styles.yearButtonText}>
                    {activeYear ? activeYear.label : 'Select Year'}
                  </Text>
                  <MaterialCommunityIcons name="chevron-down" size={16} color="#3F51B5" />
                </TouchableOpacity>
              }
            >
              {academicYears.map((year) => (
                <Menu.Item
                  key={year.id}
                  title={`${year.label}${year.isCurrent ? ' (Current)' : ''}`}
                  onPress={() => {
                    selectYear(year.id);
                    setYearMenuVisible(false);
                  }}
                />
              ))}
            </Menu>
          )}
        </View>
      </Surface>

      {/* Grid of Action Tiles */}
      <View style={styles.grid}>
        {visibleTiles.map((tile) => (
          <TouchableOpacity
            key={tile.label}
            style={styles.tileWrapper}
            activeOpacity={0.8}
            onPress={() => navigation.navigate(tile.screen as any)}
          >
            <LinearGradient
              colors={tile.gradient}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.tile}
            >
              <MaterialCommunityIcons name={tile.icon} size={36} color="#fff" />
              <Text variant="titleMedium" style={styles.tileLabel}>
                {tile.label}
              </Text>
            </LinearGradient>
          </TouchableOpacity>
        ))}
      </View>
    </ScrollView>
  );
};

const screenWidth = Dimensions.get('window').width;
const tileWidth = (screenWidth - 48) / 2;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F8F9FA',
  },
  content: {
    padding: 16,
    paddingBottom: 32,
  },
  headerRight: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  banner: {
    padding: 16,
    borderRadius: 12,
    backgroundColor: '#FFFFFF',
    marginBottom: 20,
  },
  bannerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 12,
  },
  welcomeTextGroup: {
    flex: 1,
  },
  welcomeTitle: {
    fontWeight: '700',
    color: '#212121',
  },
  roleBadge: {
    color: '#757575',
    marginTop: 2,
    textTransform: 'capitalize',
  },
  yearButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 6,
    paddingHorizontal: 12,
    backgroundColor: '#E8EAF6',
    borderRadius: 8,
    gap: 4,
  },
  yearButtonText: {
    color: '#3F51B5',
    fontWeight: '600',
    fontSize: 13,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 16,
    justifyContent: 'space-between',
  },
  tileWrapper: {
    width: tileWidth,
    borderRadius: 16,
    overflow: 'hidden',
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.15,
    shadowRadius: 6,
  },
  tile: {
    height: 120,
    padding: 16,
    justifyContent: 'space-between',
  },
  tileLabel: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 16,
  },
});

export default DashboardScreen;
