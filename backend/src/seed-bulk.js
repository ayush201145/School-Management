const dns = require("dns");
dns.setDefaultResultOrder("ipv4first");

const path = require("path");
require("dotenv").config({ path: path.join(__dirname, "../.env") });

const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

const FIRST_NAMES = ["Aarav", "Vihaan", "Aditya", "Sai", "Arjun", "Krishna", "Ishaan", "Shaurya", "Pranav", "Kabir", "Ananya", "Diya", "Ira", "Sana", "Kiara", "Aadhya", "Myra", "Riya", "Aanya", "Pari", "Rohan", "Dev", "Rahul", "Tanvi", "Neha", "Pooja", "Vikram", "Abhishek", "Karan", "Simran"];
const LAST_NAMES = ["Sharma", "Verma", "Gupta", "Kumar", "Singh", "Yadav", "Patel", "Joshi", "Chawla", "Mehta", "Iyer", "Nair", "Das", "Roy", "Bose", "Chatterjee", "Sen", "Reddy", "Rao", "Mishra", "Gill", "Soni", "Choudhary", "Dubey", "Trivedi", "Pandey"];

function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function getRandomNumber(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

async function main() {
  console.log("Starting bulk seeding of test data...");

  // 1. Fetch current Academic Year & Sections
  const academicYear = await prisma.academicYear.findFirst({
    where: { isCurrent: true, isDeleted: false }
  });
  if (!academicYear) {
    console.error("No current academic year found. Please seed the database first.");
    process.exit(1);
  }
  console.log(`Using Academic Year: ${academicYear.label}`);

  const sections = await prisma.section.findMany({
    where: { isDeleted: false },
    include: { schoolClass: true }
  });
  if (sections.length === 0) {
    console.error("No sections found. Please seed classes and sections first.");
    process.exit(1);
  }
  console.log(`Found ${sections.length} section(s) to distribute students into.`);

  // 2. Fetch or create Tuition Fee Category
  let tuitionCategory = await prisma.feeCategory.findFirst({
    where: { name: { contains: "Tuition" }, isDeleted: false }
  });
  if (!tuitionCategory) {
    tuitionCategory = await prisma.feeCategory.create({
      data: { name: "Tuition Fee", description: "Standard monthly tuition fee" }
    });
  }

  // 3. Generate 50 Teachers
  console.log("Generating 50 teachers...");
  const teachersCreated = [];
  for (let i = 1; i <= 50; i++) {
    const firstName = getRandomElement(FIRST_NAMES);
    const lastName = getRandomElement(LAST_NAMES);
    const empNo = `EMP-${String(100 + i)}`;

    const teacher = await prisma.teacher.create({
      data: {
        employeeNo: empNo,
        firstName,
        lastName,
        phone: `98765${String(10000 + i)}`,
        email: `${firstName.toLowerCase()}.${lastName.toLowerCase()}@school.edu`,
        address: `${getRandomNumber(1, 999)} Main Street, Townsville`,
        qualification: getRandomElement(["B.Ed", "M.Ed", "MA English", "M.Sc Physics", "B.Sc Mathematics"]),
      }
    });

    const staff = await prisma.staff.create({
      data: {
        name: `${firstName} ${lastName}`,
        type: "TEACHER",
        teacherId: teacher.id,
        phone: teacher.phone,
        monthlySalary: getRandomNumber(250, 600) * 100 // ₹25,000 - ₹60,000
      }
    });

    teachersCreated.push(teacher);
  }
  console.log(`Successfully created ${teachersCreated.length} teachers and payroll staff records.`);

  // 4. Generate 500 Students
  console.log("Generating 500 students...");
  const studentsCreated = [];
  const startAdmNo = 10000 + getRandomNumber(100, 999);
  
  for (let i = 1; i <= 500; i++) {
    const firstName = getRandomElement(FIRST_NAMES);
    const lastName = getRandomElement(LAST_NAMES);
    const admNo = String(startAdmNo + i);
    const section = getRandomElement(sections);

    // 10% of students have custom overrides
    const hasOverride = Math.random() < 0.1;
    const tuitionOverride = hasOverride ? getRandomNumber(120, 220) * 10 : null;

    const student = await prisma.student.create({
      data: {
        admissionNo: admNo,
        firstName,
        lastName,
        dateOfBirth: new Date(`20${getRandomNumber(10, 20)}-${String(getRandomNumber(1, 12)).padStart(2, '0')}-${String(getRandomNumber(1, 28)).padStart(2, '0')}`),
        gender: getRandomElement(["MALE", "FEMALE"]),
        guardianName: `${getRandomElement(FIRST_NAMES)} ${lastName}`,
        guardianPhone: `91234${String(10000 + i)}`,
        address: `${getRandomNumber(1, 500)} Colony Block, City`,
        tuitionFee: tuitionOverride,
        sectionId: section.id,
      }
    });
    studentsCreated.push(student);
  }
  console.log(`Successfully created ${studentsCreated.length} students distributed across classes.`);

  // 5. Generate Fee Structures (for April, May, and June Tuition)
  console.log("Creating fee structures and student dues...");
  const months = ["April 2026", "May 2026", "June 2026"];
  const structuresCreated = [];

  const classes = await prisma.schoolClass.findMany({ where: { isDeleted: false } });
  
  for (const schoolClass of classes) {
    // Standard tuition rate for class
    const standardRate = getRandomNumber(150, 250) * 10; // ₹1,500 - ₹2,500

    for (let idx = 0; idx < months.length; idx++) {
      const month = months[idx];
      const dueDate = new Date(`2026-0${4 + idx}-10`);
      
      const structure = await prisma.feeStructure.create({
        data: {
          feeCategoryId: tuitionCategory.id,
          classId: schoolClass.id,
          academicYearId: academicYear.id,
          amount: standardRate,
          dueDate,
          description: `Tuition Fee - ${month}`,
        }
      });
      structuresCreated.push(structure);

      // Bill all students in this class
      const classStudents = studentsCreated.filter(s => {
        const studSec = sections.find(sec => sec.id === s.sectionId);
        return studSec && studSec.classId === schoolClass.id;
      });

      const toBill = classStudents.map(student => {
        const finalAmount = student.tuitionFee !== null ? student.tuitionFee : standardRate;
        return {
          studentId: student.id,
          feeStructureId: structure.id,
          description: structure.description,
          amount: finalAmount,
          dueDate: structure.dueDate,
        };
      });

      if (toBill.length > 0) {
        await prisma.studentFee.createMany({ data: toBill });
      }
    }
  }
  console.log(`Generated Tuition Fee structures for April, May, and June.`);

  // 6. Record Random Transactions
  console.log("Simulating payments (cash and UPI) to create active transaction histories...");
  const unpaidFees = await prisma.studentFee.findMany({
    where: { isDeleted: false, status: "UNPAID" }
  });

  console.log(`Total outstanding student dues to simulate payments for: ${unpaidFees.length}`);
  
  let paidCount = 0;
  // 60% probability of a fee being paid (some full, some partial)
  for (const fee of unpaidFees) {
    if (Math.random() < 0.6) {
      const isPartial = Math.random() < 0.2;
      const feeAmount = Number(fee.amount);
      const payAmount = isPartial ? getRandomNumber(5, 15) * 100 : feeAmount;

      await prisma.payment.create({
        data: {
          studentFeeId: fee.id,
          amount: payAmount,
          mode: getRandomElement(["CASH", "UPI", "BANK_TRANSFER"]),
          referenceNo: Math.random() < 0.5 ? `TXN${getRandomNumber(100000, 999999)}` : null,
          paidAt: new Date(fee.dueDate.getTime() - getRandomNumber(0, 5) * 24 * 60 * 60 * 1000), // Paid on or before due date
          recordedById: "system-bulk-seeder"
        }
      });

      // Update student fee status
      const newStatus = payAmount >= feeAmount ? "PAID" : "PARTIAL";
      await prisma.studentFee.update({
        where: { id: fee.id },
        data: { status: newStatus }
      });

      paidCount++;
    }
  }

  console.log(`Simulated ${paidCount} payment transactions.`);
  console.log("Bulk seeding complete! Enjoy testing the app at high scale!");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
