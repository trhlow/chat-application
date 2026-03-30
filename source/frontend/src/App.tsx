import SignInPage from "./pages/SignInPage"
import { BrowserRouter, Routes, Route } from 'react-router'

function App() {
  return <>
  <BrowserRouter>
    <Routes>
      {/* public routes */}
      <Route 
        path='/signin'
        element={<SignInPage/>}
      />

      {/* private routes */}
    </Routes>
  </BrowserRouter>
  </>;
}
export default App
