const APP_VERSION = "1.0-web";
const DB_KEY = "controle-estoque-web-db";

const money = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const state = {
  screen: "dashboard",
  reportTab: "dashboard",
  reportType: "users",
  search: "",
  user: null,
  db: loadDb(),
};

function initialDb() {
  const now = Date.now();
  return {
    users: [
      { id: 1, code: "USR-001", name: "Administrador", login: "admin", password: "admin", active: true },
    ],
    categories: [
      { id: 1, name: "Informatica" },
      { id: 2, name: "Material de escritorio" },
      { id: 3, name: "Material de limpeza" },
    ],
    products: [
      { id: 1, code: "PRD-001", name: "Notebook Dell", category: "Informatica", quantity: 8, minQuantity: 5, maxQuantity: 20, unitPrice: 3450 },
      { id: 2, code: "PRD-002", name: "Mouse sem fio", category: "Informatica", quantity: 35, minQuantity: 10, maxQuantity: 80, unitPrice: 89.9 },
      { id: 3, code: "PRD-003", name: "Teclado mecanico", category: "Informatica", quantity: 16, minQuantity: 8, maxQuantity: 40, unitPrice: 249.9 },
    ],
    movements: [
      { id: 1, productId: 1, productCode: "PRD-001", productName: "Notebook Dell", unitPrice: 3450, type: "entry", quantity: 10, createdAt: now - 172800000 },
      { id: 2, productId: 2, productCode: "PRD-002", productName: "Mouse sem fio", unitPrice: 89.9, type: "entry", quantity: 40, createdAt: now - 86400000 },
      { id: 3, productId: 1, productCode: "PRD-001", productName: "Notebook Dell", unitPrice: 3450, type: "exit", quantity: 2, createdAt: now - 18000000 },
    ],
  };
}

function loadDb() {
  const saved = localStorage.getItem(DB_KEY);
  if (!saved) return initialDb();
  try {
    return { ...initialDb(), ...JSON.parse(saved) };
  } catch {
    return initialDb();
  }
}

function saveDb() {
  localStorage.setItem(DB_KEY, JSON.stringify(state.db));
}

function nextCode(items, prefix) {
  const next = Math.max(
    0,
    ...items.map((item) => Number(String(item.code || "").split("-").pop()) || 0),
  ) + 1;
  return `${prefix}-${String(next).padStart(3, "0")}`;
}

function nextId(items) {
  return Math.max(0, ...items.map((item) => item.id)) + 1;
}

function render() {
  const app = document.querySelector("#app");
  app.innerHTML = state.user ? appShell() : loginScreen();
  bindEvents();
}

function loginScreen() {
  return `
    <section class="login-shell">
      <form class="login-card stack" data-action="login">
        <h1>Controle de Estoque</h1>
        <div class="muted">Versao ${APP_VERSION}</div>
        <div class="muted">Banco virtual ativo no navegador</div>
        <label>Usuario<input name="login" autocomplete="username" required></label>
        <label>Senha<input name="password" type="password" autocomplete="current-password" required></label>
        <div class="error" data-login-error></div>
        <button type="submit">Entrar</button>
        <div class="muted">Acesso inicial: admin / admin</div>
      </form>
    </section>
  `;
}

function appShell() {
  return `
    <section class="app-shell">
      <aside class="sidebar no-print">
        <div class="brand">Controle de Estoque</div>
        <nav class="nav">
          ${navButton("dashboard", "Painel")}
          ${navButton("products", "Produtos")}
          ${navButton("categories", "Categorias")}
          ${navButton("users", "Usuarios")}
          ${navButton("movements", "Movimentacoes")}
          ${navButton("reports", "Relatorios")}
          <button data-action="logout">Sair</button>
        </nav>
      </aside>
      <section class="content">
        <header class="topbar no-print">
          <strong>${titleForScreen()}</strong>
          <span>Banco virtual: localStorage</span>
        </header>
        <div class="page">${screenContent()}</div>
      </section>
    </section>
  `;
}

function navButton(screen, label) {
  return `<button class="${state.screen === screen ? "active" : ""}" data-screen="${screen}">${label}</button>`;
}

