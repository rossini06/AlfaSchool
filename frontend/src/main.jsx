import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import App from "./App";
// A Inter e' servida pelo proprio projeto (public/fonts), declarada no
// topo do global.css e pre-carregada no index.html. Nao vem do Google
// Fonts: rede de escola cai, e o CDN do Google expoe o IP de quem acessa.
import "./styles/global.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
