require("dotenv").config();
require("express-async-errors"); // must be required before routes are mounted

const express = require("express");
const cors = require("cors");
const morgan = require("morgan");

const { errorHandler } = require("./middleware/errorHandler");
const authRoutes = require("./routes/authRoutes");
const teacherRoutes = require("./routes/teacherRoutes");
const academicRoutes = require("./routes/academicRoutes");
const studentRoutes = require("./routes/studentRoutes");
const feeRoutes = require("./routes/feeRoutes");
const paymentRoutes = require("./routes/paymentRoutes");
const itemRoutes = require("./routes/itemRoutes");
const syncRoutes = require("./routes/syncRoutes");
const staffRoutes = require("./routes/staffRoutes");
const expenseRoutes = require("./routes/expenseRoutes");
const reportRoutes = require("./routes/reportRoutes");
const userRoutes = require("./routes/userRoutes");
const settingsRoutes = require("./routes/settingsRoutes");
const attendanceRoutes = require("./routes/attendanceRoutes");

const app = express();

app.use(cors({ origin: process.env.CORS_ORIGIN || "*" }));
app.use(express.json());
app.use(morgan(process.env.NODE_ENV === "development" ? "dev" : "combined"));

app.get("/health", (req, res) => res.json({ status: "ok", time: new Date().toISOString() }));

app.use("/api/auth", authRoutes);
app.use("/api/teachers", teacherRoutes);
app.use("/api", academicRoutes); // /api/academic-years, /api/classes, /api/sections
app.use("/api/students", studentRoutes);
app.use("/api", feeRoutes); // /api/fee-categories, /api/fee-structures
app.use("/api", paymentRoutes); // /api/student-fees/:id/payments, /api/transactions, /api/dues
app.use("/api", itemRoutes); // /api/item-categories, /api/item-variants, /api/students/:id/purchases
app.use("/api", syncRoutes); // /api/sync/push, /api/sync/pull, /api/sync/conflicts
app.use("/api", staffRoutes); // /api/staff, /api/salary-status
app.use("/api", expenseRoutes); // /api/expenses, /api/recurring-expenses
app.use("/api", reportRoutes); // /api/reports/monthly
app.use("/api", userRoutes);
app.use("/api", settingsRoutes);
app.use("/api", attendanceRoutes);

app.use((req, res) => res.status(404).json({ error: "Route not found" }));
app.use(errorHandler);

module.exports = app;
