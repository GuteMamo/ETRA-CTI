import { type ButtonHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";

type Variant = "primary" | "secondary" | "outline" | "ghost" | "danger";
type Size = "sm" | "md" | "lg" | "icon";

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
}

const variants: Record<Variant, string> = {
  primary: "bg-blue-600 hover:bg-blue-700 text-white shadow-sm",
  secondary: "bg-slate-800 hover:bg-slate-700 text-slate-100",
  outline: "border border-slate-700 hover:bg-slate-800 text-slate-100",
  ghost: "hover:bg-slate-800 text-slate-300 hover:text-white",
  danger: "bg-red-600 hover:bg-red-700 text-white shadow-sm",
};

const sizes: Record<Size, string> = {
  sm: "h-8 px-3 text-xs",
  md: "h-10 px-4 text-sm",
  lg: "h-12 px-6 text-base",
  icon: "h-9 w-9",
};

export const Button = forwardRef<HTMLButtonElement, Props>(
  ({ className, variant = "primary", size = "md", ...props }, ref) => (
    <button
      ref={ref}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-900",
        "disabled:opacity-50 disabled:pointer-events-none",
        variants[variant], sizes[size], className
      )}
      {...props}
    />
  )
);
Button.displayName = "Button";
