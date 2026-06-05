import * as React from "react";

import { cn } from "@/components/ui/utils";

type ButtonVariant =
  | "default"
  | "destructive"
  | "outline"
  | "secondary"
  | "ghost"
  | "link";
type ButtonSize = "default" | "sm" | "lg" | "icon";

const variantClassMap: Record<ButtonVariant, string> = {
  default: "ui-button--default",
  destructive: "ui-button--destructive",
  outline: "ui-button--outline",
  secondary: "ui-button--secondary",
  ghost: "ui-button--ghost",
  link: "ui-button--link",
};

const sizeClassMap: Record<ButtonSize, string> = {
  default: "ui-button--default-size",
  sm: "ui-button--sm",
  lg: "ui-button--lg",
  icon: "ui-button--icon",
};

function Button({
  className,
  variant = "default",
  size = "default",
  ...props
}: React.ComponentProps<"button"> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
}) {
  const classes = cn(
    "ui-button",
    variantClassMap[variant],
    sizeClassMap[size],
    className,
  );

  return (
    <button
      data-slot="button"
      className={classes}
      {...props}
    />
  );
}

export { Button };