function titleForScreen() {
  return {
    dashboard: "Painel",
    products: "Produtos",
    categories: "Categorias",
    users: "Usuarios",
    movements: "Movimentacoes",
    reports: "Relatorios",
  }[state.screen];
}

function screenContent() {
  if (state.screen === "products") return productsScreen();
  if (state.screen === "categories") return categoriesScreen();
  if (state.screen === "users") return usersScreen();
  if (state.screen === "movements") return movementsScreen();
  if (state.screen === "reports") return reportsScreen();
  return dashboardScreen();
}

function dashboardScreen() {
  const products = state.db.products;
  const movements = state.db.movements;
  const totalProducts = products.length;
  const totalItems = products.reduce((sum, p) => sum + p.quantity, 0);
  const totalValue = products.reduce((sum, p) => sum + p.quantity * p.unitPrice, 0);
  const low = products.filter((p) => p.quantity <= p.minQuantity);
  return `
    <div class="grid">
      ${summaryCard("Produtos cadastrados", totalProducts)}
      ${summaryCard("Itens em estoque", totalItems)}
      ${summaryCard("Valor total em estoque", money.format(totalValue))}
    </div>
    <h3>Alertas de estoque minimo</h3>
    <div class="list">${low.length ? low.map(lowStockCard).join("") : `<p class="muted">Nenhum alerta.</p>`}</div>
    <h3>Ultimas movimentacoes</h3>
    <div class="list">${movements.slice().sort((a, b) => b.createdAt - a.createdAt).map(movementCard).join("")}</div>
  `;
}

function summaryCard(title, value) {
  return `<article class="card"><div class="muted">${title}</div><div class="summary-value">${value}</div></article>`;
}

function lowStockCard(product) {
  return `
    <article class="card low-stock">
      <h4>${escapeHtml(product.name)}</h4>
      <div>Estoque atual: ${product.quantity} | minimo: ${product.minQuantity}</div>
    </article>
  `;
}

function productsScreen() {
  const term = state.search.trim().toLowerCase();
  const products = state.db.products.filter((product) => {
    return !term ||
      product.name.toLowerCase().includes(term) ||
      product.code.toLowerCase().includes(term) ||
      product.category.toLowerCase().includes(term);
  });
  return `
    <div class="toolbar no-print">
      <button data-action="new-product" ${state.db.categories.length ? "" : "disabled"}>Cadastrar produto</button>
      <input data-action="search-products" value="${escapeAttr(state.search)}" placeholder="Pesquisar por nome, codigo ou categoria">
    </div>
    ${state.db.categories.length ? "" : `<p class="muted">Cadastre uma categoria antes de criar produtos.</p>`}
    <div class="list">${products.length ? products.map(productCard).join("") : `<p class="muted">Nenhum produto encontrado.</p>`}</div>
  `;
}

function productCard(product) {
  return `
    <article class="card">
      <div class="item-head">
        <h3>${escapeHtml(product.name)}</h3>
        <div class="row-actions no-print">
          <button class="secondary" data-action="edit-product" data-id="${product.id}">Editar</button>
          <button class="danger" data-action="delete-product" data-id="${product.id}">Excluir</button>
        </div>
      </div>
      <div class="details">
        <span class="muted">Codigo: ${escapeHtml(product.code)}</span>
        <span>Categoria: ${escapeHtml(product.category)}</span>
        <span>Quantidade: ${product.quantity}</span>
        <span>Estoque minimo: ${product.minQuantity}</span>
        <span>Estoque maximo: ${product.maxQuantity}</span>
        <span>Valor unitario: ${money.format(product.unitPrice)}</span>
      </div>
    </article>
  `;
}

function categoriesScreen() {
  return `
    <div class="toolbar no-print"><button data-action="new-category">Cadastrar categoria</button></div>
    <div class="list">${state.db.categories.map(categoryCard).join("")}</div>
  `;
}

function categoryCard(category) {
  return `
    <article class="card">
      <div class="item-head">
        <div>
          <h3>${escapeHtml(category.name)}</h3>
          <div class="muted">Codigo: CAT-${String(category.id).padStart(3, "0")}</div>
        </div>
        <button class="secondary no-print" data-action="edit-category" data-id="${category.id}">Editar</button>
      </div>
    </article>
  `;
}

