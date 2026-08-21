const API_URL = '/api/products';
const SUPPLIER_API_URL = '/api/suppliers';
const LOCATION_API_URL = '/api/locations';
const INVENTORY_API_URL = '/api/inventory';
const ROLE_API_URL = '/api/roles';
const USER_API_URL = '/api/users';
const PO_API_URL = '/api/purchase-orders';
const GRN_API_URL = '/api/grns';
const SALE_API_URL = '/api/sales-receipts';
const PAYMENT_API_URL = '/api/payments';

let suppliersCache = [];
let productsCache = [];
let locationsCache = [];
let rolesCache = [];
let usersCache = [];
let purchaseOrdersCache = [];
let grnsCache = [];
let salesCache = [];
let paymentsCache = [];

// ============================================================
// Products
// ============================================================

const form = document.getElementById('product-form');
const formTitle = document.getElementById('form-title');
const idField = document.getElementById('product-id');
const barcodeField = document.getElementById('barcode');
const nameField = document.getElementById('name');
const descriptionField = document.getElementById('description');
const categoryField = document.getElementById('category');
const brandField = document.getElementById('brand');
const sizeField = document.getElementById('size');
const colorField = document.getElementById('color');
const purchasePriceField = document.getElementById('purchase-price');
const sellingPriceField = document.getElementById('selling-price');
const productStatusSelect = document.getElementById('product-status-select');
const supplierSelect = document.getElementById('supplier-select');
const cancelBtn = document.getElementById('cancel-btn');
const tableBody = document.getElementById('product-table-body');

const scanInput = document.getElementById('scan-input');
const scanStatus = document.getElementById('scan-status');

async function loadProducts() {
  const res = await fetch(API_URL);
  productsCache = await res.json();
  tableBody.innerHTML = '';
  productsCache.forEach(p => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${p.barcode}</td>
      <td>${p.name}</td>
      <td>${p.category ?? ''}</td>
      <td>${p.brand ?? ''}</td>
      <td>${p.size ?? ''}</td>
      <td>${p.color ?? ''}</td>
      <td>${p.purchasePrice ?? ''}</td>
      <td>${p.sellingPrice ?? ''}</td>
      <td>${p.status ?? ''}</td>
      <td>${p.totalStock ?? 0}</td>
      <td>${p.supplier ? p.supplier.name : ''}</td>
      <td>
        <button data-action="edit" data-id="${p.id}">Edit</button>
        <button data-action="delete" data-id="${p.id}">Delete</button>
      </td>`;
    tableBody.appendChild(tr);
  });
  populateProductSelects();
}

function resetForm() {
  form.reset();
  idField.value = '';
  productStatusSelect.value = 'ACTIVE';
  formTitle.textContent = 'Add product';
  cancelBtn.hidden = true;
}

function fillForm(product) {
  idField.value = product.id;
  barcodeField.value = product.barcode;
  nameField.value = product.name;
  descriptionField.value = product.description ?? '';
  categoryField.value = product.category ?? '';
  brandField.value = product.brand ?? '';
  sizeField.value = product.size ?? '';
  colorField.value = product.color ?? '';
  purchasePriceField.value = product.purchasePrice;
  sellingPriceField.value = product.sellingPrice;
  productStatusSelect.value = product.status ?? 'ACTIVE';
  supplierSelect.value = product.supplier ? product.supplier.id : '';
  formTitle.textContent = `Edit product #${product.id}`;
  cancelBtn.hidden = false;
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    barcode: barcodeField.value.trim(),
    name: nameField.value.trim(),
    description: descriptionField.value.trim(),
    category: categoryField.value.trim(),
    brand: brandField.value.trim(),
    size: sizeField.value.trim(),
    color: colorField.value.trim(),
    purchasePrice: parseFloat(purchasePriceField.value),
    sellingPrice: parseFloat(sellingPriceField.value),
    status: productStatusSelect.value,
    supplier: supplierSelect.value ? { id: parseInt(supplierSelect.value, 10) } : null,
  };

  const id = idField.value;
  const res = await fetch(id ? `${API_URL}/${id}` : API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetForm();
  loadProducts();
});

cancelBtn.addEventListener('click', resetForm);

tableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${API_URL}/${id}`);
    if (res.ok) fillForm(await res.json());
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this product?')) return;
    const res = await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadProducts();
  }
});

// USB barcode scanners behave like a keyboard: they type the code then send Enter.
scanInput.addEventListener('keydown', async (e) => {
  if (e.key !== 'Enter') return;
  e.preventDefault();

  const barcode = scanInput.value.trim();
  scanInput.value = '';
  if (!barcode) return;

  const res = await fetch(`${API_URL}/barcode/${encodeURIComponent(barcode)}`);
  if (res.ok) {
    const product = await res.json();
    fillForm(product);
    scanStatus.textContent = `Found: ${product.name}`;
    scanStatus.className = 'ok';
  } else {
    resetForm();
    barcodeField.value = barcode;
    scanStatus.textContent = 'Not found — fill in details to add it';
    scanStatus.className = 'error';
  }
});

// ============================================================
// Suppliers
// ============================================================

const supplierForm = document.getElementById('supplier-form');
const supplierFormTitle = document.getElementById('supplier-form-title');
const supplierIdField = document.getElementById('supplier-id');
const supplierNameField = document.getElementById('supplier-name');
const supplierContactField = document.getElementById('supplier-contact');
const supplierPhoneField = document.getElementById('supplier-phone');
const supplierEmailField = document.getElementById('supplier-email');
const supplierAddressField = document.getElementById('supplier-address');
const supplierBankDetailsField = document.getElementById('supplier-bank-details');
const supplierStatusSelect = document.getElementById('supplier-status-select');
const supplierCancelBtn = document.getElementById('supplier-cancel-btn');
const supplierTableBody = document.getElementById('supplier-table-body');

function populateSupplierSelect() {
  const current = supplierSelect.value;
  supplierSelect.innerHTML = '<option value="">-- none --</option>';
  suppliersCache.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s.id;
    opt.textContent = s.name;
    supplierSelect.appendChild(opt);
  });
  supplierSelect.value = current;

  populatePoSupplierSelect();
}

async function loadSuppliers() {
  const res = await fetch(SUPPLIER_API_URL);
  suppliersCache = await res.json();

  supplierTableBody.innerHTML = '';
  suppliersCache.forEach(s => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${s.name}</td>
      <td>${s.contactPerson}</td>
      <td>${s.phone}</td>
      <td>${s.email}</td>
      <td>${s.address ?? ''}</td>
      <td>${s.bankDetails ?? ''}</td>
      <td>${s.status ?? ''}</td>
      <td>
        <button data-action="edit" data-id="${s.id}">Edit</button>
        <button data-action="delete" data-id="${s.id}">Delete</button>
      </td>`;
    supplierTableBody.appendChild(tr);
  });

  populateSupplierSelect();
}

function resetSupplierForm() {
  supplierForm.reset();
  supplierIdField.value = '';
  supplierStatusSelect.value = 'ACTIVE';
  supplierFormTitle.textContent = 'Add supplier';
  supplierCancelBtn.hidden = true;
}

function fillSupplierForm(supplier) {
  supplierIdField.value = supplier.id;
  supplierNameField.value = supplier.name;
  supplierContactField.value = supplier.contactPerson;
  supplierPhoneField.value = supplier.phone;
  supplierEmailField.value = supplier.email;
  supplierAddressField.value = supplier.address ?? '';
  supplierBankDetailsField.value = supplier.bankDetails ?? '';
  supplierStatusSelect.value = supplier.status ?? 'ACTIVE';
  supplierFormTitle.textContent = `Edit supplier #${supplier.id}`;
  supplierCancelBtn.hidden = false;
}

supplierForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    name: supplierNameField.value.trim(),
    contactPerson: supplierContactField.value.trim(),
    phone: supplierPhoneField.value.trim(),
    email: supplierEmailField.value.trim(),
    address: supplierAddressField.value.trim(),
    bankDetails: supplierBankDetailsField.value.trim(),
    status: supplierStatusSelect.value,
  };

  const id = supplierIdField.value;
  const res = await fetch(id ? `${SUPPLIER_API_URL}/${id}` : SUPPLIER_API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetSupplierForm();
  loadSuppliers();
  loadProducts(); // refresh in case a linked supplier's name changed
});

supplierCancelBtn.addEventListener('click', resetSupplierForm);

supplierTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${SUPPLIER_API_URL}/${id}`);
    if (res.ok) fillSupplierForm(await res.json());
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this supplier?')) return;
    const res = await fetch(`${SUPPLIER_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadSuppliers();
  }
});

// ============================================================
// Locations
// ============================================================

const locationForm = document.getElementById('location-form');
const locationFormTitle = document.getElementById('location-form-title');
const locationIdField = document.getElementById('location-id');
const locationNameField = document.getElementById('location-name');
const locationAddressField = document.getElementById('location-address');
const locationCancelBtn = document.getElementById('location-cancel-btn');
const locationTableBody = document.getElementById('location-table-body');

function locationOptionsHtml(selectedId, placeholder) {
  let html = `<option value="">${placeholder}</option>`;
  locationsCache.forEach(l => {
    const sel = String(l.id) === String(selectedId) ? 'selected' : '';
    html += `<option value="${l.id}" ${sel}>${l.locationName}</option>`;
  });
  return html;
}

function populateLocationSelects() {
  const invLoc = document.getElementById('inventory-location-select');
  const grnLoc = document.getElementById('grn-location-select');
  const saleLoc = document.getElementById('sale-location-select');
  [invLoc, grnLoc, saleLoc].forEach(sel => {
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = locationOptionsHtml(null, '-- select location --');
    sel.value = current;
  });
}

