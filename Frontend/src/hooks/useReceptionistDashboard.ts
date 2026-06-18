"use client";

import { useCallback, useMemo, useState } from "react";

import { receptionistService, type ReceptionistAppointment, type ReceptionistDoctorOption } from "@/services/receptionistService";

export function useReceptionistDashboard() {
  const [pendingAppointments, setPendingAppointments] = useState<ReceptionistAppointment[]>([]);
  const [confirmedAppointments, setConfirmedAppointments] = useState<ReceptionistAppointment[]>([]);
  const [cancelledAppointments, setCancelledAppointments] = useState<ReceptionistAppointment[]>([]);
  const [doctorOptions, setDoctorOptions] = useState<ReceptionistDoctorOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboardData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [pendingData, confirmedData, cancelledData, doctors] = await Promise.all([
        receptionistService.getPendingAppointments(),
        receptionistService.getConfirmedAppointments(),
        receptionistService.getCancelledAppointments().catch(() => []),
        receptionistService.getDoctors(),
      ]);

      setPendingAppointments(pendingData);
      setConfirmedAppointments(confirmedData);
      setCancelledAppointments(cancelledData);
      setDoctorOptions(doctors);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const totalCount = useMemo(() => {
    const idSet = new Set<number>();
    pendingAppointments.forEach((item) => idSet.add(item.id));
    confirmedAppointments.forEach((item) => idSet.add(item.id));
    cancelledAppointments.forEach((item) => idSet.add(item.id));
    return idSet.size;
  }, [pendingAppointments, confirmedAppointments, cancelledAppointments]);

  return {
    pendingAppointments,
    confirmedAppointments,
    cancelledAppointments,
    doctorOptions,
    totalCount,
    isLoading,
    loadDashboardData,
  };
}