function usersScreen() {
  return `
    <div class="toolbar no-print"><button data-action="new-user">Cadastrar usuario</button></div>
    <div class="list">${state.db.users.map(userCard).join("")}</div>
  `;
}

function userCard(user) {
  return `
    <article class="card">
      <div class="item-head">
        <div>
          <h3>${escapeHtml(user.name)}</h3>
          <div class="muted">Codigo: ${escapeHtml(user.code)}</div>
          <div>Login: ${escapeHtml(user.login)}</div>
          <div class="muted">${user.active ? "Ativo" : "Inativo"}</div>
        </div>
        <button class="secondary no-print" data-action="edit-user" data-id="${user.id}">Editar</button>
      </div>
    </article>
  `;
}

function movementsScreen() {
  const rows = state.db.movements.slice().sort((a, b) => b.createdAt - a.createdAt);
  return `
    <div class="toolbar no-print"><button data-action="new-movement" ${state.db.products.length ? "" : "disabled"}>Registrar movimentacao</button></div>
    <div class="list">${rows.length ? rows.map(movementCard).join("") : `<p class="muted">Nenhuma movimentacao registrada.</p>`}</div>
  `;
}

function movementCard(movement) {
  const signal = movement.type === "entry" ? "+" : "-";
  const typeLabel = movement.type === "entry" ? "Entrada" : "Saida";
  return `
    <article class="card">
      <div class="item-head">
        <div>
          <h3>${escapeHtml(movement.productName)}</h3>
          <div class="muted">Mov. ${movement.id} | ${typeLabel} de ${movement.quantity} un. em ${dateTime(movement.createdAt)}</div>
        </div>
        <strong>${signal}${movement.quantity}</strong>
      </div>
    </article>
  `;
}

function reportsScreen() {
  return `
    <div class="segmented no-print">
      <button class="${state.reportTab === "dashboard" ? "" : "secondary"}" data-report-tab="dashboard">Dashboard</button>
      <button class="${state.reportTab === "reports" ? "" : "secondary"}" data-report-tab="reports">Relatorios</button>
    </div>
    <br>
    ${state.reportTab === "dashboard" ? reportDashboard() : reportOptions()}
  `;
}

function reportDashboard() {
  const entries = state.db.movements.filter((m) => m.type === "entry").reduce((sum, m) => sum + m.quantity, 0);
  const exits = state.db.movements.filter((m) => m.type === "exit").reduce((sum, m) => sum + m.quantity, 0);
  const maxMovement = Math.max(entries, exits, 1);
  const byCategory = state.db.products.reduce((acc, p) => {
    acc[p.category] = (acc[p.category] || 0) + p.quantity;
    return acc;
  }, {});
  const maxCategory = Math.max(1, ...Object.values(byCategory));
  return `
    <div class="grid">
      ${summaryCard("Total de entradas", entries)}
      ${summaryCard("Total de saidas", exits)}
    </div>
    <br>
    <article class="card chart">
      <h3>Movimentacoes por tipo</h3>
      ${bar("Entradas", entries, maxMovement, "var(--primary)")}
      ${bar("Saidas", exits, maxMovement, "var(--danger)")}
    </article>
    <br>
    <article class="card chart">
      <h3>Estoque por categoria</h3>
      ${Object.keys(byCategory).length ? Object.entries(byCategory).map(([name, total]) => bar(name, total, maxCategory, "var(--secondary)")).join("") : `<p class="muted">Nenhum produto cadastrado.</p>`}
    </article>
  `;
}

function bar(label, value, max, color) {
  const width = Math.max(4, Math.round((value / max) * 100));
  return `
    <div class="bar-row">
      <div class="bar-label"><span>${escapeHtml(label)}</span><strong>${value}</strong></div>
      <div class="bar-track"><div class="bar-fill" style="width:${width}%;background:${color}"></div></div>
    </div>
  `;
}

