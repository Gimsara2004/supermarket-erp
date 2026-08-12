const API_URL = '/api/products';
const SUPPLIER_API_URL = '/api/suppliers';

const form = document.getElementById('product-form');
const formTitle = document.getElementById('form-title');
const idField = document.getElementById('product-id');
const barcodeField = document.getElementById('barcode');
const nameField = document.getElementById('name');
const descriptionField = document.getElementById('description');
const priceField = document.getElementById('price');
const quantityField = document.getElementById('quantity');
const supplierSelect = document.getElementById('supplier-select');
const cancelBtn = document.getElementById('cancel-btn');
const tableBody = document.getElementById('product-table-body');

const scanInput = document.getElementById('scan-input');
const scanStatus = document.getElementById('scan-status');

const supplierForm = document.getElementById('supplier-form');
const supplierFormTitle = document.getElementById('supplier-form-title');
const supplierIdField = document.getElementById('supplier-id');
const supplierNameField = document.getElementById('supplier-name');
const supplierContactField = document.getElementById('supplier-contact');
const supplierPhoneField = document.getElementById('supplier-phone');
const supplierEmailField = document.getElementById('supplier-email');
const supplierAddressField = document.getElementById('supplier-address');
const supplierCancelBtn = document.getElementById('supplier-cancel-btn');
const supplierTableBody = document.getElementById('supplier-table-body');

let suppliersCache = [];
let productsCache = [];

// ---------- Products ----------

async function loadProducts() {
  const res = await fetch(API_URL);
  const products = await res.json();
  productsCache = products;
  tableBody.innerHTML = '';
  products.forEach(p => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${p.barcode}</td>
      <td>${p.name}</td>
      <td>${p.description ?? ''}</td>
      <td>${p.price}</td>
      <td>${p.quantity}</td>
      <td>${p.supplier ? p.supplier.name : ''}</td>
      <td>
        <button data-action="edit" data-id="${p.id}">Edit</button>
        <button data-action="delete" data-id="${p.id}">Delete</button>
      </td>`;
    tableBody.appendChild(tr);
  });
}

function resetForm() {
  form.reset();
  idField.value = '';
  formTitle.textContent = 'Add product';
  cancelBtn.hidden = true;
}

function fillForm(product) {
  idField.value = product.id;
  barcodeField.value = product.barcode;
  nameField.value = product.name;
  descriptionField.value = product.description ?? '';
  priceField.value = product.price;
  quantityField.value = product.quantity;
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
    price: parseFloat(priceField.value),
    quantity: parseInt(quantityField.value, 10),
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
    await fetch(`${API_URL}/${id}`, { method: 'DELETE' });
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

// ---------- Suppliers ----------

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

// ---------- Purchase Orders ----------

const PO_API_URL = '/api/purchase-orders';
const GRN_API_URL = '/api/grns';

const poForm = document.getElementById('po-form');
const poFormTitle = document.getElementById('po-form-title');
const poIdField = document.getElementById('po-id');
const poNumberField = document.getElementById('po-number');
const poSupplierSelect = document.getElementById('po-supplier-select');
const poOrderDateField = document.getElementById('po-order-date');
const poNotesField = document.getElementById('po-notes');
const poItemsBody = document.getElementById('po-items-body');
const poAddItemBtn = document.getElementById('po-add-item-btn');
const poCancelBtn = document.getElementById('po-cancel-btn');
const poTableBody = document.getElementById('po-table-body');

let purchaseOrdersCache = [];

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

// ---------- Goods Received Notes ----------

// ---------- Roles ----------

const ROLE_API_URL = '/api/roles';
const roleForm = document.getElementById('role-form');
const roleFormTitle = document.getElementById('role-form-title');
const roleIdField = document.getElementById('role-id');
const roleNameField = document.getElementById('role-name');
const roleCancelBtn = document.getElementById('role-cancel-btn');
const roleTableBody = document.getElementById('role-table-body');

let rolesCache = [];

async function loadRoles() {
  const res = await fetch(ROLE_API_URL);
  rolesCache = await res.json();

  roleTableBody.innerHTML = '';
  rolesCache.forEach(role => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${role.name}</td>
      <td>
        <button data-action="edit" data-id="${role.id}">Edit</button>
        <button data-action="delete" data-id="${role.id}">Delete</button>
      </td>`;
    roleTableBody.appendChild(tr);
  });

  populateUserRoleSelect();
}

function resetRoleForm() {
  roleForm.reset();
  roleIdField.value = '';
  roleFormTitle.textContent = 'Add role';
  roleCancelBtn.hidden = true;
}

roleForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = roleIdField.value;
  const payload = { name: roleNameField.value.trim() };
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
    const role = rolesCache.find(r => String(r.id) === String(id));
    roleIdField.value = role.id;
    roleNameField.value = role.name;
    roleFormTitle.textContent = `Edit role #${role.id}`;
    roleCancelBtn.hidden = false;
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

// ---------- Locations ----------

const LOCATION_API_URL = '/api/locations';
const locationForm = document.getElementById('location-form');
const locationFormTitle = document.getElementById('location-form-title');
const locationIdField = document.getElementById('location-id');
const locationNameField = document.getElementById('location-name');
const locationAddressField = document.getElementById('location-address');
const locationCancelBtn = document.getElementById('location-cancel-btn');
const locationTableBody = document.getElementById('location-table-body');

