"use client";

import { useEffect, useMemo, useState } from "react";
import { Plus, RefreshCcw, Save } from "lucide-react";
import { toast } from "sonner";

import { getApiErrorMessage, setApiTimeout, getApiTimeout, getStoredUserClientTimeout, setStoredUserClientTimeout } from "@/services/api";
import { adminService, type AdminSystemSetting } from "@/services/adminService";
import styles from "../admin.module.css";

const keyPattern = /^[a-zA-Z0-9._-]{1,120}$/;

const sourceLabel: Record<AdminSystemSetting["source"], string> = {
  DATABASE: "Từ database",
  APPLICATION_PROPERTIES: "Từ file cấu hình",
  EMPTY: "Chưa có giá trị",
};

const isSensitiveKey = (key: string) => {
  const lowered = key.toLowerCase();
  return lowered.includes("password") || lowered.includes("secret") || lowered.includes("token");
};

export function SettingsManagement() {
  const [settings, setSettings] = useState<AdminSystemSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [savingKeys, setSavingKeys] = useState<Record<string, boolean>>({});
  const [draftValues, setDraftValues] = useState<Record<string, string>>({});
  const [draftDescriptions, setDraftDescriptions] = useState<Record<string, string>>({});
  const [searchQuery, setSearchQuery] = useState("");

  const [newKey, setNewKey] = useState("");
  const [newValue, setNewValue] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [creating, setCreating] = useState(false);
  const [clientTimeout, setClientTimeout] = useState<string>("");
  const [savingTimeout, setSavingTimeout] = useState(false);

  const loadSettings = async (isManualRefresh = false) => {
    try {
      if (isManualRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      const data = await adminService.getSettings();
      setSettings(data);

      const initialDrafts: Record<string, string> = {};
      const initialDescriptions: Record<string, string> = {};
      data.forEach((item) => {
        initialDrafts[item.settingKey] = item.settingValue ?? "";
        initialDescriptions[item.settingKey] = item.description ?? "";
      });
      setDraftValues(initialDrafts);
      setDraftDescriptions(initialDescriptions);
      // set client timeout field from stored user session or from current api timeout
      const userTimeout = getStoredUserClientTimeout();
      if (userTimeout) {
        setClientTimeout(String(userTimeout));
        try {
          setApiTimeout(userTimeout);
        } catch {}
      } else {
        setClientTimeout(String(getApiTimeout()));
      }
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải cấu hình hệ thống"));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    void loadSettings();
  }, []);

  const filteredSettings = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) {
      return settings;
    }

    return settings.filter((item) => {
      const key = item.settingKey.toLowerCase();
      const value = (item.settingValue ?? "").toLowerCase();
      const description = (item.description ?? "").toLowerCase();
      return key.includes(query) || value.includes(query) || description.includes(query);
    });
  }, [settings, searchQuery]);

  const handleSaveSetting = async (settingKey: string) => {
    if (!keyPattern.test(settingKey)) {
      toast.error("Khóa cấu hình không hợp lệ");
      return;
    }

    try {
      setSavingKeys((prev) => ({ ...prev, [settingKey]: true }));
      const nextValue = draftValues[settingKey] ?? "";
      const nextDescription = draftDescriptions[settingKey] ?? "";
      await adminService.updateSetting(settingKey, nextValue, nextDescription);
      toast.success(`Đã lưu cấu hình ${settingKey}`);
      await loadSettings(true);
    } catch (error) {
      toast.error(getApiErrorMessage(error, `Không thể lưu cấu hình ${settingKey}`));
    } finally {
      setSavingKeys((prev) => ({ ...prev, [settingKey]: false }));
    }
  };

  const handleCreateSetting = async () => {
    const key = newKey.trim();
    if (!keyPattern.test(key)) {
      toast.error("Khóa cấu hình chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang");
      return;
    }

    try {
      setCreating(true);
      await adminService.updateSetting(key, newValue.trim(), newDescription.trim());
      toast.success("Đã thêm cấu hình mới");
      setNewKey("");
      setNewValue("");
      setNewDescription("");
      await loadSettings(true);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể thêm cấu hình mới"));
    } finally {
      setCreating(false);
    }
  };

  const handleSaveClientTimeout = async () => {
    const value = clientTimeout.trim();
    const n = Number(value);
    if (Number.isNaN(n) || n <= 0) {
      toast.error("Giá trị timeout không hợp lệ");
      return;
    }

    try {
      setSavingTimeout(true);
      setStoredUserClientTimeout(n);
      toast.success("Đã lưu timeout cho người dùng hiện tại");
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể lưu timeout"));
    } finally {
      setSavingTimeout(false);
    }
  };

  return (
    <main className={styles.mainArea}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1>Cấu hình hệ thống</h1>
          <p>Quản lý cấu hình key-value cho email và các thiết lập khác mà không cần sửa file cấu hình trực tiếp.</p>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.searchWrap}>
            <input
              className={styles.searchInput}
              placeholder="Tìm theo key hoặc value..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>
          <button
            type="button"
            className={styles.outlineButton}
            onClick={() => void loadSettings(true)}
            disabled={refreshing}
          >
            <RefreshCcw size={16} /> {refreshing ? "Đang làm mới..." : "Làm mới"}
          </button>
        </div>

        <section className={styles.card}>
          <h2>Cấu hình timeout client</h2>
          <p>Giá trị timeout (ms) dùng cho các request từ trình duyệt tới API.</p>
          <div className={styles.formGrid}>
            <div>
              <label className={styles.fieldLabel} htmlFor="client-timeout">
                Timeout (ms)
              </label>
              <input
                id="client-timeout"
                className={styles.textInput}
                value={clientTimeout}
                onChange={(e) => setClientTimeout(e.target.value)}
                placeholder="10000"
              />
            </div>
          </div>
          <div className={styles.modalActions}>
            <button type="button" className={styles.primaryButton} onClick={() => void handleSaveClientTimeout()} disabled={savingTimeout}>
              <Save size={16} /> {savingTimeout ? "Đang lưu..." : "Lưu timeout"}
            </button>
          </div>
        </section>

        <section className={styles.card}>
          <div className={styles.formGrid}>
            <div>
              <label className={styles.fieldLabel} htmlFor="new-setting-key">
                Key mới
              </label>
              <input
                id="new-setting-key"
                className={styles.textInput}
                placeholder="vd: app.ui.banner-text"
                value={newKey}
                onChange={(event) => setNewKey(event.target.value)}
              />
            </div>
            <div>
              <label className={styles.fieldLabel} htmlFor="new-setting-value">
                Value
              </label>
              <input
                id="new-setting-value"
                className={styles.textInput}
                placeholder="Giá trị cấu hình"
                value={newValue}
                onChange={(event) => setNewValue(event.target.value)}
              />
            </div>
            <div>
              <label className={styles.fieldLabel} htmlFor="new-setting-description">
                Mô tả
              </label>
              <input
                id="new-setting-description"
                className={styles.textInput}
                placeholder="Mô tả tác dụng của cấu hình"
                value={newDescription}
                onChange={(event) => setNewDescription(event.target.value)}
              />
            </div>
          </div>

          <div className={styles.modalActions}>
            <button type="button" className={styles.primaryButton} onClick={() => void handleCreateSetting()} disabled={creating}>
              <Plus size={16} /> {creating ? "Đang thêm..." : "Thêm cấu hình"}
            </button>
          </div>
        </section>

        <section className={styles.card}>
          {loading ? (
            <div className={styles.emptyBox}>Đang tải cấu hình...</div>
          ) : (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Key</th>
                    <th>Value</th>
                    <th>Mô tả</th>
                    <th>Nguồn</th>
                    <th style={{ textAlign: "right" }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSettings.map((item) => {
                    const key = item.settingKey;
                    const saving = !!savingKeys[key];
                    const inputType = isSensitiveKey(key) ? "password" : "text";

                    return (
                      <tr key={key}>
                        <td className={styles.keyCell}>{key}</td>
                        <td>
                          <input
                            className={styles.textInput}
                            type={inputType}
                            value={draftValues[key] ?? ""}
                            onChange={(event) =>
                              setDraftValues((prev) => ({
                                ...prev,
                                [key]: event.target.value,
                              }))
                            }
                          />
                        </td>
                        <td>
                          <input
                            className={styles.textInput}
                            value={draftDescriptions[key] ?? ""}
                            onChange={(event) =>
                              setDraftDescriptions((prev) => ({
                                ...prev,
                                [key]: event.target.value,
                              }))
                            }
                          />
                        </td>
                        <td>
                          <span
                            className={`${styles.badge} ${
                              item.source === "DATABASE"
                                ? styles.badgeGreen
                                : item.source === "APPLICATION_PROPERTIES"
                                  ? styles.badgeBlue
                                  : styles.badgeGray
                            }`}
                          >
                            {sourceLabel[item.source]}
                          </span>
                        </td>
                        <td>
                          <div className={styles.tableActions}>
                            <button
                              type="button"
                              className={styles.primaryButton}
                              onClick={() => void handleSaveSetting(key)}
                              disabled={saving}
                            >
                              <Save size={16} /> {saving ? "Đang lưu" : "Lưu"}
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {filteredSettings.length === 0 && <div className={styles.emptyBox}>Không tìm thấy cấu hình phù hợp.</div>}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