function reportOptions() {
  const report = buildReport(state.reportType);
  return `
    <div class="toolbar no-print">
      <select data-action="report-type">
        ${[
          ["users", "Usuarios"],
          ["products", "Produtos"],
          ["movements", "Movimentacao"],
          ["entries", "Entradas"],
          ["exits", "Saidas"],
        ].map(([value, label]) => `<option value="${value}" ${state.reportType === value ? "selected" : ""}>${label}</option>`).join("")}
      </select>
      <button data-action="print-report">Imprimir</button>
    </div>
    ${reportHtml(report)}
  `;
}

function buildReport(type) {
  if (type === "users") {
    return {
      title: "Relatorio de usuarios",
      headers: ["Codigo", "Nome", "Usuario", "Status"],
      rows: state.db.users.map((u) => [u.code, u.name, u.login, u.active ? "Ativo" : "Inativo"]),
      totalizer: `Total de usuarios: ${state.db.users.length}`,
    };
  }
  if (type === "products") {
    return {
      title: "Relatorio de produtos",
      headers: ["Codigo", "Descricao", "Categoria", "Quantidade", "Valor unitario"],
      rows: state.db.products.map((p) => [p.code, p.name, p.category, p.quantity, money.format(p.unitPrice)]),
      totalizer: `Total de produtos: ${state.db.products.length}`,
    };
  }
  if (type === "movements") {
    return {
      title: "Relatorio de movimentacao",
      headers: ["Numero", "Data", "Tipo", "Codigo", "Material", "Quantidade"],
      rows: state.db.movements.map((m) => [m.id, dateTime(m.createdAt), m.type === "entry" ? "Entrada" : "Saida", m.productCode, m.productName, m.quantity]),
      totalizer: `Total de movimentacoes: ${state.db.movements.length}`,
    };
  }
  const selected = state.db.movements.filter((m) => m.type === (type === "entries" ? "entry" : "exit"));
  const total = selected.reduce((sum, m) => sum + m.unitPrice * m.quantity, 0);
  return {
    title: type === "entries" ? "Relatorio de entradas" : "Relatorio de saidas",
    headers: ["Codigo", "Descricao", "Quantidade", "Valor unitario", "Total"],
    rows: selected.map((m) => [m.productCode, m.productName, m.quantity, money.format(m.unitPrice), money.format(m.unitPrice * m.quantity)]),
    totalizer: `Total geral: ${money.format(total)}`,
  };
}

function reportHtml(report) {
  return `
    <article class="card" id="print-area">
      <h2>${escapeHtml(report.title)}</h2>
      <div class="table-wrap">
        <table>
          <thead><tr>${report.headers.map((h) => `<th>${escapeHtml(h)}</th>`).join("")}</tr></thead>
          <tbody>
            ${report.rows.length ? report.rows.map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(String(cell))}</td>`).join("")}</tr>`).join("") : `<tr><td colspan="${report.headers.length}"><p class="muted">Nenhum dado disponivel.</p></td></tr>`}
          </tbody>
        </table>
      </div>
      <h3>${escapeHtml(report.totalizer)}</h3>
    </article>
  `;
}

function bindEvents() {
  document.querySelectorAll("[data-screen]").forEach((button) => {
    button.addEventListener("click", () => {
      state.screen = button.dataset.screen;
      render();
    });
  });
  document.querySelector("[data-action='logout']")?.addEventListener("click", () => {
    state.user = null;
    render();
  });
  document.querySelector("[data-action='login']")?.addEventListener("submit", onLogin);
  document.querySelector("[data-action='search-products']")?.addEventListener("input", (event) => {
    state.search = event.target.value;
    render();
  });
  document.querySelectorAll("[data-action]").forEach((element) => {
    const action = element.dataset.action;
    if (action === "new-product") element.addEventListener("click", () => productDialog());
    if (action === "edit-product") element.addEventListener("click", () => productDialog(Number(element.dataset.id)));
    if (action === "delete-product") element.addEventListener("click", () => deleteProduct(Number(element.dataset.id)));
    if (action === "new-category") element.addEventListener("click", () => categoryDialog());
    if (action === "edit-category") element.addEventListener("click", () => categoryDialog(Number(element.dataset.id)));
    if (action === "new-user") element.addEventListener("click", () => userDialog());
    if (action === "edit-user") element.addEventListener("click", () => userDialog(Number(element.dataset.id)));
    if (action === "new-movement") element.addEventListener("click", () => movementDialog());
    if (action === "report-type") element.addEventListener("change", (event) => {
      state.reportType = event.target.value;
      render();
    });
    if (action === "print-report") element.addEventListener("click", () => window.print());
  });
  document.querySelectorAll("[data-report-tab]").forEach((button) => {
    button.addEventListener("click", () => {
      state.reportTab = button.dataset.reportTab;
      render();
    });
  });
}

