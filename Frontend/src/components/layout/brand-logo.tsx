import styles from "./brand-logo.module.css";

type BrandLogoProps = {
  className?: string;
  titleAs?: "h1" | "span";
};

export function BrandLogo({ className, titleAs = "span" }: BrandLogoProps) {
  const TitleTag = titleAs;

  return (
    <div className={[styles.brand, className].filter(Boolean).join(" ")}>
      <div className={styles.icon}>+</div>
      <div className={styles.textWrap}>
        <TitleTag className={styles.title}>Phòng Khám Đa Khoa</TitleTag>
        <p className={styles.subtitle}>Chăm sóc sức khỏe toàn diện</p>
      </div>
    </div>
  );
}
