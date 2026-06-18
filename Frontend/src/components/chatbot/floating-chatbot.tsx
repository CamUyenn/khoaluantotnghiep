"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { Bot, Loader2, MessageCircle, Send, Trash2, UserRound, X } from "lucide-react";

import { api, getApiErrorMessage, getStoredAuthSession } from "@/services/api";

import styles from "./floating-chatbot.module.css";

type ChatRole = "USER" | "ASSISTANT";

type LocalMessage = {
  id: string;
  role: ChatRole;
  content: string;
  pending?: boolean;
};

type AskResponse = {
  answer: string;
  model: string;
};

type HistoryItem = {
  id: number;
  role: string;
  content: string;
  createdAt: string;
};

const ERROR_REPLY = "Xin lỗi, mình đang gặp sự cố. Bạn thử lại sau nhé! 😅";

const mapRole = (role: string): ChatRole => (String(role).toUpperCase() === "ASSISTANT" ? "ASSISTANT" : "USER");

const createLocalId = (prefix: string) => {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return `${prefix}-${crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
};

export type FloatingChatbotScope = "all" | "landing" | "patients";

interface FloatingChatbotProps {
  visibleScopes?: readonly FloatingChatbotScope[];
}

const PATIENT_ROUTE_PREFIXES = ["/dashboard", "/appointments", "/booking", "/invoices", "/patient-history"];

const isPatientRoute = (pathname: string) => {
  return PATIENT_ROUTE_PREFIXES.some((prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`));
};

const canShowOnPath = (pathname: string, scopes: readonly FloatingChatbotScope[]) => {
  if (scopes.includes("all")) {
    return true;
  }

  if (pathname === "/" && scopes.includes("landing")) {
    return true;
  }

  if (isPatientRoute(pathname) && scopes.includes("patients")) {
    return true;
  }

  return false;
};