function onLogin(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const login = String(form.get("login")).trim();
  const password = String(form.get("password"));
  const user = state.db.users.find((u) => u.active && u.login === login && u.password === password);
  if (!user) {
    document.querySelector("[data-login-error]").textContent = "Usuario ou senha invalidos.";
    return;
  }
  state.user = user;
  render();
}

function showModal(title, body, onSubmit) {
  const wrapper = document.createElement("div");
  wrapper.className = "modal-backdrop";
  wrapper.innerHTML = `
    <form class="modal stack">
      <h2>${escapeHtml(title)}</h2>
      <div class="form-grid">${body}</div>
      <div class="error" data-modal-error></div>
      <div class="row-actions">
        <button type="submit">Salvar</button>
        <button class="secondary" type="button" data-close>Cancelar</button>
      </div>
    </form>
  `;
  document.body.appendChild(wrapper);
  wrapper.querySelector("[data-close]").addEventListener("click", () => wrapper.remove());
  wrapper.querySelector("form").addEventListener("submit", (event) => {
    event.preventDefault();
    const error = onSubmit(new FormData(event.currentTarget));
    if (error) {
      wrapper.querySelector("[data-modal-error]").textContent = error;
      return;
    }
    wrapper.remove();
    saveDb();
    render();
  });
}

function productDialog(id) {
  const product = state.db.products.find((p) => p.id === id);
  const code = product?.code || nextCode(state.db.products, "PRD");
  showModal(product ? "Editar produto" : "Novo produto", `
    ${input("name", "Nome", product?.name || "", "full")}
    ${input("code", "Codigo gerado", code)}
    ${select("category", "Categoria", state.db.categories.map((c) => c.name), product?.category || state.db.categories[0]?.name || "")}
    ${input("quantity", "Quantidade", product?.quantity ?? "", "", "number")}
    ${input("minQuantity", "Minimo", product?.minQuantity ?? "", "", "number")}
    ${input("maxQuantity", "Maximo", product?.maxQuantity ?? "", "", "number")}
    ${input("unitPrice", "Valor unitario", product?.unitPrice ?? "", "", "number", "0.01")}
  `, (form) => {
    const data = readForm(form);
    const parsed = numericProduct(data);
    if (!data.name || !data.code || !data.category || !parsed) return "Preencha todos os campos corretamente.";
    if (state.db.products.some((p) => p.id !== id && p.code.toUpperCase() === data.code.toUpperCase())) return "Ja existe produto com este codigo.";
    if (parsed.minQuantity > parsed.maxQuantity) return "O minimo nao pode ser maior que o maximo.";
    if (parsed.quantity > parsed.maxQuantity) return "A quantidade atual nao pode passar do maximo.";
    const nextProduct = { id: id || nextId(state.db.products), code: data.code.toUpperCase(), name: data.name, category: data.category, ...parsed };
    upsert(state.db.products, nextProduct);
  });
}

function numericProduct(data) {
  const parsed = {
    quantity: Number(data.quantity),
    minQuantity: Number(data.minQuantity),
    maxQuantity: Number(data.maxQuantity),
    unitPrice: Number(data.unitPrice),
  };
  return Object.values(parsed).every((value) => Number.isFinite(value)) ? parsed : null;
}

function deleteProduct(id) {
  const product = state.db.products.find((p) => p.id === id);
  if (product && confirm(`Deseja excluir ${product.name}?`)) {
    state.db.products = state.db.products.filter((p) => p.id !== id);
    saveDb();
    render();
  }
}

