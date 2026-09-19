import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext";
import App from "./App";
// A Inter estava declarada no CSS e nunca era carregada: o navegador caia
// no proximo item da pilha (system-ui), e o sistema aparecia com uma
// tipografia diferente em cada maquina — nenhuma delas a pretendida.
//
// Servida pelo proprio projeto, e nao pelo Google Fonts: rede de escola cai,
// e o CDN do Google expoe o IP de quem acessa, o que e' dado pessoal.
import "@fontsource-variable/inter";
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
