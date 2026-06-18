'use client';

import { Client } from "@stomp/stompjs";
import { useEffect, useMemo, useRef, useState } from "react";
import { API_BASE_URL } from "@/services/api";

type CashierRefreshListenerProps = {
  enabled?: boolean;
  onRefresh: () => void | Promise<void>;
};

const buildBrokerUrl = () => {
  const base = API_BASE_URL.replace(/^http:/, "ws:").replace(/^https:/, "wss:");
  return `${base.replace(/\/$/, "")}/ws-clinic/websocket`;
};

export function CashierRefreshListener({ enabled = true, onRefresh }: CashierRefreshListenerProps) {
  const [connected, setConnected] = useState(false);
  const callbackRef = useRef(onRefresh);
  const topic = useMemo(() => "/topic/cashier/refresh", []);

  useEffect(() => {
    callbackRef.current = onRefresh;
  }, [onRefresh]);

  useEffect(() => {
    if (!enabled) {
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
        client.subscribe(topic, async () => {
          if (!mounted) {
            return;
          }
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
  }, [enabled, topic]);

  if (!enabled) {
    return null;
  }

  return <span className="sr-only">{connected ? "cashier-refresh-connected" : "cashier-refresh-disconnected"}</span>;
}

export default CashierRefreshListener;