async function loadLocations() {
  const res = await fetch(LOCATION_API_URL);
  locationsCache = await res.json();

  locationTableBody.innerHTML = '';
  locationsCache.forEach(l => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${l.locationName}</td>
      <td>${l.address ?? ''}</td>
      <td>
        <button data-action="edit" data-id="${l.id}">Edit</button>
        <button data-action="delete" data-id="${l.id}">Delete</button>
      </td>`;
    locationTableBody.appendChild(tr);
  });

  populateLocationSelects();
}

function resetLocationForm() {
  locationForm.reset();
  locationIdField.value = '';
  locationFormTitle.textContent = 'Add location';
  locationCancelBtn.hidden = true;
}

function fillLocationForm(location) {
  locationIdField.value = location.id;
  locationNameField.value = location.locationName;
  locationAddressField.value = location.address ?? '';
  locationFormTitle.textContent = `Edit location #${location.id}`;
  locationCancelBtn.hidden = false;
}

locationForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    locationName: locationNameField.value.trim(),
    address: locationAddressField.value.trim(),
  };

  const id = locationIdField.value;
  const res = await fetch(id ? `${LOCATION_API_URL}/${id}` : LOCATION_API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetLocationForm();
  loadLocations();
});

locationCancelBtn.addEventListener('click', resetLocationForm);

locationTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${LOCATION_API_URL}/${id}`);
    if (res.ok) fillLocationForm(await res.json());
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this location?')) return;
    const res = await fetch(`${LOCATION_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadLocations();
  }
});

// ============================================================
// Inventory / Stock
// ============================================================

const inventoryForm = document.getElementById('inventory-form');
const inventoryProductSelect = document.getElementById('inventory-product-select');
const inventoryLocationSelect = document.getElementById('inventory-location-select');
const inventoryQuantityField = document.getElementById('inventory-quantity');
const inventoryTableBody = document.getElementById('inventory-table-body');

function populateProductSelects() {
  const targets = [
    'inventory-product-select',
  ];
  targets.forEach(selId => {
    const sel = document.getElementById(selId);
    if (!sel) return;
    const current = sel.value;
    sel.innerHTML = productOptionsHtml(null);
    sel.value = current;
  });
}

async function loadInventory() {
  const res = await fetch(INVENTORY_API_URL);
  const inventory = await res.json();

  inventoryTableBody.innerHTML = '';
  inventory.forEach(inv => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${inv.product ? inv.product.name : ''}</td>
      <td>${inv.location ? inv.location.locationName : ''}</td>
      <td>${inv.quantityOnHand}</td>
      <td>${inv.lastUpdated ? new Date(inv.lastUpdated).toLocaleString() : ''}</td>
      <td><button data-action="delete" data-id="${inv.id}">Delete</button></td>`;
    inventoryTableBody.appendChild(tr);
  });
}

inventoryForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const productId = inventoryProductSelect.value;
  const locationId = inventoryLocationSelect.value;
  if (!productId || !locationId) {
    alert('Select both a product and a location');
    return;
  }

  const res = await fetch(`${INVENTORY_API_URL}/product/${productId}/location/${locationId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantityOnHand: parseInt(inventoryQuantityField.value, 10) }),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  inventoryForm.reset();
  loadInventory();
  loadProducts(); // total stock may have changed
});

inventoryTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this inventory record? (Prefer setting quantity to 0 unless you are removing the location entirely.)')) return;
    const res = await fetch(`${INVENTORY_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadInventory();
    loadProducts();
  }
});

// ============================================================
// Roles
// ============================================================

const roleForm = document.getElementById('role-form');
const roleFormTitle = document.getElementById('role-form-title');
const roleIdField = document.getElementById('role-id');
const roleNameField = document.getElementById('role-name');
const roleCancelBtn = document.getElementById('role-cancel-btn');
const roleTableBody = document.getElementById('role-table-body');

function populateRoleSelect() {
  const sel = document.getElementById('user-role-select');
  if (!sel) return;
  const current = sel.value;
  let html = '<option value="">-- select role --</option>';
  rolesCache.forEach(r => {
    html += `<option value="${r.id}">${r.name}</option>`;
  });
  sel.innerHTML = html;
  sel.value = current;
}

async function loadRoles() {
  const res = await fetch(ROLE_API_URL);
  rolesCache = await res.json();

  roleTableBody.innerHTML = '';
  rolesCache.forEach(r => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${r.name}</td>
      <td>
        <button data-action="edit" data-id="${r.id}">Edit</button>
        <button data-action="delete" data-id="${r.id}">Delete</button>
      </td>`;
    roleTableBody.appendChild(tr);
  });

  populateRoleSelect();
}

function resetRoleForm() {
  roleForm.reset();
  roleIdField.value = '';
  roleFormTitle.textContent = 'Add role';
  roleCancelBtn.hidden = true;
}

function fillRoleForm(role) {
  roleIdField.value = role.id;
  roleNameField.value = role.name;
  roleFormTitle.textContent = `Edit role #${role.id}`;
  roleCancelBtn.hidden = false;
}

roleForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = { name: roleNameField.value.trim() };

  const id = roleIdField.value;
  const res = await fetch(id ? `${ROLE_API_URL}/${id}` : ROLE_API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetRoleForm();
  loadRoles();
});

roleCancelBtn.addEventListener('click', resetRoleForm);

roleTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${ROLE_API_URL}/${id}`);
    if (res.ok) fillRoleForm(await res.json());
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this role?')) return;
    const res = await fetch(`${ROLE_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadRoles();
  }
});

// ============================================================
// Users
// ============================================================

const userForm = document.getElementById('user-form');
const userFormTitle = document.getElementById('user-form-title');
const userIdField = document.getElementById('user-id');
const userFullNameField = document.getElementById('user-fullname');
const userUsernameField = document.getElementById('user-username');
const userPasswordField = document.getElementById('user-password');
const userRoleSelect = document.getElementById('user-role-select');
const userEmailField = document.getElementById('user-email');
const userContactField = document.getElementById('user-contact');
const userStatusSelect = document.getElementById('user-status-select');
const userCancelBtn = document.getElementById('user-cancel-btn');
const userTableBody = document.getElementById('user-table-body');

function userOptionsHtml(selectedId, placeholder) {
  let html = `<option value="">${placeholder}</option>`;
  usersCache.forEach(u => {
    const sel = String(u.id) === String(selectedId) ? 'selected' : '';
    html += `<option value="${u.id}" ${sel}>${u.fullName} (${u.username})</option>`;
  });
  return html;
}

function populateUserSelects() {
  const targets = ['po-created-by-select', 'po-approved-by-select', 'grn-received-by-select', 'sale-cashier-select'];
  targets.forEach(selId => {
    const sel = document.getElementById(selId);
    if (!sel) return;
    const current = sel.value;
    const placeholder = selId === 'sale-cashier-select' ? '-- select cashier --' : '-- none --';
    sel.innerHTML = userOptionsHtml(null, placeholder);
    sel.value = current;
  });
}

async function loadUsers() {
  const res = await fetch(USER_API_URL);
  usersCache = await res.json();

  userTableBody.innerHTML = '';
  usersCache.forEach(u => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${u.fullName}</td>
      <td>${u.username}</td>
      <td>${u.role ? u.role.name : ''}</td>
      <td>${u.email ?? ''}</td>
      <td>${u.contactNo ?? ''}</td>
      <td>${u.status}</td>
      <td>
        <button data-action="edit" data-id="${u.id}">Edit</button>
        <button data-action="delete" data-id="${u.id}">Delete</button>
      </td>`;
    userTableBody.appendChild(tr);
  });

  populateUserSelects();
}

function resetUserForm() {
  userForm.reset();
  userIdField.value = '';
  userFormTitle.textContent = 'Add user';
  userCancelBtn.hidden = true;
}

function fillUserForm(user) {
  userIdField.value = user.id;
  userFullNameField.value = user.fullName;
  userUsernameField.value = user.username;
  userPasswordField.value = '';
  userRoleSelect.value = user.role ? user.role.id : '';
  userEmailField.value = user.email ?? '';
  userContactField.value = user.contactNo ?? '';
  userStatusSelect.value = user.status ?? 'ACTIVE';
  userFormTitle.textContent = `Edit user #${user.id}`;
  userCancelBtn.hidden = false;
}

userForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = userIdField.value;

  if (!id && !userPasswordField.value.trim()) {
    alert('A password is required for new users');
    return;
  }

  const payload = {
    fullName: userFullNameField.value.trim(),
    username: userUsernameField.value.trim(),
    password: userPasswordField.value, // may be blank when editing = keep unchanged
    role: { id: parseInt(userRoleSelect.value, 10) },
    email: userEmailField.value.trim(),
    contactNo: userContactField.value.trim(),
    status: userStatusSelect.value,
  };

  const res = await fetch(id ? `${USER_API_URL}/${id}` : USER_API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetUserForm();
  loadUsers();
});

userCancelBtn.addEventListener('click', resetUserForm);

userTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${USER_API_URL}/${id}`);
    if (res.ok) fillUserForm(await res.json());
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this user?')) return;
    const res = await fetch(`${USER_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadUsers();
  }
});

// ============================================================
// Purchase Orders
// ============================================================

const poForm = document.getElementById('po-form');
const poFormTitle = document.getElementById('po-form-title');
const poIdField = document.getElementById('po-id');
const poNumberField = document.getElementById('po-number');
const poSupplierSelect = document.getElementById('po-supplier-select');
const poOrderDateField = document.getElementById('po-order-date');
const poCreatedBySelect = document.getElementById('po-created-by-select');
const poApprovedBySelect = document.getElementById('po-approved-by-select');
const poNotesField = document.getElementById('po-notes');
const poItemsBody = document.getElementById('po-items-body');
const poAddItemBtn = document.getElementById('po-add-item-btn');
const poCancelBtn = document.getElementById('po-cancel-btn');
const poTableBody = document.getElementById('po-table-body');

