import { BrowserRouter, Route, Routes } from "react-router"
import { Toaster } from "sonner"

import { AuthBootstrap } from "./components/auth-bootstrap"
import { ProtectedRoute } from "./components/protected-route"
import { PublicOnlyRoute } from "./components/public-only-route"
import ChatAppPage from "./pages/ChatAppPage"
import SignInPage from "./pages/SignInPage"
import SignUpPage from "./pages/SignUpPage"

function App() {
  return (
    <>
      <Toaster richColors />
      <BrowserRouter>
        <AuthBootstrap>
          <Routes>
            <Route element={<PublicOnlyRoute />}>
              <Route path="/signin" element={<SignInPage />} />
              <Route path="/signup" element={<SignUpPage />} />
            </Route>
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<ChatAppPage />} />
            </Route>
          </Routes>
        </AuthBootstrap>
      </BrowserRouter>
    </>
  )
}

export default App
