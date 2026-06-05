"use client";

import * as React from "react";
import { XIcon } from "lucide-react";

import { cn } from "./utils";

type DialogContextValue = {
  open: boolean;
  onOpenChange?: (open: boolean) => void;
};

const DialogContext = React.createContext<DialogContextValue | null>(null);

function Dialog({
  open = false,
  onOpenChange,
  children,
  ...props
}: React.ComponentProps<"div"> & {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
}) {
  return (
    <DialogContext.Provider value={{ open, onOpenChange }}>
      <div data-slot="dialog" {...props}>
        {children}
      </div>
    </DialogContext.Provider>
  );
}

function DialogTrigger({
  children,
  onClick,
  ...props
}: React.ComponentProps<"button">) {
  const context = React.useContext(DialogContext);

  return (
    <button
      data-slot="dialog-trigger"
      onClick={(event) => {
        onClick?.(event);
        context?.onOpenChange?.(true);
      }}
      {...props}
    >
      {children}
    </button>
  );
}

function DialogPortal({
  children,
  ...props
}: React.ComponentProps<"div">) {
  return (
    <div data-slot="dialog-portal" {...props}>
      {children}
    </div>
  );
}

function DialogClose({
  children,
  onClick,
  ...props
}: React.ComponentProps<"button">) {
  const context = React.useContext(DialogContext);

  return (
    <button
      data-slot="dialog-close"
      onClick={(event) => {
        onClick?.(event);
        context?.onOpenChange?.(false);
      }}
      {...props}
    >
      {children}
    </button>
  );
}

function DialogOverlay({
  className,
  onClick,
  ...props
}: React.ComponentProps<"div">) {
  const context = React.useContext(DialogContext);

  return (
    <div
      data-slot="dialog-overlay"
      className={cn("ui-dialog-overlay", className)}
      onClick={(event) => {
        onClick?.(event);
        context?.onOpenChange?.(false);
      }}
      {...props}
    />
  );
}

function DialogContent({
  className,
  children,
  onClick,
  ...props
}: React.ComponentProps<"div">) {
  const context = React.useContext(DialogContext);

  if (!context?.open) {
    return null;
  }

  return (
    <DialogPortal data-slot="dialog-portal">
      <DialogOverlay />
      <div
        data-slot="dialog-content"
        className={cn("ui-dialog-content", className)}
        onClick={(event) => {
          event.stopPropagation();
          onClick?.(event);
        }}
        {...props}
      >
        {children}
        <button
          className="ui-dialog-close"
          onClick={() => context.onOpenChange?.(false)}
          type="button"
        >
          <XIcon />
          <span className="sr-only">Close</span>
        </button>
      </div>
    </DialogPortal>
  );
}

function DialogHeader({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="dialog-header"
      className={cn("ui-dialog-header", className)}
      {...props}
    />
  );
}

function DialogFooter({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="dialog-footer"
      className={cn("ui-dialog-footer", className)}
      {...props}
    />
  );
}

function DialogTitle({
  className,
  ...props
}: React.ComponentProps<"h2">) {
  return (
    <h2
      data-slot="dialog-title"
      className={cn("ui-dialog-title", className)}
      {...props}
    />
  );
}

function DialogDescription({
  className,
  ...props
}: React.ComponentProps<"p">) {
  return (
    <p
      data-slot="dialog-description"
      className={cn("ui-dialog-description", className)}
      {...props}
    />
  );
}

export {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
};