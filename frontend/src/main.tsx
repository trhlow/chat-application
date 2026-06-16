import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { Toaster } from "sonner";

import { AppErrorBoundary } from "@/components/app-error-boundary";
import { ThemeProvider } from "@/components/theme-provider";

import { App } from "./App";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ThemeProvider>
      <BrowserRouter>
        <AppErrorBoundary>
          <App />
          <Toaster closeButton position="top-right" richColors />
        </AppErrorBoundary>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>,
);
