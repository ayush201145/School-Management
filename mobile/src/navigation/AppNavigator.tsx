import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { RootStackParamList } from './types';
import { useAuthStore } from '../stores/authStore';

// Screens
import LoginScreen from '../screens/LoginScreen';
import DashboardScreen from '../screens/DashboardScreen';
import StudentListScreen from '../screens/StudentListScreen';
import StudentDetailScreen from '../screens/StudentDetailScreen';
import TeacherListScreen from '../screens/TeacherListScreen';
import DuesScreen from '../screens/DuesScreen';
import TransactionsScreen from '../screens/TransactionsScreen';
import InventoryScreen from '../screens/InventoryScreen';
import FeeStructuresScreen from '../screens/FeeStructuresScreen';
import StaffListScreen from '../screens/StaffListScreen';
import StaffDetailScreen from '../screens/StaffDetailScreen';
import ExpensesScreen from '../screens/ExpensesScreen';
import MonthlyReportScreen from '../screens/MonthlyReportScreen';
import AttendanceScreen from '../screens/AttendanceScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

export const AppNavigator: React.FC = () => {
  const { isAuthenticated } = useAuthStore();

  return (
    <Stack.Navigator
      initialRouteName={isAuthenticated ? 'Dashboard' : 'Login'}
      screenOptions={{
        headerStyle: {
          backgroundColor: '#3F51B5',
        },
        headerTintColor: '#fff',
        headerTitleStyle: {
          fontWeight: '600',
        },
      }}
    >
      {!isAuthenticated ? (
        <Stack.Screen
          name="Login"
          component={LoginScreen}
          options={{ headerShown: false }}
        />
      ) : (
        <>
          <Stack.Screen
            name="Dashboard"
            component={DashboardScreen}
            options={{ title: 'Dashboard' }}
          />
          <Stack.Screen
            name="StudentList"
            component={StudentListScreen}
            options={{ title: 'Students' }}
          />
          <Stack.Screen
            name="StudentDetail"
            component={StudentDetailScreen}
            options={{ title: 'Student Profile' }}
          />
          <Stack.Screen
            name="TeacherList"
            component={TeacherListScreen}
            options={{ title: 'Teachers' }}
          />
          <Stack.Screen
            name="DuesReport"
            component={DuesScreen}
            options={{ title: 'Fee Dues' }}
          />
          <Stack.Screen
            name="Transactions"
            component={TransactionsScreen}
            options={{ title: 'Transactions Ledger' }}
          />
          <Stack.Screen
            name="Inventory"
            component={InventoryScreen}
            options={{ title: 'Inventory' }}
          />
          <Stack.Screen
            name="FeeStructures"
            component={FeeStructuresScreen}
            options={{ title: 'Fee Structures' }}
          />
          <Stack.Screen
            name="StaffList"
            component={StaffListScreen}
            options={{ title: 'Staff Directory' }}
          />
          <Stack.Screen
            name="StaffDetail"
            component={StaffDetailScreen}
            options={{ title: 'Staff Detail' }}
          />
          <Stack.Screen
            name="Expenses"
            component={ExpensesScreen}
            options={{ title: 'Expenses' }}
          />
          <Stack.Screen
            name="MonthlyReport"
            component={MonthlyReportScreen}
            options={{ title: 'Monthly Financial Report' }}
          />
          <Stack.Screen
            name="Attendance"
            component={AttendanceScreen}
            options={{ title: 'Attendance' }}
          />
        </>
      )}
    </Stack.Navigator>
  );
};

export default AppNavigator;
