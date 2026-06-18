import { Suspense } from "react";
import { PatientAppointments } from "@/features/patient/appointments/appointments";

export default function PatientAppointmentsPage() {
  return (
    <Suspense fallback={null}>
      <PatientAppointments />
    </Suspense>
  );
}
