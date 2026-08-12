# Supermarket ERP — Spring MVC Demo

A minimal Spring Boot app demonstrating classic **Controller → Service → Repository** MVC layering, backed by MySQL. Four resources — `Product` (with a barcode-scanner-friendly search endpoint), `Supplier`, `PurchaseOrder` (PO), and `GoodsReceivedNote` (GRN) — with full CRUD, and each module wired into the ones before it: Products link to Suppliers, Purchase Orders are placed against Suppliers for specific Products, and Goods Received Notes record what actually arrived against a Purchase Order, automatically updating Product stock levels.

```
[Browser page + USB scanner]
        |  JSON over HTTP
        v
  Controller  (@RestController - HTTP only, no business logic)
        |
     Service   (business rules)
        |
   Repository  (JpaRepository - DB access)
        |
      MySQL
```

## 1. Prerequisites

### Install Java 17

Spring Boot 3.x requires Java 17 or newer.

- **Windows**: download the installer from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17) (choose JDK 17, `.msi` for Windows), run it, and make sure "Add to PATH" and "Set JAVA_HOME" are checked during install.
- Alternatively with `winget`:
  ```powershell
  winget install EclipseAdoptium.Temurin.17.JDK
  ```

Verify it worked (open a **new** terminal so PATH changes apply):
```powershell
java -version
```
You should see something like `openjdk version "17.0.x"`.

### Install Maven

