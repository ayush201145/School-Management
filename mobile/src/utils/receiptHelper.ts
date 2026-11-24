import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import { settingsApi } from '../api/settings';
import { InvoiceSettings, ReceiptData } from '../types';
import { formatCurrency } from './format';

let cachedSettings: InvoiceSettings | null = null;

export async function getSchoolInvoiceSettings(): Promise<InvoiceSettings> {
  if (cachedSettings) return cachedSettings;
  try {
    const settings = await settingsApi.getInvoiceSettings();
    cachedSettings = settings;
    return settings;
  } catch {
    return {
      schoolName: 'ABC Public School',
      footerNote: 'Thank you for your payment!',
    };
  }
}

export function buildReceiptHtml(data: ReceiptData, settings: InvoiceSettings): string {
  const schoolName = settings.schoolName || 'ABC Public School';
  const address = settings.address || '';
  const phone = settings.phone || '';
  const email = settings.email || '';
  const footerNote = settings.footerNote || 'Thank you for your payment!';
  const dateStr = data.dateStr || new Date().toISOString().substring(0, 10);

  const itemsHtml = data.items
    .map(
      (item, idx) => `
      <tr>
        <td style="padding: 8px; border-bottom: 1px solid #E0E0E0;">${idx + 1}</td>
        <td style="padding: 8px; border-bottom: 1px solid #E0E0E0;">${item.description}</td>
        <td style="padding: 8px; border-bottom: 1px solid #E0E0E0; text-align: right;">₹${formatCurrency(
          item.amount
        )}</td>
      </tr>
    `
    )
    .join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no" />
        <style>
          body {
            font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
            color: #333;
            margin: 0;
            padding: 24px;
            background-color: #ffffff;
          }
          .header {
            text-align: center;
            border-bottom: 2px solid #3F51B5;
            padding-bottom: 12px;
            margin-bottom: 20px;
          }
          .school-name {
            font-size: 24px;
            font-weight: bold;
            color: #1A237E;
            text-transform: uppercase;
            margin: 0;
          }
          .school-meta {
            font-size: 12px;
            color: #666;
            margin-top: 4px;
          }
          .receipt-title {
            font-size: 16px;
            font-weight: 700;
            letter-spacing: 1px;
            color: #3F51B5;
            margin-top: 10px;
            text-transform: uppercase;
          }
          .meta-grid {
            display: flex;
            justify-content: space-between;
            margin-bottom: 20px;
            font-size: 13px;
          }
          .meta-col {
            line-height: 1.6;
          }
          .table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
            font-size: 13px;
          }
          .table th {
            background-color: #F5F5F7;
            padding: 10px 8px;
            border-bottom: 2px solid #BDBDBD;
            text-align: left;
            font-weight: 600;
          }
          .summary-box {
            float: right;
            width: 250px;
            margin-top: 10px;
            font-size: 14px;
          }
          .summary-row {
            display: flex;
            justify-content: space-between;
            padding: 4px 0;
          }
          .summary-row.total {
            font-weight: bold;
            border-top: 1px solid #BDBDBD;
            border-bottom: 2px solid #3F51B5;
            padding-top: 6px;
            padding-bottom: 6px;
            color: #1A237E;
            font-size: 16px;
          }
          .footer {
            clear: both;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px dashed #BDBDBD;
            text-align: center;
            font-size: 12px;
            color: #777;
          }
          .signature-area {
            display: flex;
            justify-content: space-between;
            margin-top: 60px;
            font-size: 12px;
          }
          .sig-line {
            border-top: 1px solid #777;
            width: 160px;
            text-align: center;
            padding-top: 4px;
          }
        </style>
      </head>
      <body>
        <div class="header">
          <h1 class="school-name">${schoolName}</h1>
          ${address ? `<div class="school-meta">${address}</div>` : ''}
          ${
            phone || email
              ? `<div class="school-meta">${[phone && `Phone: ${phone}`, email && `Email: ${email}`]
                  .filter(Boolean)
                  .join(' | ')}</div>`
              : ''
          }
          <div class="receipt-title">Payment Receipt</div>
        </div>

        <div class="meta-grid">
          <div class="meta-col">
            <div><strong>Student:</strong> ${data.studentName}</div>
            <div><strong>Admission No:</strong> ${data.admissionNo}</div>
            <div><strong>Class:</strong> ${data.className}</div>
          </div>
          <div class="meta-col" style="text-align: right;">
            <div><strong>Receipt No:</strong> ${data.receiptNo}</div>
            <div><strong>Date:</strong> ${dateStr}</div>
            <div><strong>Payment Mode:</strong> ${data.mode}</div>
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th style="width: 40px;">#</th>
              <th>Fee / Item Description</th>
              <th style="width: 110px; text-align: right;">Amount (₹)</th>
            </tr>
          </thead>
          <tbody>
            ${itemsHtml}
          </tbody>
        </table>

        <div class="summary-box">
          <div class="summary-row total">
            <span>Total Paid:</span>
            <span>₹${formatCurrency(data.paidAmount)}</span>
          </div>
          <div class="summary-row" style="color: ${data.remainingDues > 0 ? '#D32F2F' : '#388E3C'}; margin-top: 4px;">
            <span>Outstanding Balance:</span>
            <span>₹${formatCurrency(data.remainingDues)}</span>
          </div>
        </div>

        <div class="signature-area">
          <div class="sig-line">Parent / Guardian</div>
          <div class="sig-line">Authorized Signatory</div>
        </div>

        <div class="footer">
          <div>${footerNote}</div>
          <div style="font-size: 10px; color: #999; margin-top: 4px;">Generated electronically by School Management System</div>
        </div>
      </body>
    </html>
  `;
}

export async function generateAndShareReceiptPdf(data: ReceiptData): Promise<void> {
  const settings = await getSchoolInvoiceSettings();
  const html = buildReceiptHtml(data, settings);

  const { uri } = await Print.printToFileAsync({ html });

  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(uri, {
      UTI: '.pdf',
      mimeType: 'application/pdf',
      dialogTitle: `Payment Receipt - ${data.receiptNo}`,
    });
  }
}

export async function printReceiptDirect(data: ReceiptData): Promise<void> {
  const settings = await getSchoolInvoiceSettings();
  const html = buildReceiptHtml(data, settings);

  await Print.printAsync({ html });
}
