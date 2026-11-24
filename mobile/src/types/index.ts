export type UserRole = 'ADMIN' | 'ACCOUNTANT' | 'TEACHER' | 'MASTER';

export type FeeStatus = 'UNPAID' | 'PARTIAL' | 'PAID';

export type PaymentMode = 'CASH' | 'UPI' | 'CHEQUE' | 'BANK_TRANSFER' | 'CARD' | 'OTHER';

export type StaffType = 'TEACHER' | 'ADMIN' | 'ACCOUNTANT_STAFF' | 'PEON' | 'DRIVER' | 'OTHER';

export type WithdrawalReason = 'TRANSFERRED' | 'GRADUATED' | 'EXPELLED' | 'OTHER';

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'LEAVE';

export type ItemType = 'BOOK' | 'UNIFORM_SUMMER' | 'UNIFORM_WINTER' | 'OTHER';

export interface User {
  id: string;
  username: string;
  role: UserRole;
  teacherId?: string | null;
}

export interface LoginResponse {
  token: string;
  user: User;
}

// Academic
export interface AcademicYear {
  id: string;
  label: string;
  startDate: string;
  endDate: string;
  isCurrent: boolean;
}

export interface SchoolClass {
  id: string;
  name: string;
  academicYearId: string;
  sections?: Section[];
}

export interface Section {
  id: string;
  name: string;
  classId: string;
  schoolClass?: SchoolClass;
}

// Students
export interface Student {
  id: string;
  admissionNo: string;
  rollNo?: string | null;
  firstName: string;
  lastName: string;
  dateOfBirth?: string | null;
  gender?: string | null;
  guardianName?: string | null;
  guardianPhone?: string | null;
  guardianEmail?: string | null;
  address?: string | null;
  sectionId: string;
  admissionDate?: string | null;
  isActive: boolean;
  withdrawalReason?: WithdrawalReason | null;
  withdrawnDate?: string | null;
  withdrawalNotes?: string | null;
  section?: Section & { schoolClass?: SchoolClass };
  studentFees?: StudentFee[];
}

export interface CreateStudentInput {
  admissionNo: string;
  rollNo?: string;
  firstName: string;
  lastName: string;
  dateOfBirth?: string;
  gender?: string;
  guardianName?: string;
  guardianPhone?: string;
  guardianEmail?: string;
  address?: string;
  sectionId: string;
  admissionDate?: string;
}

export interface UpdateStudentInput {
  firstName?: string;
  lastName?: string;
  rollNo?: string;
  dateOfBirth?: string;
  gender?: string;
  guardianName?: string;
  guardianPhone?: string;
  guardianEmail?: string;
  address?: string;
  sectionId?: string;
  isActive?: boolean;
}

// Teachers
export interface Teacher {
  id: string;
  employeeNo: string;
  firstName: string;
  lastName: string;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  qualification?: string | null;
  joiningDate?: string | null;
  isActive: boolean;
}

export interface CreateTeacherInput {
  employeeNo: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  address?: string;
  qualification?: string;
  joiningDate?: string;
}

// Fees
export interface FeeCategory {
  id: string;
  name: string;
  description?: string | null;
}

export interface FeeStructure {
  id: string;
  feeCategoryId: string;
  classId: string;
  academicYearId: string;
  amount: number;
  dueDate: string;
  description?: string | null;
  feeCategory?: FeeCategory;
  schoolClass?: SchoolClass;
}

export interface StudentFee {
  id: string;
  studentId: string;
  feeStructureId?: string | null;
  purchaseId?: string | null;
  description: string;
  amount: number;
  dueDate: string;
  discount: number;
  status: FeeStatus;
  payments?: Payment[];
}

export interface Payment {
  id: string;
  studentFeeId: string;
  amount: number;
  mode: PaymentMode;
  referenceNo?: string | null;
  paidAt: string;
  recordedById?: string;
  notes?: string | null;
  studentFee?: StudentFee & {
    student?: {
      id: string;
      firstName: string;
      lastName: string;
      admissionNo: string;
    };
  };
}

