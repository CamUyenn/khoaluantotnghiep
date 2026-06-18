import Link from "next/link";

import { BrandLogo } from "@/components/layout/brand-logo";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

import styles from "./landingpage.module.css";

const services = [
  {
    title: "Khám bệnh tổng quát",
    description: "Khám sức khỏe định kỳ, tư vấn và điều trị các bệnh thông thường.",
    tag: "TQ",
  },
  {
    title: "Khám chuyên khoa",
    description: "Tim mạch, nội tiết, tiêu hóa, hô hấp, da liễu và nhiều chuyên khoa khác.",
    tag: "CK",
  },
  {
    title: "Đặt lịch trực tuyến",
    description: "Đặt lịch nhanh chóng và theo dõi lịch hẹn mọi lúc trên hệ thống.",
    tag: "DL",
  },
];

export function LandingPage() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.container}>
          <BrandLogo titleAs="h1" />

          <nav className={styles.nav}>
            <a href="#services">Dịch vụ</a>
            <a href="#about">Về chúng tôi</a>
            <a href="#contact">Liên hệ</a>
            <Link href="/signin">
              <Button variant="outline">Đăng nhập</Button>
            </Link>
          </nav>
        </div>
      </header>

      <main>
        <section className={styles.hero}>
          <div className={styles.containerHero}>
            <div>
              <p className={styles.heroBadge}>Hệ thống khám chữa bệnh thông minh</p>
              <h2 className={styles.heroTitle}>
                Chăm sóc sức khỏe hiện đại,
                <br />
                nhanh chóng và đáng tin cậy.
              </h2>
              <p className={styles.heroText}>
                Đội ngũ bác sĩ nhiều kinh nghiệm cùng quy trình khám chữa bệnh tiêu chuẩn
                giúp bệnh nhân được phục vụ tốt hơn mỗi ngày.
              </p>
              <div className={styles.heroActions}>
                <Link href="/signin">
                  <Button size="lg">Đặt lịch khám</Button>
                </Link>
                <Link href="/signup">
                  <Button variant="outline">Đăng ký tài khoản mới</Button>
                </Link>
              </div>
            </div>

            <div className={styles.statsGrid}>
              <Card className={styles.statCard}>
                <p className={styles.statNumber}>5000+</p>
                <p className={styles.statLabel}>Bệnh nhân</p>
              </Card>
              <Card className={styles.statCard}>
                <p className={styles.statNumber}>15+</p>
                <p className={styles.statLabel}>Bác sĩ</p>
              </Card>
              <Card className={styles.statCard}>
                <p className={styles.statNumber}>10+</p>
                <p className={styles.statLabel}>Năm kinh nghiệm</p>
              </Card>
              <Card className={styles.statCard}>
                <p className={styles.statNumber}>98%</p>
                <p className={styles.statLabel}>Mức độ hài lòng</p>
              </Card>
            </div>
          </div>
        </section>

        <section id="services" className={styles.section}>
          <div className={styles.containerColumn}>
            <h3 className={styles.sectionTitle}>Dịch vụ y tế</h3>
            <p className={styles.sectionSubtitle}>
              Chúng tôi cung cấp đa dạng các dịch vụ chăm sóc sức khỏe chuyên nghiệp.
            </p>

            <div className={styles.serviceGrid}>
              {services.map((service) => (
                <Card key={service.title} className={styles.serviceCard}>
                  <div className={styles.serviceTag}>{service.tag}</div>
                  <h4 className={styles.serviceTitle}>{service.title}</h4>
                  <p className={styles.serviceDescription}>{service.description}</p>
                </Card>
              ))}
            </div>
          </div>
        </section>

        <section id="about" className={`${styles.section} ${styles.aboutSection}`}>
          <div className={styles.aboutWrap}>
            <div>
              <h3 className={styles.sectionTitle}>Về phòng khám</h3>
              <p className={styles.aboutText}>
                Phòng khám được xây dựng với định hướng lấy bệnh nhân làm trung tâm,
                tập trung vào trải nghiệm khám chữa bệnh thuận tiện và minh bạch.
              </p>
            </div>

            <Card className={styles.workCard}>
              <h4>Giờ làm việc</h4>
              <p>Thứ 2 - Thứ 7: 08:00 - 17:00</p>
              <p>Chủ nhật: 08:00 - 11:00</p>
              <p className={styles.hotline}>Hotline: 1900 1234</p>
            </Card>
          </div>
        </section>
      </main>

      <footer id="contact" className={styles.footer}>
        <div className={styles.footerWrap}>
          <div>
            <p className={styles.footerTitle}>Phòng Khám Đa Khoa</p>
            <p>123 Đường Lý Thường Kiệt, phường Thuận Hóa, TP. Huế</p>
            <p>Email: info@phongkham.vn</p>
          </div>
          <Link href="/signin">
            <Button>Vào trang đăng nhập</Button>
          </Link>
        </div>
      </footer>
    </div>
  );
}
