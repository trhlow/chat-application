import { Component, type ErrorInfo, type ReactNode } from "react";

interface AppErrorBoundaryProps {
  children: ReactNode;
}

interface AppErrorBoundaryState {
  error: Error | null;
}

export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): AppErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Application render error", error, info);
  }

  render() {
    if (!this.state.error) {
      return this.props.children;
    }

    return (
      <main className="grid min-h-screen place-items-center bg-background p-6 text-foreground">
        <section className="w-full max-w-xl rounded-2xl border border-red-500/30 bg-card p-6 shadow-xl">
          <h1 className="text-lg font-bold text-red-600">Không thể hiển thị cuộc trò chuyện</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Đã xảy ra lỗi khi tải dữ liệu. Hãy tải lại trang để thử lại.
          </p>
          <pre className="mt-4 overflow-auto rounded-lg bg-muted p-3 text-xs">
            {this.state.error.message}
          </pre>
          <button
            type="button"
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
            onClick={() => window.location.reload()}
          >
            Tải lại trang
          </button>
        </section>
      </main>
    );
  }
}
