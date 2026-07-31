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

// ---------- Products ----------

async function loadProducts() {
  const res = await fetch(API_URL);
  const products = await res.json();
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

loadSuppliers().then(loadProducts);