export function FloatingChatbot({ visibleScopes = ["all"] }: FloatingChatbotProps) {
  const pathname = usePathname();
  const currentPath = pathname || "/";

  const shouldRender = useMemo(() => canShowOnPath(currentPath, visibleScopes), [currentPath, visibleScopes]);

  const [isOpen, setIsOpen] = useState(false);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [messageInput, setMessageInput] = useState("");
  const [messages, setMessages] = useState<LocalMessage[]>([]);

  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, isOpen]);

  useEffect(() => {
    if (isOpen) {
      setHistoryLoaded(false);
    }
  }, [isOpen]);

  useEffect(() => {
    setIsOpen(false);
  }, [currentPath]);

  useEffect(() => {
    if (!isOpen || historyLoaded) {
      return;
    }

    const isAuthenticated = Boolean(getStoredAuthSession()?.token);
    if (!isAuthenticated) {
      setHistoryLoaded(true);
      return;
    }

    let isMounted = true;
    setIsHistoryLoading(true);

    api
      .get<HistoryItem[]>("/api/chatbot/history")
      .then((response) => {
        if (!isMounted) {
          return;
        }

        const mapped = response.data.map((item) => ({
          id: String(item.id),
          role: mapRole(item.role),
          content: item.content,
        }));

        setMessages(mapped);
      })
      .catch(() => {
        // Ignore history errors for guests/expired sessions and allow chatting normally.
      })
      .finally(() => {
        if (!isMounted) {
          return;
        }
        setIsHistoryLoading(false);
        setHistoryLoaded(true);
      });

    return () => {
      isMounted = false;
    };
  }, [historyLoaded, isOpen]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const trimmed = messageInput.trim();
    if (!trimmed || isSending) {
      return;
    }

    const userMessageId = createLocalId("u");
    const pendingMessageId = createLocalId("a-pending");

    setMessages((prev) => [
      ...prev,
      { id: userMessageId, role: "USER", content: trimmed },
      { id: pendingMessageId, role: "ASSISTANT", content: "", pending: true },
    ]);
    setMessageInput("");
    setIsSending(true);

    try {
      const response = await api.post<AskResponse>("/api/chatbot/ask", { message: trimmed });
      const answer = response.data?.answer?.trim() || ERROR_REPLY;

      setMessages((prev) =>
        prev.map((item) =>
          item.id === pendingMessageId
            ? {
                id: createLocalId("a"),
                role: "ASSISTANT",
                content: answer,
              }
            : item,
        ),
      );
    } catch (error) {
      const serverMessage = getApiErrorMessage(error, ERROR_REPLY);
      const fallback = serverMessage || ERROR_REPLY;

      setMessages((prev) =>
        prev.map((item) =>
          item.id === pendingMessageId
            ? {
                id: createLocalId("a-error"),
                role: "ASSISTANT",
                content: fallback,
              }
            : item,
        ),
      );
    } finally {
      setIsSending(false);
    }
  };

  const clearHistory = async () => {
    const isAuthenticated = Boolean(getStoredAuthSession()?.token);
    if (!isAuthenticated) {
      setMessages([]);
      return;
    }

    try {
      await api.delete("/api/chatbot/history");
      setMessages([]);
    } catch {
      setMessages([]);
    }
  };

  if (!shouldRender) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      {isOpen ? (
        <section className={styles.panel} aria-label="AI chatbot">
          <header className={styles.header}>
            <div className={styles.headerInfo}>
              <Bot size={22} aria-hidden="true" />
              <div>
                <h2 className={styles.title}>Trợ lý ảo</h2>
                <p className={styles.status}>{isSending ? "Đang trả lời..." : "Sẵn sàng hỗ trợ"}</p>
              </div>
            </div>

            <div className={styles.headerActions}>
              <button
                type="button"
                className={styles.iconButton}
                onClick={clearHistory}
                aria-label="Xóa hội thoại"
                title="Xóa hội thoại"
              >
                <Trash2 size={16} />
              </button>
              <button
                type="button"
                className={styles.iconButton}
                onClick={() => setIsOpen(false)}
                aria-label="Đóng chatbot"
                title="Đóng chatbot"
              >
                <X size={18} />
              </button>
            </div>
          </header>

          <div className={styles.messageList}>
            {isHistoryLoading ? (
              <div className={styles.emptyState}>Đang tải lịch sử hội thoại...</div>
            ) : null}

            {!isHistoryLoading && messages.length === 0 ? (
              <div className={styles.emptyState}>Bạn có thể hỏi về lịch khám, dịch vụ hoặc thông tin sức khỏe cơ bản.</div>
            ) : null}

            {messages.map((message) => (
              <article
                key={message.id}
                className={`${styles.messageRow} ${
                  message.role === "USER" ? styles.messageRowUser : styles.messageRowAssistant
                }`}
              >
                {message.role === "ASSISTANT" ? <span className={styles.avatar}>🤖</span> : null}

                <div
                  className={`${styles.bubble} ${
                    message.role === "USER" ? styles.bubbleUser : styles.bubbleAssistant
                  }`}
                >
                  {message.pending ? (
                    <span className={styles.pending}>
                      <Loader2 size={16} className={styles.spin} />
                      Đang xử lý...
                    </span>
                  ) : (
                    message.content
                  )}
                </div>

                {message.role === "USER" ? <span className={styles.avatar}><UserRound size={15} /></span> : null}
              </article>
            ))}
            <div ref={bottomRef} />
          </div>

          <div className={styles.composer}>
            <form className={styles.form} onSubmit={handleSubmit}>
              <input
                className={styles.input}
                type="text"
                placeholder="Nhập tin nhắn..."
                value={messageInput}
                onChange={(event) => setMessageInput(event.target.value)}
                disabled={isSending}
              />
              <button className={styles.sendButton} type="submit" disabled={isSending || !messageInput.trim()}>
                <Send size={19} />
              </button>
            </form>
          </div>
        </section>
      ) : null}

      <button
        type="button"
        className={styles.trigger}
        onClick={() => setIsOpen((prev) => !prev)}
        aria-label={isOpen ? "Đóng trợ lý ảo" : "Mở trợ lý ảo"}
      >
        <MessageCircle size={30} />
        <span className={styles.badge}>AI</span>
      </button>
    </div>
  );
}
