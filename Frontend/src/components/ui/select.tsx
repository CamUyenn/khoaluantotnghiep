"use client";

import * as React from "react";
import { ChevronDownIcon } from "lucide-react";

import { cn } from "./utils";

type SelectContextValue = {
  value: string;
  setValue: (value: string) => void;
  open: boolean;
  setOpen: (open: boolean) => void;
};

const SelectContext = React.createContext<SelectContextValue | null>(null);

function Select({
  value,
  defaultValue,
  onValueChange,
  children,
  ...props
}: React.ComponentProps<"div"> & {
  value?: string;
  defaultValue?: string;
  onValueChange?: (value: string) => void;
}) {
  const [internalValue, setInternalValue] = React.useState(defaultValue ?? "");
  const [open, setOpen] = React.useState(false);
  const currentValue = value ?? internalValue;

  const setValue = (nextValue: string) => {
    if (value === undefined) {
      setInternalValue(nextValue);
    }
    onValueChange?.(nextValue);
  };

  return (
    <SelectContext.Provider value={{ value: currentValue, setValue, open, setOpen }}>
      <div data-slot="select" {...props}>
        {children}
      </div>
    </SelectContext.Provider>
  );
}

function SelectGroup({
  children,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div data-slot="select-group" {...props}>
      {children}
    </div>
  );
}

function SelectValue({
  placeholder,
  ...props
}: React.ComponentProps<"span"> & { placeholder?: string }) {
  const context = React.useContext(SelectContext);

  return (
    <span data-slot="select-value" {...props}>
      {context?.value || placeholder || "Chọn"}
    </span>
  );
}

function SelectTrigger({
  className,
  onClick,
  children,
  ...props
}: React.ComponentProps<"button">) {
  const context = React.useContext(SelectContext);

  return (
    <button
      type="button"
      data-slot="select-trigger"
      className={cn("ui-select-trigger", className)}
      onClick={(event) => {
        onClick?.(event);
        context?.setOpen(!context.open);
      }}
      {...props}
    >
      {children}
      <ChevronDownIcon className="size-4" />
    </button>
  );
}

function SelectContent({
  className,
  children,
  ...props
}: React.ComponentProps<"div">) {
  const context = React.useContext(SelectContext);

  if (!context?.open) {
    return null;
  }

  return (
    <div data-slot="select-content" className={cn("ui-select-content", className)} {...props}>
      {children}
    </div>
  );
}

function SelectLabel({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="select-label"
      className={cn("ui-select-label", className)}
      {...props}
    />
  );
}

function SelectItem({
  className,
  children,
  onClick,
  value,
  ...props
}: React.ComponentProps<"button"> & { value: string }) {
  const context = React.useContext(SelectContext);

  return (
    <button
      type="button"
      data-slot="select-item"
      className={cn("ui-select-item", className)}
      onClick={(event) => {
        onClick?.(event);
        context?.setValue(value);
        context?.setOpen(false);
      }}
      {...props}
    >
      {children}
    </button>
  );
}

function SelectSeparator({
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="select-separator"
      className={cn("ui-select-separator", className)}
      {...props}
    />
  );
}

function SelectScrollUpButton({
  children,
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="select-scroll-up-button"
      className={cn("ui-select-scroll-button", className)}
      {...props}
    >
      {children}
    </div>
  );
}

function SelectScrollDownButton({
  children,
  className,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="select-scroll-down-button"
      className={cn("ui-select-scroll-button", className)}
      {...props}
    >
      {children}
    </div>
  );
}

export {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectScrollDownButton,
  SelectScrollUpButton,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
};