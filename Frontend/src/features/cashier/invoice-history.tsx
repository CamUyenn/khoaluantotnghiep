"use client";

import { useEffect, useMemo, useState, type ChangeEvent } from "react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Calendar,
  DollarSign,
  Download,
  Search,
  User,
} from "lucide-react";
import styles from "@/styles/common.module.css";
import historyStyles from "./invoice-history.module.css";
import cashierStyles from "./cashier.module.css";
import { cashierService } from "@/services/cashierService";
import { getApiErrorMessage } from "@/services/api";
import { toast } from "sonner";
import type { CashierPaidItem, CashierPaymentDetail } from "@/types/pharmacy.type";

const toDateInputValue = (value: Date) => {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const toStartDateTimeParam = (date: string) => `${date}T00:00:00`;
const toEndDateTimeParam = (date: string) => `${date}T23:59:59`;

export function InvoicesHistory() {
  const today = new Date();
  const defaultStartDate = new Date();
  defaultStartDate.setDate(today.getDate() - 7);

  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [transactions, setTransactions] = useState<CashierPaidItem[]>([]);
  const [invoiceDetails, setInvoiceDetails] = useState<Record<number, CashierPaymentDetail>>({});
  const [startDate, setStartDate] = useState(toDateInputValue(defaultStartDate));
  const [endDate, setEndDate] = useState(toDateInputValue(today));

  const loadInvoiceHistory = async (range?: { startDate: string; endDate: string }) => {
    const selectedStart = range?.startDate ?? startDate;
    const selectedEnd = range?.endDate ?? endDate;

    if (selectedStart > selectedEnd) {
      toast.error("Từ ngày không được lớn hơn đến ngày");
      return;
    }

    try {
      setLoading(true);
      const history = await cashierService.getTransactionHistory({
        startTime: toStartDateTimeParam(selectedStart),
        endTime: toEndDateTimeParam(selectedEnd),
      });
      const paid = history.transactions || [];
      setTransactions(paid);

      const details = await Promise.all(
        paid.map((item) => cashierService.getPaidInvoiceDetail(item.invoiceId).catch(() => null)),
      );

      const detailMap: Record<number, CashierPaymentDetail> = {};
      details.forEach((detail) => {
        if (detail) {
          detailMap[detail.invoiceId] = detail;
        }
      });
      setInvoiceDetails(detailMap);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải lịch sử thanh toán"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadInvoiceHistory({
      startDate: toDateInputValue(defaultStartDate),
      endDate: toDateInputValue(today),
    });
  }, []);

  const filteredInvoices = useMemo(() => transactions.filter((invoice) => {
    const detail = invoiceDetails[invoice.invoiceId];
    const phone = detail?.phoneNumber || "";

    return (
      String(invoice.invoiceId).includes(searchQuery) ||
      invoice.patientName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      phone.includes(searchQuery)
    );
  }), [invoiceDetails, searchQuery, transactions]);

  const handlePrint = async (invoiceId: number) => {
    try {
      const blob = await cashierService.exportInvoicePdf(invoiceId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `invoice-${invoiceId}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      toast.success(`Đã tải hóa đơn #${invoiceId}`);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể xuất hóa đơn PDF"));
    }
  };

  const paymentMethodLabel = (method: string) => {
    if (method === "TIEN_MAT") return "Tiền mặt";
    if (method === "CHUYEN_KHOAN") return "Chuyển khoản";
    if (method === "POS") return "POS";
    return method;
  };

  const handleApplyDateFilter = () => {
    void loadInvoiceHistory();
  };

  const handleQuickLast7Days = () => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 7);

    const nextStartDate = toDateInputValue(start);
    const nextEndDate = toDateInputValue(end);
    setStartDate(nextStartDate);
    setEndDate(nextEndDate);
    void loadInvoiceHistory({ startDate: nextStartDate, endDate: nextEndDate });
  };

  return (
      <main className={styles.mainArea}>
        <div className={styles.container}>
          <div className={styles.header}>
            <h1>Lịch sử thanh toán</h1>
            <p>Xem và in lại hóa đơn đã thanh toán</p>
          </div>

          <div className={historyStyles.wrapper}>
            <div className={cashierStyles.statsGrid}>
              <Card className={cashierStyles.statCard}>
                <div className={cashierStyles.statInner}>
                  <div className={`${cashierStyles.statIcon} ${cashierStyles.statIconBlue}`}>
                    <Calendar size={24} />
                  </div>
                  <div>
                    <p className={cashierStyles.statLabel}>Hóa đơn theo bộ lọc</p>
                    <p className={cashierStyles.statValue}>{transactions.length}</p>
                  </div>
                </div>
              </Card>

              <Card className={cashierStyles.statCard}>
                <div className={cashierStyles.statInner}>
                  <div className={`${cashierStyles.statIcon} ${cashierStyles.statIconGreen}`}>
                    <DollarSign size={24} />
                  </div>
                  <div>
                    <p className={cashierStyles.statLabel}>Doanh thu theo bộ lọc</p>
                    <p className={cashierStyles.statValue}>
                      {transactions
                        .reduce((sum, inv) => sum + inv.grandTotal, 0)
                        .toLocaleString("vi-VN")}
                      đ
                    </p>
                  </div>
                </div>
              </Card>

              <Card className={cashierStyles.statCard}>
                <div className={cashierStyles.statInner}>
                  <div className={`${cashierStyles.statIcon} ${historyStyles.statIconPurple}`}>
                    <User size={24} />
                  </div>
                  <div>
                    <p className={cashierStyles.statLabel}>Bệnh nhân</p>
                    <p className={cashierStyles.statValue}>{new Set(transactions.map((item) => item.patientId)).size}</p>
                  </div>
                </div>
              </Card>
            </div>

            <div className={historyStyles.filterRow}>
              <div className={historyStyles.filterField}>
                <label className={historyStyles.filterLabel} htmlFor="history-start-date">Từ ngày</label>
                <Input
                  id="history-start-date"
                  type="date"
                  value={startDate}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => setStartDate(e.target.value)}
                />
              </div>
              <div className={historyStyles.filterField}>
                <label className={historyStyles.filterLabel} htmlFor="history-end-date">Đến ngày</label>
                <Input
                  id="history-end-date"
                  type="date"
                  value={endDate}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => setEndDate(e.target.value)}
                />
              </div>
              <div className={historyStyles.filterActions}>
                <Button onClick={handleApplyDateFilter}>Lọc theo ngày</Button>
                <Button variant="outline" onClick={handleQuickLast7Days}>7 ngày gần nhất</Button>
              </div>
            </div>

            <div className={historyStyles.searchBox}>
              <Search size={18} className={historyStyles.searchIcon} />
              <Input
                placeholder="Tìm kiếm theo mã hóa đơn, tên bệnh nhân hoặc SĐT..."
                className={historyStyles.searchInput}
                value={searchQuery}
                onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
              />
            </div>

            <div className={historyStyles.list}>
              {loading && (
                <Card className={cashierStyles.emptyCard}>
                  <p className={cashierStyles.emptyText}>Đang tải dữ liệu...</p>
                </Card>
              )}

              {filteredInvoices.map((invoice) => (
                <Card key={invoice.invoiceId} className={historyStyles.historyCard}>
                  <div className={historyStyles.headerRow}>
                    <div>
                      <div className={historyStyles.metaRow}>
                        <h3 className={historyStyles.title}>{invoice.patientName}</h3>
                        <Badge variant="secondary" className={historyStyles.idBadge}>#{invoice.invoiceId}</Badge>
                      </div>
                      <div className={historyStyles.metaRow}>
                        <div className={historyStyles.metaItem}>
                          <Calendar size={14} />
                          <span>{new Date(invoice.paidAt).toLocaleString("vi-VN")}</span>
                        </div>
                        <div className={historyStyles.metaItem}>
                          <User size={14} />
                          <span>{invoiceDetails[invoice.invoiceId]?.phoneNumber || "Chưa cập nhật"}</span>
                        </div>
                        <div className={historyStyles.metaItem}>
                          <DollarSign size={14} />
                          <span>{paymentMethodLabel(invoice.paymentMethod)}</span>
                        </div>
                      </div>
                    </div>
                    <Button size="sm" className={historyStyles.printButton} onClick={() => void handlePrint(invoice.invoiceId)}>
                      <Download size={14} />
                      In hóa đơn
                    </Button>
                  </div>

                  <div className={historyStyles.servicesBox}>
                    <p className={historyStyles.servicesTitle}>Dịch vụ đã khám và được tính tiền:</p>
                    <div className={cashierStyles.amountList}>
                      {(invoiceDetails[invoice.invoiceId]?.services || []).map((service) => (
                        <div key={service.serviceId} className={cashierStyles.amountRow}>
                          <span className={cashierStyles.amountLabel}>
                            {service.serviceName} x {service.quantity}
                          </span>
                          <span className={cashierStyles.amountValue}>
                            {service.lineTotal.toLocaleString("vi-VN")}đ
                          </span>
                        </div>
                      ))}
                      {(invoiceDetails[invoice.invoiceId]?.services || []).length === 0 && (
                        <span className={cashierStyles.emptyText}>Không có dịch vụ được tính tiền</span>
                      )}
                    </div>
                  </div>

                  <div className={cashierStyles.amountList}>
                    <div className={`${cashierStyles.amountRow} ${cashierStyles.amountBorder}`}>
                      <span className={cashierStyles.amountLabel}>Tổng tiền dịch vụ:</span>
                      <span className={cashierStyles.amountValue}>{(invoiceDetails[invoice.invoiceId]?.totalServiceFee || 0).toLocaleString("vi-VN")}đ</span>
                    </div>
                  </div>
                </Card>
              ))}

              {!loading && filteredInvoices.length === 0 && (
                <Card className={cashierStyles.emptyCard}>
                  <Search size={44} color="#cbd5e1" />
                  <p className={cashierStyles.emptyText}>Không tìm thấy hóa đơn nào</p>
                </Card>
              )}
            </div>
          </div>
        </div>
      </main>
  );
}
