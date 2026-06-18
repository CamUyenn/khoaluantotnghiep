'use client';

import React from 'react';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { BrandLogo } from '@/components/layout/brand-logo';
import { useAuth } from '@/hooks/useAuth';
import { patientService } from '@/services/patientService';
import styles from '@/features/patient/dashboard/dashboard.module.css';
import {
  LayoutDashboard,
  Calendar,
  FileText,
  Receipt,
  LogOut,
  Menu,
  X,
} from 'lucide-react';

export default function PatientsLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const { username, logout } = useAuth();
  const [displayName, setDisplayName] = useState('Bệnh nhân');
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const loadDisplayName = async () => {
      if (username) {
        setDisplayName(username);
      }

      try {
        const profile = await patientService.getProfile();
        if (!isMounted) {
          return;
        }

        setDisplayName(profile.fullName || profile.gmail || username || 'Bệnh nhân');
      } catch {
        if (!isMounted) {
          return;
        }

        setDisplayName(username || 'Bệnh nhân');
      }
    };

    void loadDisplayName();

    return () => {
      isMounted = false;
    };
  }, [username]);

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  const handleLogout = async () => {
    await logout();
    toast.success('Đăng xuất thành công');
    router.push('/');
  };

  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);

  return (
    <div className={styles.container}>
      <aside className={styles.sidebar}>
        <div className={styles.mobileTopBar}>
          <button
            type="button"
            className={styles.hamburgerButton}
            aria-label={mobileMenuOpen ? 'Đóng menu' : 'Mở menu'}
            onClick={() => setMobileMenuOpen((prev) => !prev)}
          >
            {mobileMenuOpen ? <X size={18} /> : <Menu size={18} />}
          </button>

          <div className={styles.mobileUserActions}>
            <div className={styles.mobileUserMeta}>
              <div className={styles.mobileUserName}>{displayName}</div>
              <div className={styles.mobileUserRole}>Bệnh nhân</div>
            </div>
            <button
              className={styles.mobileLogoutButton}
              type="button"
              aria-label="Đăng xuất"
              onClick={() => void handleLogout()}
            >
              <LogOut size={16} />
            </button>
          </div>
        </div>

        <BrandLogo className={styles.logoContainer} />

        <nav className={`${styles.nav} ${mobileMenuOpen ? '' : styles.navCollapsed}`}>
          <Link
            href="/dashboard"
            className={`${styles.navItem} ${isActive('/dashboard') ? styles.active : ''}`}
          >
            <LayoutDashboard size={20} /> Tổng quan
          </Link>
          <Link
            href="/appointments"
            className={`${styles.navItem} ${isActive('/appointments') ? styles.active : ''}`}
          >
            <Calendar size={20} /> Lịch hẹn
          </Link>
          <Link
            href="/patient-history"
            className={`${styles.navItem} ${isActive('/patient-history') ? styles.active : ''}`}
          >
            <FileText size={20} /> Hồ sơ bệnh án
          </Link>
          <Link
            href="/invoices"
            className={`${styles.navItem} ${isActive('/invoices') ? styles.active : ''}`}
          >
            <Receipt size={20} /> Hóa đơn
          </Link>
        </nav>

        <div className={`${styles.sidebarFooter} ${styles.desktopSidebarFooter}`}>
          <div className={styles.userInfo}>
            <img src="https://github.com/shadcn.png" alt="Avatar" className={styles.avatar} />
            <div>
              <div className={styles.userName}>{displayName}</div>
              <div className={styles.userRole}>Bệnh nhân</div>
            </div>
          </div>
          <button
            className={`${styles.navItem} ${styles.logoutButton}`}
            type="button"
            onClick={() => void handleLogout()}
          >
            <LogOut size={20} /> Đăng xuất
          </button>
        </div>
      </aside>

      <div style={{ flex: 1, minWidth: 0 }}>
        {children}
      </div>
    </div>
  );
}
