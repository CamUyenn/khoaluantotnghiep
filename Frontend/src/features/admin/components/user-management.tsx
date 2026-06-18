'use client';

import { useEffect, useMemo, useState } from "react";
import { Edit, Lock, Plus, Search, Trash2, Unlock } from "lucide-react";
import { toast } from "sonner";

import { getApiErrorMessage } from "@/services/api";
import { adminService, type AdminRole, type AdminUser } from "@/services/adminService";
import styles from "../admin.module.css";

const roleLabels: Record<AdminRole, string> = {
  ADMIN: "Quản trị viên",
  DOCTOR: "Bác sĩ",
  RECEPTIONIST: "Lễ tân",
  CASHIER: "Thu ngân",
  PATIENT: "Bệnh nhân",
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const normalizeText = (value: unknown) => String(value ?? "").trim();

export function UsersManagement() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [filterRole, setFilterRole] = useState<"all" | AdminRole>("all");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<AdminUser | null>(null);
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    fullName: "",
    email: "",
    phoneNumber: "",
    role: "DOCTOR" as AdminRole,
  });

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await adminService.getUsers();
      setUsers(data);
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể tải danh sách người dùng"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadUsers();
  }, []);

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const fullName = normalizeText(user.fullName).toLowerCase();
      const email = normalizeText(user.email).toLowerCase();
      const phone = normalizeText(user.phoneNumber).toLowerCase();
      const query = normalizeText(searchQuery).toLowerCase();
      const matchesSearch = fullName.includes(query) || email.includes(query) || phone.includes(query);
      const matchesRole = filterRole === "all" || filterRole === user.role;
      return matchesSearch && matchesRole;
    });
  }, [users, searchQuery, filterRole]);

  const formatDate = (iso?: string) => {
    if (!iso) {
      return "-";
    }
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
      return "-";
    }
    return date.toLocaleDateString("vi-VN");
  };

  const openModal = (user?: AdminUser) => {
    if (user) {
      setEditingUser(user);
      setFormData({
        username: normalizeText(user.username),
        password: "",
        fullName: normalizeText(user.fullName),
        email: normalizeText(user.email),
        phoneNumber: normalizeText(user.phoneNumber),
        role: user.role,
      });
    } else {
      setEditingUser(null);
      setFormData({ username: "", password: "", fullName: "", email: "", phoneNumber: "", role: "DOCTOR" });
    }
    setIsModalOpen(true);
  };

  const handleSave = async () => {
    if (!formData.fullName.trim()) {
      toast.error("Vui lòng nhập họ và tên");
      return;
    }

    if (!emailPattern.test(formData.email.trim())) {
      toast.error("Email không hợp lệ");
      return;
    }

    const normalizedPhone = formData.phoneNumber.replace(/\D/g, "");
    if (normalizedPhone.length < 6) {
      toast.error("Số điện thoại phải có ít nhất 6 chữ số");
      return;
    }

    if (formData.role === "ADMIN") {
      toast.error("Không thể tạo hoặc chỉnh vai trò Quản trị viên tại popup này");
      return;
    }

    if (!editingUser && !formData.username.trim()) {
      toast.error("Vui lòng nhập tên đăng nhập");
      return;
    }

    if (!editingUser && !formData.password.trim()) {
      toast.error("Vui lòng nhập mật khẩu");
      return;
    }

    try {
      setSubmitting(true);
      const username = formData.username.trim();

      if (editingUser) {
        const payload: {
          username?: string;
          fullName?: string;
          email?: string;
          phoneNumber?: string;
          role?: AdminRole;
        } = {
          username,
          fullName: formData.fullName.trim(),
          email: formData.email.trim(),
          phoneNumber: formData.phoneNumber.trim(),
          role: formData.role,
        };

        await adminService.updateUser(editingUser.id, payload);
        toast.success("Đã cập nhật tài khoản");
      } else {
        await adminService.createUser({
          username,
          fullName: formData.fullName.trim(),
          email: formData.email.trim(),
          phoneNumber: formData.phoneNumber.trim(),
          role: formData.role,
          password: formData.password.trim(),
          isActive: true,
        });
        toast.success("Đã thêm tài khoản");
      }

      setIsModalOpen(false);
      await loadUsers();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể lưu tài khoản"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Bạn có chắc muốn vô hiệu hóa tài khoản này?")) {
      return;
    }

    try {
      await adminService.deleteUser(id);
      toast.success("Đã vô hiệu hóa tài khoản");
      await loadUsers();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể xóa tài khoản"));
    }
  };

  const handleToggleStatus = async (user: AdminUser) => {
    try {
      await adminService.updateUser(user.id, { isActive: !user.isActive });
      toast.success("Đã cập nhật trạng thái");
      await loadUsers();
    } catch (error) {
      toast.error(getApiErrorMessage(error, "Không thể cập nhật trạng thái"));
    }
  };

  return (
    <main className={styles.mainArea}>
      <div className={styles.container}>
        <div className={styles.header}>
          <h1>Quản lý người dùng</h1>
          <p>Thêm, sửa, khóa và vô hiệu hóa tài khoản hệ thống.</p>
        </div>

        <div className={styles.toolbar}>
          <div className={styles.searchWrap}>
            <Search className={styles.searchIcon} size={18} />
            <input
              className={styles.searchInput}
              placeholder="Tìm kiếm theo tên, email hoặc số điện thoại..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>

          <div className={styles.selectWrap}>
            <select
              className={styles.selectInput}
              value={filterRole}
              onChange={(event) => setFilterRole(event.target.value as "all" | AdminRole)}
            >
              <option value="all">Tất cả vai trò</option>
              <option value="ADMIN">Quản trị viên</option>
              <option value="DOCTOR">Bác sĩ</option>
              <option value="RECEPTIONIST">Lễ tân</option>
              <option value="CASHIER">Thu ngân</option>
              <option value="PATIENT">Bệnh nhân</option>
            </select>
          </div>

          <button type="button" className={styles.primaryButton} onClick={() => openModal()}>
            <Plus size={16} /> Thêm người dùng
          </button>
        </div>

        <section className={styles.card}>
          {loading ? (
            <div className={styles.emptyBox}>Đang tải dữ liệu...</div>
          ) : (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Họ tên</th>
                    <th>Email</th>
                    <th>Số điện thoại</th>
                    <th>Vai trò</th>
                    <th>Trạng thái</th>
                    <th>Ngày tạo</th>
                    <th style={{ textAlign: "right" }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((user) => (
                    <tr key={user.id}>
                      <td>{normalizeText(user.fullName) || "-"}</td>
                      <td>{normalizeText(user.email) || "-"}</td>
                      <td>{normalizeText(user.phoneNumber) || "-"}</td>
                      <td>
                        <span className={`${styles.badge} ${styles.badgeBlue}`}>{roleLabels[user.role]}</span>
                      </td>
                      <td>
                        <span className={`${styles.badge} ${user.isActive ? styles.badgeGreen : styles.badgeGray}`}>
                          {user.isActive ? "Hoạt động" : "Tạm khóa"}
                        </span>
                      </td>
                      <td>{formatDate(user.createdAt)}</td>
                      <td>
                        <div className={styles.tableActions}>
                          <button type="button" className={styles.iconButton} onClick={() => openModal(user)}>
                            <Edit size={16} />
                          </button>
                          <button type="button" className={styles.iconButton} onClick={() => void handleToggleStatus(user)}>
                            {user.isActive ? <Lock size={16} /> : <Unlock size={16} />}
                          </button>
                          <button type="button" className={styles.iconButton} onClick={() => void handleDelete(user.id)}>
                            <Trash2 size={16} color="#dc2626" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {filteredUsers.length === 0 && <div className={styles.emptyBox}>Không tìm thấy người dùng.</div>}
            </div>
          )}
        </section>
      </div>

      {isModalOpen && (
        <>
          <div className={styles.modalOverlay} onClick={() => setIsModalOpen(false)} />
          <div className={styles.modal}>
            <h2 className={styles.modalTitle}>{editingUser ? "Chỉnh sửa người dùng" : "Thêm người dùng mới"}</h2>
            <div style={{ display: "grid", gap: "0.9rem" }}>
              <div>
                <label className={styles.fieldLabel} htmlFor="username">Tên đăng nhập *</label>
                <input
                  id="username"
                  className={styles.textInput}
                  placeholder="ten-dang-nhap"
                  value={formData.username}
                  onChange={(event) => setFormData((prev) => ({ ...prev, username: event.target.value }))}
                  disabled={Boolean(editingUser)}
                />
              </div>
              {!editingUser && (
                <div>
                  <label className={styles.fieldLabel} htmlFor="password">Mật khẩu *</label>
                  <input
                    id="password"
                    type="password"
                    className={styles.textInput}
                    placeholder="Nhập mật khẩu"
                    value={formData.password}
                    onChange={(event) => setFormData((prev) => ({ ...prev, password: event.target.value }))}
                  />
                </div>
              )}
              <div>
                <label className={styles.fieldLabel} htmlFor="fullName">Họ và tên *</label>
                <input
                  id="fullName"
                  className={styles.textInput}
                  placeholder="Nguyễn Văn A"
                  value={formData.fullName}
                  onChange={(event) => setFormData((prev) => ({ ...prev, fullName: event.target.value }))}
                />
              </div>
              <div>
                <label className={styles.fieldLabel} htmlFor="email">Email *</label>
                <input
                  id="email"
                  type="email"
                  className={styles.textInput}
                  placeholder="email@phongkham.vn"
                  value={formData.email}
                  onChange={(event) => setFormData((prev) => ({ ...prev, email: event.target.value }))}
                />
              </div>
              <div>
                <label className={styles.fieldLabel} htmlFor="phone">Số điện thoại *</label>
                <input
                  id="phone"
                  className={styles.textInput}
                  placeholder="0123456789"
                  value={formData.phoneNumber}
                  onChange={(event) => setFormData((prev) => ({ ...prev, phoneNumber: event.target.value }))}
                />
              </div>
              <div>
                <label className={styles.fieldLabel} htmlFor="role">Vai trò *</label>
                <select
                  id="role"
                  className={styles.selectInput}
                  value={formData.role}
                  onChange={(event) => setFormData((prev) => ({ ...prev, role: event.target.value as AdminRole }))}
                >
                  <option value="DOCTOR">Bác sĩ</option>
                  <option value="RECEPTIONIST">Lễ tân</option>
                  <option value="CASHIER">Thu ngân</option>
                  <option value="PATIENT">Bệnh nhân</option>
                </select>
              </div>
            </div>
            <div className={styles.modalActions}>
              <button type="button" className={styles.primaryButton} onClick={() => void handleSave()} disabled={submitting}>
                {editingUser ? "Cập nhật" : "Thêm mới"}
              </button>
              <button type="button" className={styles.outlineButton} onClick={() => setIsModalOpen(false)} disabled={submitting}>
                Hủy
              </button>
            </div>
          </div>
        </>
      )}
    </main>
  );
}