- **Windows**: download the binary zip from [Maven's download page](https://maven.apache.org/download.cgi), extract it (e.g. to `C:\Program Files\Maven`), then add `<extract-path>\bin` to your `PATH` environment variable (System Properties → Environment Variables → edit `Path`).
- Alternatively with `winget`:
  ```powershell
  winget install Apache.Maven
  ```

Verify:
```powershell
mvn -v
```
You should see the Maven version and the Java version it's using (should match step above).

### Install MySQL

- **Windows**: download the installer from [MySQL's download page](https://dev.mysql.com/downloads/installer/) and run it (choose "Server only" or the full "Developer Default" setup). During setup you'll set a root password — remember it, you'll need it below.
- Alternatively with `winget`:
  ```powershell
  winget install Oracle.MySQL
  ```
- Or run it in Docker instead of installing it locally:
  ```powershell
  docker run --name supermarket-mysql -e MYSQL_ROOT_PASSWORD=<your-password> -p 3306:3306 -d mysql:8
  ```

Verify it's running (either open MySQL Workbench, or):
```powershell
mysql -u root -p -e "SELECT VERSION();"
```

You don't need to create the database or tables by hand — `spring.jpa.hibernate.ddl-auto=update` creates the `supermarket_erp` database's tables automatically on first run (the connection URL below includes `createDatabaseIfNotExist=true`, which creates the database itself if it doesn't exist yet).

## 2. Clone and set up the project

```powershell
git clone https://github.com/Thamel777/Spring-MVC.git
cd Spring-MVC
```

The database credentials are **not** stored in the repo — they're read from environment variables at startup (see [application.properties](src/main/resources/application.properties)). Set them in your terminal session before running the app:

```powershell
$env:MYSQL_DB_URL="jdbc:mysql://localhost:3306/supermarket_erp?createDatabaseIfNotExist=true"
$env:MYSQL_DB_USERNAME="root"
$env:MYSQL_DB_PASSWORD="<your-mysql-password>"
```

Replace `<your-mysql-password>` with the root password you set when installing MySQL. These `$env:` variables only last for the current terminal session — you'll need to re-set them if you open a new window (or set them permanently via System Properties → Environment Variables).

## 3. Run it

```powershell
mvn spring-boot:run
```

On first run, Hibernate auto-creates the `supermarket_erp` database and its `products`/`suppliers` tables (`spring.jpa.hibernate.ddl-auto=update`). Once you see `Started ProductCrudApplication`, open:

```
http://localhost:8081
```

You'll see a page with a barcode-scan input, an add/edit form, and a product table, followed by a supplier management section below it. A USB barcode scanner behaves like a keyboard (types the code, then presses Enter), so clicking into the scan field and scanning "just works."

## 4. API endpoints

### Products

| Method | Path | Description |
|---|---|---|
| POST | `/api/products` | Create a product |
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get a product by id |
| GET | `/api/products/barcode/{barcode}` | Get a product by barcode (used by the scanner) |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |

To link a product to a supplier, send `"supplier": { "id": 3 }` in the request body (or `"supplier": null` for no supplier).

### Suppliers

| Method | Path | Description |
|---|---|---|
| POST | `/api/suppliers` | Create a supplier |
| GET | `/api/suppliers` | List all suppliers |
| GET | `/api/suppliers/{id}` | Get a supplier by id |
| PUT | `/api/suppliers/{id}` | Update a supplier |
| DELETE | `/api/suppliers/{id}` | Delete a supplier (rejected with 409 if products still reference it) |

### Purchase Orders (PO)

| Method | Path | Description |
|---|---|---|
| POST | `/api/purchase-orders` | Create a PO (with a supplier and one or more line items) |
| GET | `/api/purchase-orders` | List all POs |
| GET | `/api/purchase-orders/{id}` | Get a PO by id |
| PUT | `/api/purchase-orders/{id}` | Update a PO (only allowed while status is `PENDING`, except for cancelling) |
| DELETE | `/api/purchase-orders/{id}` | Delete a PO (rejected with 409 if any GRN already references it) |

A PO body looks like:
```json
{
  "poNumber": "PO-1001",
  "supplier": { "id": 3 },
  "orderDate": "2026-08-01",
  "notes": "Monthly dairy restock",
  "items": [
    { "product": { "id": 7 }, "orderedQuantity": 50, "unitPrice": 210.00 }
  ]
}
```

Every PO carries a `status`, which the system moves through automatically as GRNs are recorded against it — you never set it directly except to cancel:

```
PENDING  --(some items received)-->  PARTIALLY_RECEIVED  --(all items fully received)-->  RECEIVED
   |                                          |
   \------------------(cancel)---------------->  CANCELLED
```

### Goods Received Notes (GRN)

| Method | Path | Description |
|---|---|---|
| POST | `/api/grns` | Record goods received against a PO — increases Product stock |
| GET | `/api/grns` | List all GRNs |
| GET | `/api/grns/{id}` | Get a GRN by id |
| PUT | `/api/grns/{id}` | Update a GRN's descriptive fields only (`receivedDate`, `receivedBy`) — quantities are permanent once recorded |
| DELETE | `/api/grns/{id}` | Delete a GRN — reverses the stock and PO-received-quantity effects it caused |

A GRN body looks like:
```json
{
  "grnNumber": "GRN-2001",
  "purchaseOrder": { "id": 4 },
  "location": { "id": 1 },
  "receivedDate": "2026-08-05",
  "receivedBy": { "id": 2 },
  "items": [
    { "purchaseOrderItem": { "id": 12 }, "receivedQuantity": 30 }
  ]
}
```

Rules enforced by `GoodsReceivedNoteServiceImpl`:
- Can't receive against a `CANCELLED` or already fully `RECEIVED` purchase order.
- Can't receive more of an item than is still outstanding on the PO (`OverReceiptException` if you try).
- Each accepted item immediately credits the per-location `Inventory` ledger (see section 6 below) and keeps `Product.quantity` in sync as a running total across all locations.
- After saving, the parent PO's status is recalculated (`PENDING` → `PARTIALLY_RECEIVED` → `RECEIVED`) based on how much of each line has now been received in total.
- Deleting a GRN reverses everything it did: subtracts the quantity back out of stock (both `Inventory` and `Product.quantity`), reduces the PO item's received-so-far count, and recalculates the PO's status again.

## 5. Assignment 03 upgrade — Users, Locations, Inventory, and Sales

This adds the entities from the updated ER diagram: `Role`, `User`, `Location` (branch/warehouse), a proper per-location `Inventory` ledger, and a full Sales/Payment module (`SalesPaymentReceipt`, `SalesReceiptItem`, `Payment`). Same MVC layering as every other module.

**Design decisions made to fill gaps the diagram didn't fully specify** (stated here so they're easy to explain if asked):
- The diagram's `Inventory/Stock` table needs to know *which* location holds stock for a product, but `Goods Receipt`/`Goods Receipt Item` didn't show a location FK. A GRN has to specify where the goods physically arrived for the ledger to make sense, so `location` was added to `GoodsReceivedNote` (one location per whole delivery, not per line — a single delivery normally arrives at one place).
- Same reasoning for Sales: `SalesPaymentReceipt` has a required `location` (which branch/register the sale happened at), needed so a sale can correctly decrease that location's stock.
- `Product.quantity` (from Assignment 02) was **kept**, but is no longer the source of truth — it's now a denormalized running total kept in sync automatically every time `Inventory` changes, purely so the existing simple Product screens still show a sensible number without needing a rewrite.
- `User.password` is stored as plain text. This upgrade only implements the **data structure** from the diagram (entities/relationships), not a login/authentication system — don't reuse this field as-is anywhere real passwords matter.

### New endpoints

| Method | Path | Notes |
|---|---|---|
| CRUD | `/api/roles` | Simple lookup table |
| CRUD | `/api/locations` | Delete blocked (409) if the location still has `Inventory` rows |
| CRUD | `/api/users` | Unique `username` and `email`; delete blocked (409) if the user is a cashier on any sales receipt |
| GET | `/api/inventory`, `/api/inventory/product/{id}`, `/api/inventory/location/{id}` | Read-only — quantities only ever change via GRN or Sales, never edited directly |
| POST, GET, DELETE | `/api/sales-receipts` | Create decreases stock per line (`InsufficientStockException` if not enough); delete reverses it |
| POST, GET, DELETE | `/api/payments` | Records a payment against a receipt; blocks paying more than the remaining balance (`OverpaymentException`); marks the receipt `PAID` once fully covered |

A Sales Receipt body looks like:
```json
{
  "receiptNumber": "RCT-5001",
  "cashier": { "id": 3 },
  "location": { "id": 1 },
  "discount": 50.00,
  "paymentMethod": "CASH",
  "items": [
    { "product": { "id": 7 }, "quantity": 2, "unitPrice": 350.00 }
  ]
}
```
`subtotal` and `totalAmount` are always calculated server-side from `quantity × unitPrice` (minus discount) rather than trusted from the client, same principle as everywhere else in this codebase.

`PurchaseOrder` also gained optional `createdBy` and `approvedBy` `User` links (per the diagram), resolved the same trusted-lookup-by-id way as every other relationship in the app.

**Frontend:** Role, Location, and User have basic Add/Edit/Delete screens (needed so the GRN form's Location and "Received by" dropdowns have real data to select from). The Sales/Payment module currently has a working REST API only — no dedicated POS-style screen has been built for it yet.

## 6. Project structure

```
src/main/java/com/bci/productcrud/
  controller/   -> @RestController - HTTP in, HTTP out, no business logic
  service/      -> business rules (e.g. "barcode must be unique", "supplier email must be unique")
  repository/   -> JpaRepository interfaces - DB access, no SQL written by hand
  model/        -> Product and Supplier entities (JPA-mapped domain objects)
  exception/    -> centralized error handling (@RestControllerAdvice)
src/main/resources/
  application.properties -> config (DB connection, port, JPA settings)
  static/                -> the frontend: index.html + app.js + style.css
```

Request flow for a barcode scan: `app.js` → `GET /api/products/barcode/{code}` → `ProductController` → `ProductService` → `ProductRepository` → MySQL, and the `Product` JSON flows back the same path in reverse.

Product ↔ Supplier link: `Product` holds a `@ManyToOne` reference to `Supplier` (nullable — a product doesn't have to have a supplier). When creating/updating a product, `ProductServiceImpl` resolves the supplier by id through `SupplierService` rather than trusting whatever partial supplier object the client sent, so the persisted relationship always points at a real, managed `Supplier` row. Deleting a supplier that's still referenced by any product is rejected (409 Conflict) — unlink the products first.

PO ↔ GRN ↔ Product chain: `PurchaseOrder` holds a list of `PurchaseOrderItem` (one per product being ordered), each tracking both `orderedQuantity` and a running `receivedQuantity`. `GoodsReceivedNote` holds a list of `GrnItem`, each pointing at one `PurchaseOrderItem` and recording how much of it arrived on that specific delivery. `GoodsReceivedNoteServiceImpl.create()` is the one place in the whole system that increases `Product.quantity` from a delivery — it resolves the PO and its items through the existing services/repositories (not from raw client input), checks the received amount doesn't exceed what's still outstanding, updates the product's stock, updates the PO item's received count, and recalculates the parent PO's overall status — all inside a single `@Transactional` method so a failure partway through rolls everything back together.
