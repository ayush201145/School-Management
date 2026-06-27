/**
 * Seed script — sets up the academic year, the actual class list
 * (Play, Nursery, LKG, UKG, 1–5), the three uniform categories, and
 * the uniform size scale (20,22,24...34). Book variants are created
 * per class but left at price 0 / stock 0 for you to fill in via the
 * app, since exact book-set prices aren't known yet.
 *
 * Run with: node prisma/seed.js
 */
const { PrismaClient } = require("@prisma/client");
const prisma = new PrismaClient();

const CLASS_NAMES = ["Play", "Nursery", "LKG", "UKG", "Class 1", "Class 2", "Class 3", "Class 4", "Class 5"];
const UNIFORM_SIZES = ["20", "22", "24", "26", "28", "30", "32", "34"];

async function main() {
  // 1. Academic year
  let year = await prisma.academicYear.findUnique({ where: { label: "2026-27" } });
  if (!year) {
    year = await prisma.academicYear.create({
      data: {
        label: "2026-27",
        startDate: new Date("2026-04-01"),
        endDate: new Date("2027-03-31"),
        isCurrent: true,
      },
    });
    console.log("Created academic year 2026-27");
  }

  // 2. Classes: Play, Nursery, LKG, UKG, Class 1..5
  const classMap = {};
  for (const name of CLASS_NAMES) {
    let cls = await prisma.schoolClass.findFirst({ where: { name, academicYearId: year.id } });
    if (!cls) {
      cls = await prisma.schoolClass.create({ data: { name, academicYearId: year.id } });
      console.log(`Created class: ${name}`);
    }
    classMap[name] = cls;

    // Ensure default General section exists
    let section = await prisma.section.findFirst({ where: { name: "General", classId: cls.id } });
    if (!section) {
      await prisma.section.create({ data: { name: "General", classId: cls.id } });
      console.log(`  Created default section "General" for class: ${name}`);
    }
  }

  // 3. Book category + one variant per class (price/stock left at 0 —
  //    fill in real prices via PATCH /api/item-variants/:id once known)
  let bookCategory = await prisma.itemCategory.findUnique({ where: { name: "Books" } });
  if (!bookCategory) {
    bookCategory = await prisma.itemCategory.create({ data: { name: "Books", type: "BOOK" } });
    console.log("Created item category: Books");
  }
  for (const name of CLASS_NAMES) {
    const label = `${name} Book Set`;
    const exists = await prisma.itemVariant.findFirst({
      where: { itemCategoryId: bookCategory.id, classId: classMap[name].id },
    });
    if (!exists) {
      await prisma.itemVariant.create({
        data: {
          itemCategoryId: bookCategory.id,
          classId: classMap[name].id,
          label,
          price: 0, // set the real price later
          stockQuantity: 0,
        },
      });
      console.log(`  Created book variant: ${label}`);
    }
  }

  // 4. Uniform categories: 2 summer types + 1 winter type
  const uniformCategoryNames = [
    { name: "Uniform - Summer (Regular)", type: "UNIFORM_SUMMER" },
    { name: "Uniform - Summer (PT/Sports)", type: "UNIFORM_SUMMER" },
    { name: "Uniform - Winter", type: "UNIFORM_WINTER" },
  ];

  for (const { name, type } of uniformCategoryNames) {
    let category = await prisma.itemCategory.findUnique({ where: { name } });
    if (!category) {
      category = await prisma.itemCategory.create({ data: { name, type } });
      console.log(`Created item category: ${name}`);
    }

    // 5. One variant per size (20,22,24...34) for each uniform category
    for (const size of UNIFORM_SIZES) {
      const label = `Size ${size}`;
      const exists = await prisma.itemVariant.findFirst({
        where: { itemCategoryId: category.id, size },
      });
      if (!exists) {
        await prisma.itemVariant.create({
          data: {
            itemCategoryId: category.id,
            size,
            label,
            price: 0, // set the real price later
            stockQuantity: 0,
          },
        });
        console.log(`  Created uniform variant: ${name} / ${label}`);
      }
    }
  }

  console.log("\nSeed complete. Remember to set real prices via:");
  console.log("  PATCH /api/item-variants/:id   { price: <amount> }");
  console.log("And add initial stock via:");
  console.log("  POST /api/item-variants/:id/restock   { quantity: <amount>, note: \"Initial stock\" }");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