function populatePoSupplierSelect() {
  const current = poSupplierSelect.value;
  poSupplierSelect.innerHTML = '<option value="">-- select supplier --</option>';
  suppliersCache.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s.id;
    opt.textContent = s.name;
    poSupplierSelect.appendChild(opt);
  });
  poSupplierSelect.value = current;
}

function productOptionsHtml(selectedId) {
  let html = '<option value="">-- select product --</option>';
  productsCache.forEach(p => {
    const sel = String(p.id) === String(selectedId) ? 'selected' : '';
    html += `<option value="${p.id}" ${sel}>${p.name} (${p.barcode})</option>`;
  });
  return html;
}

function addPoItemRow(item) {
  const tr = document.createElement('tr');
  tr.innerHTML = `
    <td><select class="po-item-product">${productOptionsHtml(item?.product?.id)}</select></td>
    <td><input class="po-item-qty" type="number" min="1" value="${item?.orderedQuantity ?? 1}" required></td>
    <td><input class="po-item-price" type="number" min="0" step="0.01" value="${item?.unitPrice ?? 0}" required></td>
    <td><button type="button" class="po-item-remove">Remove</button></td>`;
  tr.querySelector('.po-item-remove').addEventListener('click', () => tr.remove());
  poItemsBody.appendChild(tr);
}

poAddItemBtn.addEventListener('click', () => addPoItemRow());

function collectPoItems() {
  return Array.from(poItemsBody.querySelectorAll('tr')).map(tr => ({
    product: { id: parseInt(tr.querySelector('.po-item-product').value, 10) },
    orderedQuantity: parseInt(tr.querySelector('.po-item-qty').value, 10),
    unitPrice: parseFloat(tr.querySelector('.po-item-price').value),
  }));
}

function poStatusLabel(status) {
  return status.replaceAll('_', ' ');
}

async function loadPurchaseOrders() {
  const res = await fetch(PO_API_URL);
  purchaseOrdersCache = await res.json();

  poTableBody.innerHTML = '';
  purchaseOrdersCache.forEach(po => {
    const tr = document.createElement('tr');
    const canEdit = po.status === 'PENDING';
    const canCancel = po.status === 'PENDING' || po.status === 'PARTIALLY_RECEIVED';
    tr.innerHTML = `
      <td>${po.poNumber}</td>
      <td>${po.supplier ? po.supplier.name : ''}</td>
      <td>${po.orderDate}</td>
      <td>${poStatusLabel(po.status)}</td>
      <td>${(po.totalAmount ?? 0).toFixed(2)}</td>
      <td>${po.createdBy ? po.createdBy.fullName : ''}</td>
      <td>${po.approvedBy ? po.approvedBy.fullName : ''}</td>
      <td>${po.items.length} item(s)</td>
      <td>
        ${canEdit ? `<button data-action="edit" data-id="${po.id}">Edit</button>` : ''}
        ${canCancel ? `<button data-action="cancel" data-id="${po.id}">Cancel PO</button>` : ''}
        <button data-action="delete" data-id="${po.id}">Delete</button>
      </td>`;
    poTableBody.appendChild(tr);
  });

  populateGrnPoSelect();
}

function resetPoForm() {
  poForm.reset();
  poIdField.value = '';
  poItemsBody.innerHTML = '';
  addPoItemRow();
  poFormTitle.textContent = 'Add purchase order';
  poCancelBtn.hidden = true;
  poOrderDateField.value = new Date().toISOString().slice(0, 10);
}

function fillPoForm(po) {
  poIdField.value = po.id;
  poNumberField.value = po.poNumber;
  poSupplierSelect.value = po.supplier ? po.supplier.id : '';
  poOrderDateField.value = po.orderDate;
  poCreatedBySelect.value = po.createdBy ? po.createdBy.id : '';
  poApprovedBySelect.value = po.approvedBy ? po.approvedBy.id : '';
  poNotesField.value = po.notes ?? '';
  poItemsBody.innerHTML = '';
  po.items.forEach(item => addPoItemRow(item));
  poFormTitle.textContent = `Edit purchase order #${po.id}`;
  poCancelBtn.hidden = false;
}

poForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const items = collectPoItems();
  if (items.length === 0) {
    alert('Add at least one item to the purchase order');
    return;
  }

  const payload = {
    poNumber: poNumberField.value.trim(),
    supplier: { id: parseInt(poSupplierSelect.value, 10) },
    orderDate: poOrderDateField.value,
    createdBy: poCreatedBySelect.value ? { id: parseInt(poCreatedBySelect.value, 10) } : null,
    approvedBy: poApprovedBySelect.value ? { id: parseInt(poApprovedBySelect.value, 10) } : null,
    notes: poNotesField.value.trim(),
    items,
  };

  const id = poIdField.value;
  const res = await fetch(id ? `${PO_API_URL}/${id}` : PO_API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetPoForm();
  loadPurchaseOrders();
});

