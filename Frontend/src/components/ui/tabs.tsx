"use client";

import * as React from "react";

import { cn } from "@/components/ui/utils";

type TabsContextValue = {
  value: string;
  setValue: (nextValue: string) => void;
};

const TabsContext = React.createContext<TabsContextValue | null>(null);

type TabsProps = React.ComponentProps<"div"> & {
  value?: string;
  defaultValue?: string;
  onValueChange?: (value: string) => void;
};

function Tabs({
  className,
  value,
  defaultValue,
  onValueChange,
  children,
  ...props
}: TabsProps) {
  const [internalValue, setInternalValue] = React.useState(defaultValue ?? "");
  const currentValue = value ?? internalValue;

  const setValue = (nextValue: string) => {
    if (value === undefined) {
      setInternalValue(nextValue);
    }
    onValueChange?.(nextValue);
  };

  return (
    <TabsContext.Provider value={{ value: currentValue, setValue }}>
      <div
      data-slot="tabs"
        className={cn("ui-tabs", className)}
        {...props}
      >
        {children}
      </div>
    </TabsContext.Provider>
  );
}

function TabsList({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="tabs-list"
      className={cn("ui-tabs-list", className)}
      {...props}
    />
  );
}

function TabsTrigger({
  className,
  value,
  children,
  onClick,
  ...props
}: React.ComponentProps<"button"> & { value: string }) {
  const context = React.useContext(TabsContext);

  if (!context) {
    throw new Error("TabsTrigger must be used inside Tabs");
  }

  const isActive = context.value === value;

  return (
    <button
      type="button"
      data-slot="tabs-trigger"
      data-state={isActive ? "active" : "inactive"}
      className={cn("ui-tabs-trigger", isActive && "ui-tabs-trigger--active", className)}
      onClick={(event) => {
        context.setValue(value);
        onClick?.(event);
      }}
      {...props}
    >
      {children}
    </button>
  );
}

function TabsContent({
  className,
  value,
  children,
  ...props
}: React.ComponentProps<"div"> & { value: string }) {
  const context = React.useContext(TabsContext);

  if (!context) {
    throw new Error("TabsContent must be used inside Tabs");
  }

  if (context.value !== value) {
    return null;
  }

  return (
    <div
      data-slot="tabs-content"
      className={cn("ui-tabs-content", className)}
      {...props}
    >
      {children}
    </div>
  );
}

export { Tabs, TabsList, TabsTrigger, TabsContent };