let locationsCache = [];

async function loadLocations() {
  const res = await fetch(LOCATION_API_URL);
  locationsCache = await res.json();

  locationTableBody.innerHTML = '';
  locationsCache.forEach(loc => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${loc.name}</td>
      <td>${loc.address ?? ''}</td>
      <td>
        <button data-action="edit" data-id="${loc.id}">Edit</button>
        <button data-action="delete" data-id="${loc.id}">Delete</button>
      </td>`;
    locationTableBody.appendChild(tr);
  });

  populateGrnLocationSelect();
}

function resetLocationForm() {
  locationForm.reset();
  locationIdField.value = '';
  locationFormTitle.textContent = 'Add location';
  locationCancelBtn.hidden = true;
}

locationForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = locationIdField.value;
  const payload = { name: locationNameField.value.trim(), address: locationAddressField.value.trim() };
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
    const loc = locationsCache.find(l => String(l.id) === String(id));
    locationIdField.value = loc.id;
    locationNameField.value = loc.name;
    locationAddressField.value = loc.address ?? '';
    locationFormTitle.textContent = `Edit location #${loc.id}`;
    locationCancelBtn.hidden = false;
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

// ---------- Users ----------

const USER_API_URL = '/api/users';
const userForm = document.getElementById('user-form');
const userFormTitle = document.getElementById('user-form-title');
const userIdField = document.getElementById('user-id');
const userFullNameField = document.getElementById('user-fullname');
const userUsernameField = document.getElementById('user-username');
const userPasswordField = document.getElementById('user-password');
const userRoleSelect = document.getElementById('user-role-select');
const userEmailField = document.getElementById('user-email');
const userContactField = document.getElementById('user-contact');
const userCancelBtn = document.getElementById('user-cancel-btn');
const userTableBody = document.getElementById('user-table-body');

let usersCache = [];

function populateUserRoleSelect() {
  const current = userRoleSelect.value;
  userRoleSelect.innerHTML = '<option value="">-- select role --</option>';
  rolesCache.forEach(r => {
    const opt = document.createElement('option');
    opt.value = r.id;
    opt.textContent = r.name;
    userRoleSelect.appendChild(opt);
  });
  userRoleSelect.value = current;
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
      <td>${u.email}</td>
      <td>${u.status}</td>
      <td>
        <button data-action="edit" data-id="${u.id}">Edit</button>
        <button data-action="delete" data-id="${u.id}">Delete</button>
      </td>`;
    userTableBody.appendChild(tr);
  });

  populateGrnReceivedBySelect();
}

function resetUserForm() {
  userForm.reset();
  userIdField.value = '';
  userFormTitle.textContent = 'Add user';
  userCancelBtn.hidden = true;
}

userForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = userIdField.value;
  const payload = {
    fullName: userFullNameField.value.trim(),
    username: userUsernameField.value.trim(),
    password: userPasswordField.value,
    role: { id: parseInt(userRoleSelect.value, 10) },
    email: userEmailField.value.trim(),
    contactNo: userContactField.value.trim(),
  };
  if (!id && !payload.password) {
    alert('Password is required when creating a new user');
    return;
  }
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
    const u = usersCache.find(x => String(x.id) === String(id));
    userIdField.value = u.id;
    userFullNameField.value = u.fullName;
    userUsernameField.value = u.username;
    userPasswordField.value = '';
    userPasswordField.placeholder = 'Leave blank to keep current password';
    userRoleSelect.value = u.role ? u.role.id : '';
    userEmailField.value = u.email;
    userContactField.value = u.contactNo ?? '';
    userFormTitle.textContent = `Edit user #${u.id}`;
    userCancelBtn.hidden = false;
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

// ---------- Goods Received Notes ----------

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

let grnsCache = [];
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

function populateGrnLocationSelect() {
  const current = grnLocationSelect.value;
  grnLocationSelect.innerHTML = '<option value="">-- select location --</option>';
  locationsCache.forEach(loc => {
    const opt = document.createElement('option');
    opt.value = loc.id;
    opt.textContent = loc.name;
    grnLocationSelect.appendChild(opt);
  });
  grnLocationSelect.value = current;
}

function populateGrnReceivedBySelect() {
  const current = grnReceivedBySelect.value;
  grnReceivedBySelect.innerHTML = '<option value="">-- unspecified --</option>';
  usersCache.forEach(u => {
    const opt = document.createElement('option');
    opt.value = u.id;
    opt.textContent = u.fullName;
    grnReceivedBySelect.appendChild(opt);
  });
  grnReceivedBySelect.value = current;
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
  grnLocationSelect.value = grn.location ? grn.location.id : '';
  grnLocationSelect.disabled = true; // the location goods were received into can't be changed after the fact either
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
      alert('Select a location for this GRN');
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
  }
});

// ---------- Wire supplier save to also refresh PO supplier dropdown ----------
const originalPopulateSupplierSelect = populateSupplierSelect;
populateSupplierSelect = function () {
  originalPopulateSupplierSelect();
  populatePoSupplierSelect();
};

// ---------- Initial load ----------
resetPoForm();
resetRoleForm();
resetLocationForm();
resetUserForm();
resetGrnForm();
loadSuppliers()
  .then(loadProducts)
  .then(loadPurchaseOrders)
  .then(loadRoles)
  .then(loadLocations)
  .then(loadUsers)
  .then(loadGrns);