poCancelBtn.addEventListener('click', resetPoForm);

poTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${PO_API_URL}/${id}`);
    if (res.ok) fillPoForm(await res.json());
  }

  if (btn.dataset.action === 'cancel') {
    if (!confirm('Cancel this purchase order?')) return;
    const po = purchaseOrdersCache.find(p => String(p.id) === String(id));
    const res = await fetch(`${PO_API_URL}/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...po, status: 'CANCELLED' }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Cancel failed');
      return;
    }
    loadPurchaseOrders();
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this purchase order?')) return;
    const res = await fetch(`${PO_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadPurchaseOrders();
  }
});

// ============================================================
// Goods Received Notes
// ============================================================

const grnForm = document.getElementById('grn-form');
const grnFormTitle = document.getElementById('grn-form-title');
const grnIdField = document.getElementById('grn-id');
const grnNumberField = document.getElementById('grn-number');
const grnPoSelect = document.getElementById('grn-po-select');
const grnLocationSelect = document.getElementById('grn-location-select');
const grnReceivedDateField = document.getElementById('grn-received-date');
const grnReceivedBySelect = document.getElementById('grn-received-by-select');
const grnItemsBody = document.getElementById('grn-items-body');
const grnCancelBtn = document.getElementById('grn-cancel-btn');
const grnTableBody = document.getElementById('grn-table-body');

let editingGrnOriginal = null;

function populateGrnPoSelect() {
  const current = grnPoSelect.value;
  grnPoSelect.innerHTML = '<option value="">-- select purchase order --</option>';
  purchaseOrdersCache
    .filter(po => po.status === 'PENDING' || po.status === 'PARTIALLY_RECEIVED')
    .forEach(po => {
      const opt = document.createElement('option');
      opt.value = po.id;
      opt.textContent = `${po.poNumber} (${po.supplier ? po.supplier.name : ''})`;
      grnPoSelect.appendChild(opt);
    });
  grnPoSelect.value = current;
}

function buildGrnItemsFromPo(po) {
  grnItemsBody.innerHTML = '';
  po.items.forEach(item => {
    const remaining = item.orderedQuantity - item.receivedQuantity;
    const tr = document.createElement('tr');
    tr.dataset.poItemId = item.id;
    tr.innerHTML = `
      <td>${item.product.name}</td>
      <td>${item.orderedQuantity}</td>
      <td>${item.receivedQuantity}</td>
      <td><input class="grn-item-qty" type="number" min="0" max="${remaining}" value="0" ${remaining === 0 ? 'disabled' : ''}></td>`;
    grnItemsBody.appendChild(tr);
  });
}

grnPoSelect.addEventListener('change', () => {
  const po = purchaseOrdersCache.find(p => String(p.id) === String(grnPoSelect.value));
  if (po) buildGrnItemsFromPo(po);
  else grnItemsBody.innerHTML = '';
});

function collectGrnItems() {
  return Array.from(grnItemsBody.querySelectorAll('tr'))
    .map(tr => ({
      purchaseOrderItem: { id: parseInt(tr.dataset.poItemId, 10) },
      receivedQuantity: parseInt(tr.querySelector('.grn-item-qty')?.value || '0', 10),
    }))
    .filter(item => item.receivedQuantity > 0);
}

async function loadGrns() {
  const res = await fetch(GRN_API_URL);
  grnsCache = await res.json();

  grnTableBody.innerHTML = '';
  grnsCache.forEach(grn => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${grn.grnNumber}</td>
      <td>${grn.purchaseOrder ? grn.purchaseOrder.poNumber : ''}</td>
      <td>${grn.location ? grn.location.locationName : ''}</td>
      <td>${grn.receivedDate}</td>
      <td>${grn.receivedBy ? grn.receivedBy.fullName : ''}</td>
      <td>${grn.items.length} item(s)</td>
      <td>
        <button data-action="edit" data-id="${grn.id}">Edit</button>
        <button data-action="delete" data-id="${grn.id}">Delete</button>
      </td>`;
    grnTableBody.appendChild(tr);
  });
}

function resetGrnForm() {
  grnForm.reset();
  grnIdField.value = '';
  grnItemsBody.innerHTML = '';
  grnPoSelect.disabled = false;
  grnLocationSelect.disabled = false;
  editingGrnOriginal = null;
  grnFormTitle.textContent = 'Add GRN';
  grnCancelBtn.hidden = true;
  grnReceivedDateField.value = new Date().toISOString().slice(0, 10);
}

