'use client';

import { Client } from "@stomp/stompjs";
import { useEffect, useMemo, useRef, useState } from "react";
import { toast } from "sonner";
import { API_BASE_URL } from "@/services/api";

type PaymentRealtimeListenerProps = {
  paymentCode?: string | null;
  enabled?: boolean;
  onPaymentSuccess: () => void | Promise<void>;
  successMessage?: string;
  waitingMessage?: string;
};

const normalizePaymentCode = (value: string) => value.trim().replace(/\s+/g, "_");

const buildBrokerUrl = () => {
  const base = API_BASE_URL.replace(/^http:/, "ws:").replace(/^https:/, "wss:");
  return `${base.replace(/\/$/, "")}/ws-clinic/websocket`;
};

export function PaymentRealtimeListener({
  paymentCode,
  enabled = true,
  onPaymentSuccess,
  successMessage = "Thanh toán đã được xác nhận tự động.",
  waitingMessage = "Hệ thống đang chờ nhận thanh toán...",
}: PaymentRealtimeListenerProps) {
  const [connected, setConnected] = useState(false);
  const handledRef = useRef(false);
  const callbackRef = useRef(onPaymentSuccess);
  const normalizedPaymentCode = useMemo(() => (paymentCode ? normalizePaymentCode(paymentCode) : ""), [paymentCode]);

  useEffect(() => {
    callbackRef.current = onPaymentSuccess;
  }, [onPaymentSuccess]);

  useEffect(() => {
    handledRef.current = false;

    if (!enabled || !normalizedPaymentCode) {
      setConnected(false);
      return;
    }

    let mounted = true;
    const client = new Client({
      brokerURL: buildBrokerUrl(),
      reconnectDelay: 5000,
      onConnect: () => {
        if (!mounted) {
          return;
        }
        setConnected(true);
        client.subscribe(`/topic/payments/${normalizedPaymentCode}`, async (message) => {
          if (!mounted || handledRef.current) {
            return;
          }
          if (message.body !== "PAYMENT_SUCCESS") {
            return;
          }

          handledRef.current = true;
          toast.success(successMessage);
          await callbackRef.current();
        });
      },
      onDisconnect: () => {
        if (mounted) {
          setConnected(false);
        }
      },
      onWebSocketClose: () => {
        if (mounted) {
          setConnected(false);
        }
      },
      onWebSocketError: () => {
        if (mounted) {
          setConnected(false);
        }
      },
    });

    client.activate();

    return () => {
      mounted = false;
      setConnected(false);
      void client.deactivate();
    };
  }, [enabled, normalizedPaymentCode, successMessage]);

  if (!enabled || !normalizedPaymentCode) {
    return null;
  }

  return <p className="text-sm text-muted-foreground animate-pulse">{connected ? waitingMessage : "Đang kết nối thông báo thanh toán..."}</p>;
}

export default PaymentRealtimeListener;