function categoryDialog(id) {
  const category = state.db.categories.find((c) => c.id === id);
  showModal(category ? "Editar categoria" : "Nova categoria", `
    ${input("name", "Nome", category?.name || "", "full")}
  `, (form) => {
    const data = readForm(form);
    if (!data.name) return "Informe o nome da categoria.";
    if (state.db.categories.some((c) => c.id !== id && c.name.toLowerCase() === data.name.toLowerCase())) return "Ja existe categoria com este nome.";
    if (category) {
      state.db.products.forEach((p) => {
        if (p.category === category.name) p.category = data.name;
      });
    }
    upsert(state.db.categories, { id: id || nextId(state.db.categories), name: data.name });
  });
}

function userDialog(id) {
  const user = state.db.users.find((u) => u.id === id);
  showModal(user ? "Editar usuario" : "Novo usuario", `
    ${input("code", "Codigo", user?.code || nextCode(state.db.users, "USR"))}
    ${input("name", "Nome", user?.name || "")}
    ${input("login", "Usuario", user?.login || "")}
    ${input("password", "Senha", user?.password || "", "", "password")}
    <label>Usuario ativo<select name="active"><option value="true" ${user?.active !== false ? "selected" : ""}>Ativo</option><option value="false" ${user?.active === false ? "selected" : ""}>Inativo</option></select></label>
  `, (form) => {
    const data = readForm(form);
    if (!data.code || !data.name || !data.login || !data.password) return "Preencha todos os campos.";
    if (state.db.users.some((u) => u.id !== id && u.code.toUpperCase() === data.code.toUpperCase())) return "Ja existe usuario com este codigo.";
    if (state.db.users.some((u) => u.id !== id && u.login.toLowerCase() === data.login.toLowerCase())) return "Ja existe usuario com este login.";
    upsert(state.db.users, { id: id || nextId(state.db.users), code: data.code.toUpperCase(), name: data.name, login: data.login, password: data.password, active: data.active === "true" });
  });
}

function movementDialog() {
  showModal("Nova movimentacao", `
    ${select("productId", "Produto", state.db.products.map((p) => [p.id, `${p.name} (${p.quantity} un.)`]))}
    <label>Tipo<select name="type"><option value="entry">Entrada</option><option value="exit">Saida</option></select></label>
    ${input("quantity", "Quantidade", "", "", "number")}
  `, (form) => {
    const data = readForm(form);
    const product = state.db.products.find((p) => p.id === Number(data.productId));
    const quantity = Number(data.quantity);
    if (!product || !Number.isFinite(quantity) || quantity <= 0) return "Informe uma quantidade valida.";
    if (data.type === "exit" && quantity > product.quantity) return "Saida maior que o estoque disponivel.";
    if (data.type === "entry" && product.quantity + quantity > product.maxQuantity) return "Entrada ultrapassa o estoque maximo.";
    product.quantity += data.type === "entry" ? quantity : -quantity;
    state.db.movements.unshift({
      id: nextId(state.db.movements),
      productId: product.id,
      productCode: product.code,
      productName: product.name,
      unitPrice: product.unitPrice,
      type: data.type,
      quantity,
      createdAt: Date.now(),
    });
  });
}

function input(name, label, value = "", cls = "", type = "text", step = "") {
  return `<label class="${cls}">${label}<input name="${name}" type="${type}" value="${escapeAttr(value)}" ${step ? `step="${step}"` : ""} required></label>`;
}

function select(name, label, options, value = "") {
  const html = options.map((option) => {
    const optionValue = Array.isArray(option) ? option[0] : option;
    const optionLabel = Array.isArray(option) ? option[1] : option;
    return `<option value="${escapeAttr(optionValue)}" ${String(optionValue) === String(value) ? "selected" : ""}>${escapeHtml(String(optionLabel))}</option>`;
  }).join("");
  return `<label>${label}<select name="${name}">${html}</select></label>`;
}

function readForm(form) {
  return Object.fromEntries([...form.entries()].map(([key, value]) => [key, String(value).trim()]));
}

function upsert(items, item) {
  const index = items.findIndex((current) => current.id === item.id);
  if (index >= 0) items[index] = item;
  else items.push(item);
}

function dateTime(value) {
  return new Date(value).toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", hour: "2-digit", minute: "2-digit" });
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;",
  })[char]);
}

function escapeAttr(value) {
  return escapeHtml(value);
}

render();