function fillGrnForm(grn) {
  editingGrnOriginal = grn;
  grnIdField.value = grn.id;
  grnNumberField.value = grn.grnNumber;
  grnPoSelect.innerHTML = `<option value="${grn.purchaseOrder.id}" selected>${grn.purchaseOrder.poNumber}</option>`;
  grnPoSelect.disabled = true; // the PO a GRN was raised against can't be changed after the fact
  grnLocationSelect.innerHTML = `<option value="${grn.location.id}" selected>${grn.location.locationName}</option>`;
  grnLocationSelect.disabled = true; // same for the receiving location
  grnReceivedDateField.value = grn.receivedDate;
  grnReceivedBySelect.value = grn.receivedBy ? grn.receivedBy.id : '';

  grnItemsBody.innerHTML = '';
  grn.items.forEach(item => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${item.purchaseOrderItem.product.name}</td>
      <td>${item.purchaseOrderItem.orderedQuantity}</td>
      <td>${item.purchaseOrderItem.receivedQuantity}</td>
      <td>${item.receivedQuantity} (already recorded on this GRN, not editable)</td>`;
    grnItemsBody.appendChild(tr);
  });

  grnFormTitle.textContent = `Edit GRN #${grn.id}`;
  grnCancelBtn.hidden = false;
}

grnForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = grnIdField.value;

  let payload;
  if (id) {
    // Editing: only descriptive fields change; re-send original items so validation still passes.
    payload = {
      grnNumber: grnNumberField.value.trim(),
      purchaseOrder: { id: editingGrnOriginal.purchaseOrder.id },
      location: { id: editingGrnOriginal.location.id },
      receivedDate: grnReceivedDateField.value,
      receivedBy: grnReceivedBySelect.value ? { id: parseInt(grnReceivedBySelect.value, 10) } : null,
      items: editingGrnOriginal.items.map(i => ({
        purchaseOrderItem: { id: i.purchaseOrderItem.id },
        receivedQuantity: i.receivedQuantity,
      })),
    };
  } else {
    const items = collectGrnItems();
    if (items.length === 0) {
      alert('Enter a quantity greater than 0 for at least one item');
      return;
    }
    if (!grnLocationSelect.value) {
      alert('Select a receiving location');
      return;
    }
    payload = {
      grnNumber: grnNumberField.value.trim(),
      purchaseOrder: { id: parseInt(grnPoSelect.value, 10) },
      location: { id: parseInt(grnLocationSelect.value, 10) },
      receivedDate: grnReceivedDateField.value,
      receivedBy: grnReceivedBySelect.value ? { id: parseInt(grnReceivedBySelect.value, 10) } : null,
      items,
    };
  }

  const res = await fetch(id ? `${GRN_API_URL}/${id}` : GRN_API_URL, {
    method: id ? 'PUT' : 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetGrnForm();
  loadGrns();
  loadPurchaseOrders(); // PO status and received quantities may have changed
  loadProducts(); // stock quantities may have changed
  loadInventory();
});

grnCancelBtn.addEventListener('click', resetGrnForm);

grnTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'edit') {
    const res = await fetch(`${GRN_API_URL}/${id}`);
    if (res.ok) fillGrnForm(await res.json());
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this GRN? This will reverse the stock it added.')) return;
    const res = await fetch(`${GRN_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadGrns();
    loadPurchaseOrders();
    loadProducts();
    loadInventory();
  }
});

// ============================================================
// Sales / POS
// ============================================================

const saleForm = document.getElementById('sale-form');
const saleCashierSelect = document.getElementById('sale-cashier-select');
const saleLocationSelect = document.getElementById('sale-location-select');
const salePaymentMethodSelect = document.getElementById('sale-payment-method-select');
const saleDiscountField = document.getElementById('sale-discount');
const saleItemsBody = document.getElementById('sale-items-body');
const saleAddItemBtn = document.getElementById('sale-add-item-btn');
const saleTableBody = document.getElementById('sale-table-body');

function addSaleItemRow() {
  const tr = document.createElement('tr');
  tr.innerHTML = `
    <td><select class="sale-item-product">${productOptionsHtml(null)}</select></td>
    <td><input class="sale-item-qty" type="number" min="1" value="1" required></td>
    <td><input class="sale-item-price" type="number" min="0" step="0.01" value="0" required></td>
    <td><button type="button" class="sale-item-remove">Remove</button></td>`;
  const productSelect = tr.querySelector('.sale-item-product');
  const priceInput = tr.querySelector('.sale-item-price');
  productSelect.addEventListener('change', () => {
    const product = productsCache.find(p => String(p.id) === String(productSelect.value));
    if (product) priceInput.value = product.sellingPrice;
  });
  tr.querySelector('.sale-item-remove').addEventListener('click', () => tr.remove());
  saleItemsBody.appendChild(tr);
}

saleAddItemBtn.addEventListener('click', () => addSaleItemRow());

function collectSaleItems() {
  return Array.from(saleItemsBody.querySelectorAll('tr')).map(tr => ({
    product: { id: parseInt(tr.querySelector('.sale-item-product').value, 10) },
    quantity: parseInt(tr.querySelector('.sale-item-qty').value, 10),
    unitPrice: parseFloat(tr.querySelector('.sale-item-price').value),
  }));
}

function resetSaleForm() {
  saleForm.reset();
  saleItemsBody.innerHTML = '';
  addSaleItemRow();
  saleDiscountField.value = 0;
}

async function loadSales() {
  const res = await fetch(SALE_API_URL);
  salesCache = await res.json();

  saleTableBody.innerHTML = '';
  salesCache.forEach(sale => {
    const tr = document.createElement('tr');
    const canVoid = sale.status !== 'VOIDED';
    tr.innerHTML = `
      <td>${sale.receiptNumber}</td>
      <td>${sale.cashier ? sale.cashier.fullName : ''}</td>
      <td>${sale.location ? sale.location.locationName : ''}</td>
      <td>${(sale.totalAmount ?? 0).toFixed(2)}</td>
      <td>${(sale.discount ?? 0).toFixed(2)}</td>
      <td>${sale.paymentMethod ?? ''}</td>
      <td>${sale.status}</td>
      <td>
        ${canVoid ? `<button data-action="void" data-id="${sale.id}">Void</button>` : ''}
        <button data-action="delete" data-id="${sale.id}">Delete</button>
      </td>`;
    saleTableBody.appendChild(tr);
  });

  populatePaymentReceiptSelect();
}

saleForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const items = collectSaleItems();
  if (items.length === 0) {
    alert('Add at least one item to the sale');
    return;
  }

  const payload = {
    cashier: { id: parseInt(saleCashierSelect.value, 10) },
    location: { id: parseInt(saleLocationSelect.value, 10) },
    paymentMethod: salePaymentMethodSelect.value,
    discount: parseFloat(saleDiscountField.value || '0'),
    items,
  };

  const res = await fetch(SALE_API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  resetSaleForm();
  loadSales();
  loadProducts();
  loadInventory();
});

saleTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'void') {
    if (!confirm('Void this sale? This will restore the stock it deducted.')) return;
    const res = await fetch(`${SALE_API_URL}/${id}/void`, { method: 'PUT' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Void failed');
      return;
    }
    loadSales();
    loadProducts();
    loadInventory();
  }

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this sale? This will restore any stock it deducted (if not already voided).')) return;
    const res = await fetch(`${SALE_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadSales();
    loadProducts();
    loadInventory();
  }
});

