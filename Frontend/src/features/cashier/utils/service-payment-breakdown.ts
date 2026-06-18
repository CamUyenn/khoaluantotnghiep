import { CashierServiceLine } from "@/types/pharmacy.type";

const normalizeServiceName = (value: string) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();

export const isConsultationServiceName = (serviceName?: string | null) => {
  if (!serviceName) {
    return false;
  }

  const normalized = normalizeServiceName(serviceName);
  return ["kham", "consultation", "examination", "tu van"].some((keyword) =>
    normalized.includes(keyword),
  );
};

export interface ServicePaymentBreakdownLine extends CashierServiceLine {
  coveredLineTotal: number;
  unpaidLineTotal: number;
}

export interface ServicePaymentBreakdown {
  consultationServices: ServicePaymentBreakdownLine[];
  additionalServices: ServicePaymentBreakdownLine[];
  consultationFee: number;
  consultationCoveredAmount: number;
  consultationOutstandingAmount: number;
  additionalServiceFee: number;
  additionalCoveredAmount: number;
  additionalOutstandingAmount: number;
  consultationOutstandingFee: number;
  unpaidAdditionalServices: ServicePaymentBreakdownLine[];
  unpaidAdditionalServiceFee: number;
  totalDueAfterAdvance: number;
}

export interface BackendServicePaymentBreakdownLine extends CashierServiceLine {
  coveredAmount: number;
  unpaidAmount: number;
}

export interface BackendServicePaymentBreakdownSource {
  serviceItems?: CashierServiceLine[];
  consultationFee?: number;
  consultationCoveredAmount?: number;
  consultationOutstandingAmount?: number;
  additionalServiceFee?: number;
  additionalCoveredAmount?: number;
  additionalOutstandingAmount?: number;
  serviceBreakdown?: BackendServicePaymentBreakdownLine[];
}

const sumLineTotal = (items: CashierServiceLine[]) =>
  items.reduce((sum, item) => sum + Number(item.lineTotal || 0), 0);

export const getServicePaymentBreakdown = (
  source: BackendServicePaymentBreakdownSource,
  advanceAmount: number,
): ServicePaymentBreakdown => {
  if (
    Array.isArray(source.serviceBreakdown) &&
    source.serviceBreakdown.length > 0 &&
    source.consultationFee !== undefined &&
    source.consultationCoveredAmount !== undefined &&
    source.consultationOutstandingAmount !== undefined &&
    source.additionalServiceFee !== undefined &&
    source.additionalCoveredAmount !== undefined &&
    source.additionalOutstandingAmount !== undefined
  ) {
    const consultationServices = source.serviceItems?.filter((item) => isConsultationServiceName(item.serviceName)) ?? [];
    const additionalServices = source.serviceItems?.filter((item) => !isConsultationServiceName(item.serviceName)) ?? [];
    const unpaidAdditionalServices = source.serviceBreakdown
      .filter((item) => item.unpaidAmount > 0 && !isConsultationServiceName(item.serviceName))
      .map((item) => ({
        ...item,
        coveredLineTotal: item.coveredAmount,
        unpaidLineTotal: item.unpaidAmount,
      }));

    return {
      consultationServices,
      additionalServices,
      consultationFee: Number(source.consultationFee || 0),
      consultationCoveredAmount: Number(source.consultationCoveredAmount || 0),
      consultationOutstandingAmount: Number(source.consultationOutstandingAmount || 0),
      additionalServiceFee: Number(source.additionalServiceFee || 0),
      additionalCoveredAmount: Number(source.additionalCoveredAmount || 0),
      additionalOutstandingAmount: Number(source.additionalOutstandingAmount || 0),
      consultationOutstandingFee: Number(source.consultationOutstandingAmount || 0),
      unpaidAdditionalServices,
      unpaidAdditionalServiceFee: unpaidAdditionalServices.reduce((sum, item) => sum + item.unpaidLineTotal, 0),
      totalDueAfterAdvance:
        Number(source.consultationOutstandingAmount || 0) + Number(source.additionalOutstandingAmount || 0),
    };
  }

  const serviceItems = source.serviceItems ?? [];
  const consultationRaw = serviceItems.filter((item) => isConsultationServiceName(item.serviceName));
  const additionalRaw = serviceItems.filter((item) => !isConsultationServiceName(item.serviceName));

  const consultationFee =
    consultationRaw.length > 0
      ? sumLineTotal(consultationRaw)
      : Math.max(0, sumLineTotal(serviceItems) - sumLineTotal(additionalRaw));

  const additionalServiceFee = sumLineTotal(additionalRaw);

  // Chỉ dùng tiền đặt trước để bù cho dịch vụ khám ban đầu.
  // Dịch vụ phát sinh phải luôn còn nợ cho tới khi thu riêng tại quầy.
  let remainingAdvance = Math.max(0, Number(advanceAmount || 0));

  const consultationServicesDetailed: ServicePaymentBreakdownLine[] = consultationRaw.map((item) => {
    const lineTotal = Number(item.lineTotal || 0);
    const coveredLineTotal = Math.min(lineTotal, remainingAdvance);
    remainingAdvance -= coveredLineTotal;
    return {
      ...item,
      coveredLineTotal,
      unpaidLineTotal: Math.max(0, lineTotal - coveredLineTotal),
    };
  });

  const additionalServicesDetailed: ServicePaymentBreakdownLine[] = additionalRaw.map((item) => {
    const lineTotal = Number(item.lineTotal || 0);
    return {
      ...item,
      coveredLineTotal: 0,
      unpaidLineTotal: lineTotal,
    };
  });

  const unpaidAdditionalServices = additionalServicesDetailed;
  const unpaidAdditionalServiceFee = additionalServiceFee;

  const consultationCovered = consultationServicesDetailed.reduce((s, i) => s + Number(i.coveredLineTotal || 0), 0);
  const consultationOutstandingFee = Math.max(0, consultationFee - consultationCovered);
  const additionalCoveredAmount = 0;
  const additionalOutstandingAmount = unpaidAdditionalServiceFee;
  const totalDueAfterAdvance = consultationOutstandingFee + additionalOutstandingAmount;

  return {
    consultationServices: consultationServicesDetailed,
    additionalServices: additionalServicesDetailed,
    consultationFee,
    consultationCoveredAmount: consultationCovered,
    consultationOutstandingAmount: consultationOutstandingFee,
    additionalServiceFee,
    additionalCoveredAmount,
    additionalOutstandingAmount,
    consultationOutstandingFee,
    unpaidAdditionalServices,
    unpaidAdditionalServiceFee,
    totalDueAfterAdvance,
  };
};