'use client';

import React from 'react';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import {
  Activity,
  Building2,
  LayoutDashboard,
  LogOut,
  Pill,
  Settings2,
  UserRound,
  Menu,
  X,
} from 'lucide-react';
import { toast } from 'sonner';
import { BrandLogo } from '@/components/layout/brand-logo';
import { useAuth } from '@/hooks/useAuth';
import styles from '@/styles/common.module.css';

const navItems = [
  { href: '/admin', label: 'Tổng quan', icon: LayoutDashboard },
  { href: '/admin/users', label: 'Người dùng', icon: UserRound },
  { href: '/admin/rooms', label: 'Phòng khám', icon: Building2 },
  { href: '/admin/medicines', label: 'Danh mục thuốc', icon: Pill },
  { href: '/admin/services', label: 'Dịch vụ', icon: Activity },
  { href: '/admin/symptoms', label: 'Triệu chứng', icon: Activity },
  { href: '/admin/disease-categories', label: 'Loại bệnh', icon: Activity },
  { href: '/admin/diagnoses', label: 'Chẩn đoán & Thuốc', icon: Activity },
  { href: '/admin/settings', label: 'Cấu hình', icon: Settings2 },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { username, logout } = useAuth();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [pathname]);

  const handleLogout = async () => {
    await logout();
    toast.success('Đăng xuất thành công');
    router.push('/');
  };

  return (
    <div className={styles.pageLayout}>
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
              <div className={styles.mobileUserName}>{username || 'Quản trị viên'}</div>
              <div className={styles.mobileUserRole}>ADMIN</div>
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
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = item.href === '/admin' ? pathname === item.href : pathname.startsWith(item.href);
            return (
              <Link key={item.href} href={item.href} className={`${styles.navItem} ${isActive ? styles.active : ''}`}>
                <Icon size={18} /> {item.label}
              </Link>
            );
          })}
        </nav>

        <div className={`${styles.sidebarFooter} ${styles.desktopSidebarFooter}`}>
          <div className={styles.userInfo}>
            <img src='https://github.com/shadcn.png' alt='Avatar' className={styles.avatar} />
            <div>
              <div className={styles.userName}>{username || 'Quản trị viên'}</div>
              <div className={styles.userRole}>ADMIN</div>
            </div>
          </div>
          <button className={`${styles.navItem} ${styles.logoutButton}`} type='button' onClick={() => void handleLogout()}>
            <LogOut size={20} /> Đăng xuất
          </button>
        </div>
      </aside>

      {children}
    </div>
  );
}