// ============================================================
// Payments
// ============================================================

const paymentForm = document.getElementById('payment-form');
const paymentReceiptSelect = document.getElementById('payment-receipt-select');
const paymentMethodSelect = document.getElementById('payment-method-select');
const paymentAmountField = document.getElementById('payment-amount');
const paymentTableBody = document.getElementById('payment-table-body');

function populatePaymentReceiptSelect() {
  const current = paymentReceiptSelect.value;
  let html = '<option value="">-- select receipt --</option>';
  salesCache
    .filter(s => s.status !== 'VOIDED')
    .forEach(s => {
      html += `<option value="${s.id}">${s.receiptNumber} (total ${(s.totalAmount ?? 0).toFixed(2)}, ${s.status})</option>`;
    });
  paymentReceiptSelect.innerHTML = html;
  paymentReceiptSelect.value = current;
}

async function loadPayments() {
  const res = await fetch(PAYMENT_API_URL);
  paymentsCache = await res.json();

  paymentTableBody.innerHTML = '';
  paymentsCache.forEach(p => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${p.salesReceipt ? p.salesReceipt.receiptNumber : ''}</td>
      <td>${p.paymentMethod}</td>
      <td>${(p.amount ?? 0).toFixed(2)}</td>
      <td>${p.paidAt ? new Date(p.paidAt).toLocaleString() : ''}</td>
      <td><button data-action="delete" data-id="${p.id}">Delete</button></td>`;
    paymentTableBody.appendChild(tr);
  });
}

paymentForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    salesReceipt: { id: parseInt(paymentReceiptSelect.value, 10) },
    paymentMethod: paymentMethodSelect.value,
    amount: parseFloat(paymentAmountField.value),
  };

  const res = await fetch(PAYMENT_API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    alert(err.message || 'Request failed');
    return;
  }

  paymentForm.reset();
  loadPayments();
  loadSales();
});

paymentTableBody.addEventListener('click', async (e) => {
  const btn = e.target.closest('button');
  if (!btn) return;
  const id = btn.dataset.id;

  if (btn.dataset.action === 'delete') {
    if (!confirm('Delete this payment?')) return;
    const res = await fetch(`${PAYMENT_API_URL}/${id}`, { method: 'DELETE' });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      alert(err.message || 'Delete failed');
      return;
    }
    loadPayments();
    loadSales();
  }
});

// ============================================================
// Initial load
// ============================================================

resetPoForm();
resetGrnForm();
resetSaleForm();

loadLocations()
  .then(loadRoles)
  .then(loadUsers)
  .then(loadSuppliers)
  .then(loadProducts)
  .then(loadInventory)
  .then(loadPurchaseOrders)
  .then(loadGrns)
  .then(loadSales)
  .then(loadPayments);
