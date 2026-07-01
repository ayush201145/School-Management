const prisma = require("../prismaClient");
const { ApiError } = require("../middleware/errorHandler");

/**
 * GET /api/invoice-settings
 * Returns the school's general invoice configurations (public read)
 */
async function getInvoiceSettings(req, res) {
  let settings = await prisma.invoiceSettings.findFirst();
  if (!settings) {
    settings = await prisma.invoiceSettings.create({
      data: {
        schoolName: "ABC Public School",
        footerNote: "Thank you for your payment!",
        thermalWidth: 576,
        marginSize: 20,
        headerFontSize: 28,
        bodyFontSize: 14,
      },
    });
  }
  res.json(settings);
}

/**
 * PATCH /api/invoice-settings
 * Updates configurations (restricted to MASTER role only)
 */
async function updateInvoiceSettings(req, res) {
  let settings = await prisma.invoiceSettings.findFirst();
  if (!settings) {
    settings = await prisma.invoiceSettings.create({
      data: {
        schoolName: "ABC Public School",
        footerNote: "Thank you for your payment!",
        thermalWidth: 576,
        marginSize: 20,
        headerFontSize: 28,
        bodyFontSize: 14,
      },
    });
  }

  const {
    schoolName,
    address,
    phone,
    email,
    footerNote,
    thermalWidth,
    marginSize,
    headerFontSize,
    bodyFontSize,
  } = req.body;

  const data = {};
  if (schoolName !== undefined) data.schoolName = schoolName;
  if (address !== undefined) data.address = address;
  if (phone !== undefined) data.phone = phone;
  if (email !== undefined) data.email = email;
  if (footerNote !== undefined) data.footerNote = footerNote;
  if (thermalWidth !== undefined) data.thermalWidth = parseInt(thermalWidth) || 576;
  if (marginSize !== undefined) data.marginSize = parseInt(marginSize) || 20;
  if (headerFontSize !== undefined) data.headerFontSize = parseInt(headerFontSize) || 28;
  if (bodyFontSize !== undefined) data.bodyFontSize = parseInt(bodyFontSize) || 14;

  const updated = await prisma.invoiceSettings.update({
    where: { id: settings.id },
    data,
  });

  res.json(updated);
}

module.exports = {
  getInvoiceSettings,
  updateInvoiceSettings,
};
