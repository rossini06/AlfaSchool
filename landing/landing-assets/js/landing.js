/* AlfaSchool — landing. JS minimo: tema, menu mobile, entrada ao rolar,
   voltar-ao-topo e formulario (com ponto de integracao isolado). */
(function () {
  "use strict";
  var root = document.documentElement;

  /* ---- Tema: localStorage > prefers-color-scheme > claro ---- */
  var btnTema = document.querySelector("[data-theme-toggle]");
  function rotuloTema() {
    return root.getAttribute("data-theme") === "dark"
      ? "Mudar para o tema claro"
      : "Mudar para o tema escuro";
  }
  function aplicaRotuloTema() {
    if (!btnTema) return;
    btnTema.setAttribute("aria-label", rotuloTema());
    btnTema.setAttribute("title", rotuloTema());
  }
  try {
    var salvo = localStorage.getItem("alfaschool_landing_tema");
    if (salvo === "dark" || salvo === "light") {
      root.setAttribute("data-theme", salvo);
    } else if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
      root.setAttribute("data-theme", "dark");
    }
  } catch (e) { /* localStorage bloqueado: fica no tema do HTML */ }
  aplicaRotuloTema();
  if (btnTema) {
    btnTema.addEventListener("click", function () {
      var novo = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
      root.setAttribute("data-theme", novo);
      try { localStorage.setItem("alfaschool_landing_tema", novo); } catch (e) {}
      aplicaRotuloTema();
    });
  }

  /* ---- Menu mobile ---- */
  var btnMenu = document.querySelector("[data-menu-btn]");
  var menu = document.getElementById("menu-mobile");
  function fechaMenu() {
    if (!menu || !btnMenu) return;
    menu.style.display = "none";
    btnMenu.setAttribute("aria-expanded", "false");
    btnMenu.setAttribute("aria-label", "Abrir menu");
  }
  if (btnMenu && menu) {
    btnMenu.addEventListener("click", function () {
      var aberto = menu.style.display === "block";
      menu.style.display = aberto ? "none" : "block";
      btnMenu.setAttribute("aria-expanded", aberto ? "false" : "true");
      btnMenu.setAttribute("aria-label", aberto ? "Abrir menu" : "Fechar menu");
    });
    menu.querySelectorAll("a").forEach(function (a) {
      a.addEventListener("click", fechaMenu);
    });
    window.addEventListener("resize", function () {
      if (window.innerWidth >= 1040) fechaMenu();
    });
  }

  /* ---- Entrada ao rolar (cascata de 90ms entre irmaos) ---- */
  var alvos = Array.prototype.slice.call(document.querySelectorAll("[data-reveal]"));
  var reduz = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (alvos.length && !reduz && "IntersectionObserver" in window) {
    var ordem = new Map();
    alvos.forEach(function (el) {
      var pai = el.parentElement;
      var n = ordem.get(pai) || 0;
      ordem.set(pai, n + 1);
      el.classList.add("lp-reveal-init");
      el.style.transition =
        "opacity .6s cubic-bezier(.2,.7,.3,1) " + (n * 90) + "ms, transform .6s cubic-bezier(.2,.7,.3,1) " + (n * 90) + "ms";
    });
    var mostra = function (el) { el.classList.add("lp-reveal-in"); };
    var obs = new IntersectionObserver(function (entradas) {
      entradas.forEach(function (e) {
        if (e.isIntersecting) { mostra(e.target); obs.unobserve(e.target); }
      });
    }, { rootMargin: "0px 0px -8% 0px", threshold: 0.08 });
    alvos.forEach(function (el) { obs.observe(el); });
    setTimeout(function () { alvos.forEach(mostra); }, 3000); // fallback
  }

  /* ---- Voltar ao topo ---- */
  var btnTopo = document.querySelector("[data-topo]");
  if (btnTopo) {
    var atualiza = function () { btnTopo.hidden = window.scrollY <= 600; };
    atualiza();
    window.addEventListener("scroll", atualiza, { passive: true });
    btnTopo.addEventListener("click", function () {
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }

  /* ---- Ano do rodape ---- */
  var ano = document.querySelector("[data-ano]");
  if (ano) ano.textContent = String(new Date().getFullYear());

  /* ---- Formulario ---- */
  // Ponto de integracao isolado: troque o corpo por um POST ao CRM/API.
  function enviarPedido(dados) {
    console.info("[AlfaSchool] pedido de demonstracao:", dados);
    return Promise.resolve();
  }
  var form = document.querySelector("[data-form]");
  if (form) {
    var erroEl = form.querySelector("[data-form-erro]");
    var sucessoEl = document.querySelector("[data-form-sucesso]");
    var mostraErro = function (msg) {
      if (!erroEl) return;
      erroEl.textContent = msg;
      erroEl.hidden = !msg;
    };
    form.addEventListener("submit", function (e) {
      e.preventDefault();
      var dados = {
        nome: form.nome.value.trim(),
        escola: form.escola.value.trim(),
        email: form.email.value.trim(),
        telefone: form.telefone.value.trim(),
        mensagem: form.mensagem.value.trim(),
      };
      if (!dados.nome || !dados.escola || !dados.email) {
        mostraErro("Preencha nome, escola e e-mail para continuar.");
        return;
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(dados.email)) {
        mostraErro("Confira o e-mail informado.");
        return;
      }
      mostraErro("");
      enviarPedido(dados);
      if (sucessoEl) {
        form.hidden = true;
        sucessoEl.hidden = false;
        sucessoEl.setAttribute("tabindex", "-1");
        sucessoEl.focus();
      }
    });
    var novo = document.querySelector("[data-form-novo]");
    if (novo) {
      novo.addEventListener("click", function () {
        if (sucessoEl) sucessoEl.hidden = true;
        form.hidden = false;
        form.reset();
        mostraErro("");
        form.nome.focus();
      });
    }
  }
})();
