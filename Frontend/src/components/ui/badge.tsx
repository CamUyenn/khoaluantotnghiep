import * as React from "react";

import { cn } from "./utils";

type BadgeVariant = "default" | "secondary" | "destructive" | "outline";

const badgeVariantClassMap: Record<BadgeVariant, string> = {
  default: "ui-badge--default",
  secondary: "ui-badge--secondary",
  destructive: "ui-badge--destructive",
  outline: "ui-badge--outline",
};

function Badge({
  className,
  variant = "default",
  ...props
}: React.ComponentProps<"span"> & { variant?: BadgeVariant }) {
  const classes = cn("ui-badge", badgeVariantClassMap[variant], className);

  return (
    <span
      data-slot="badge"
      className={classes}
      {...props}
    />
  );
}

export { Badge };