export interface DueItem {
  id: string;
  description: string;
  dueDate: string;
  status: FeeStatus;
  amount: number;
  paid: number;
  balance: number;
  student: {
    id: string;
    firstName: string;
    lastName: string;
    admissionNo: string;
    guardianPhone?: string | null;
  };
}

export interface DuesResponse {
  count: number;
  totalOutstanding: number;
  dues: DueItem[];
}

export interface TransactionsResponse {
  count: number;
  totalAmount: number;
  transactions: Payment[];
}

// Inventory & Items
export interface ItemCategory {
  id: string;
  name: string;
  type: ItemType;
  description?: string | null;
}

export interface ItemVariant {
  id: string;
  itemCategoryId: string;
  label: string;
  classId?: string | null;
  size?: string | null;
  price: number;
  costPrice?: number | null;
  stockQuantity: number;
  isActive: boolean;
  itemCategory?: ItemCategory;
}

export interface InventoryItem {
  id: string;
  category: string;
  label: string;
  size?: string | null;
  class?: string | null;
  price: number;
  stockQuantity: number;
}

export interface InventoryResponse {
  count: number;
  items: InventoryItem[];
}

export interface PurchaseItemResponse {
  purchase: {
    id: string;
    studentId: string;
    itemVariantId: string;
    quantity: number;
  };
  studentFee: StudentFee;
  remainingStock: number;
  warning?: string | null;
}

// Staff & Salary
export interface Staff {
  id: string;
  name: string;
  type: StaffType;
  teacherId?: string | null;
  phone?: string | null;
  monthlySalary?: number | null;
  isActive: boolean;
  teacher?: Teacher | null;
  salaryPayments?: SalaryPayment[];
}

export interface SalaryPayment {
  id: string;
  staffId: string;
  amount: number;
  month: number;
  year: number;
  paidAt: string;
  mode: PaymentMode;
  notes?: string | null;
}

// Expenses
export interface ExpenseCategory {
  id: string;
  name: string;
  description?: string | null;
}

export interface Expense {
  id: string;
  expenseCategoryId: string;
  description: string;
  amount: number;
  mode: PaymentMode;
  referenceNo?: string | null;
  spentAt: string;
  expenseCategory?: ExpenseCategory;
}

export interface ExpensesResponse {
  count: number;
  totalAmount: number;
  expenses: Expense[];
}

export interface RecurringExpenseTemplate {
  id: string;
  expenseCategoryId: string;
  name: string;
  amount: number;
  dayOfMonth: number;
  expenseCategory?: ExpenseCategory;
}

// Attendance
export interface AttendanceRecord {
  id?: string;
  studentId: string;
  sectionId: string;
  date: string;
  status: AttendanceStatus;
  student?: {
    id: string;
    firstName: string;
    lastName: string;
    admissionNo: string;
    rollNo?: string | null;
  };
}

// Monthly Report
export interface MonthlyReport {
  period: { month: number; year: number };
  cashCollected: number;
  totalExpenses: number;
  totalSalaries: number;
  netForMonth: number;
  itemSalesRevenue?: number;
  itemCostOfSales?: number;
  itemGrossMargin?: number;
  expensesByCategory?: Array<{ name: string; total: number }>;
}

// Invoice & Receipt
export interface InvoiceSettings {
  id?: string;
  schoolName: string;
  address?: string | null;
  phone?: string | null;
  email?: string | null;
  footerNote?: string | null;
  thermalWidth?: number;
  marginSize?: number;
  headerFontSize?: number;
  bodyFontSize?: number;
}

export interface ReceiptItem {
  description: string;
  amount: number;
}

export interface ReceiptData {
  receiptNo: string;
  paidAmount: number;
  mode: string;
  studentName: string;
  admissionNo: string;
  className: string;
  items: ReceiptItem[];
  remainingDues: number;
  dateStr?: string;
}

