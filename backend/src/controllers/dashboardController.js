const prisma = require("../prismaClient");

async function getDashboardStats(req, res) {
  const { academicYearId } = req.query;
  if (!academicYearId) {
    return res.status(400).json({ error: "academicYearId query param is required" });
  }

  try {
    // 1. Fetch active students in the selected academic year
    const activeStudents = await prisma.student.findMany({
      where: {
        isDeleted: false,
        isActive: true,
        section: {
          schoolClass: {
            academicYearId: academicYearId
          }
        }
      },
      include: {
        studentFees: {
          where: { isDeleted: false, isDefaulted: false },
          include: { payments: { where: { isDeleted: false } } }
        }
      }
    });

    const totalStudents = activeStudents.length;

    // 2. Calculate fee collections and outstanding dues in this academic year
    let totalBilled = 0;
    let totalCollected = 0;
    let totalOutstanding = 0;

    for (const s of activeStudents) {
      for (const f of s.studentFees) {
        const billed = Number(f.amount) - Number(f.discount);
        const paid = f.payments.reduce((sum, p) => sum + Number(p.amount), 0);
        totalBilled += billed;
        totalCollected += paid;
        totalOutstanding += Math.max(billed - paid, 0);
      }
    }

    // 3. Inventory Stock distribution count by category type
    const variants = await prisma.itemVariant.findMany({
      where: { isDeleted: false },
      include: { itemCategory: true }
    });

    const stockByCategoryType = {
      BOOK: 0,
      UNIFORM_SUMMER: 0,
      UNIFORM_WINTER: 0,
      OTHER: 0
    };

    for (const v of variants) {
      const type = v.itemCategory.type;
      if (stockByCategoryType[type] !== undefined) {
        stockByCategoryType[type] += v.stockQuantity;
      }
    }

    // 4. Student Attendance Today in this academic year
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);

    const presentCount = await prisma.attendance.count({
      where: {
        status: "PRESENT",
        date: { gte: today, lt: tomorrow },
        student: {
          section: {
            schoolClass: {
              academicYearId: academicYearId
            }
          }
        }
      }
    });

    res.json({
      totalStudents,
      totalBilled,
      totalCollected,
      totalOutstanding,
      stockByCategoryType,
      presentToday: presentCount
    });
  } catch (err) {
    res.status(500).json({ error: err.message || "Failed to load dashboard statistics" });
  }
}

module.exports = { getDashboardStats };
