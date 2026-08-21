# Supermarket ERP — Spring MVC Demo

A Spring Boot app demonstrating classic **Controller → Service → Repository** MVC layering, backed by MySQL. It now covers the full warehouse-management schema: `Product`, `Supplier`, `Location`, `Inventory`, `Role`, `User`, `PurchaseOrder` (PO), `GoodsReceivedNote` (GRN), `SalesReceipt` (POS), and `Payment` — with full CRUD on each, and each module wired into the ones before it:

- Products link to Suppliers.
- Purchase Orders are placed against Suppliers for specific Products, and now also record who created and approved them (`User`).
- Goods Received Notes record what actually arrived against a Purchase Order **into a specific Location**, which increases `Inventory` (stock now lives per-product-per-location, not as a single number on `Product`).
- Sales (Point of Sale) deduct `Inventory` at the sale's Location when a receipt is created, and can be settled with one or more `Payment`s.

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

> **Upgrading an existing database?** This version adds several new tables and changes the type of `goods_received_notes.received_by` from text to a foreign key, and removes the old `products.quantity` column in favor of the new `inventory` table. Hibernate's `update` mode cannot safely migrate a column's type on its own, so if you already have an older `supermarket_erp` database, drop it first (`DROP DATABASE supermarket_erp;`) and let the app recreate it fresh on next startup.

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

On first run, Hibernate auto-creates the `supermarket_erp` database and all of its tables (`spring.jpa.hibernate.ddl-auto=update`). A `DataSeeder` also runs once on startup and inserts:
- Three default roles: `ADMIN`, `MANAGER`, `CASHIER`
- One default location: `Main Warehouse`
- One default user: username `admin`, password `admin123` (role `ADMIN`) — change this before using the app for anything real.

Once you see `Started ProductCrudApplication`, open:

```
http://localhost:8081
```

You'll see a sticky nav bar linking to every module: Products, Suppliers, Locations, Inventory, Roles, Users, Purchase Orders, GRNs, Sales/POS, and Payments. A USB barcode scanner behaves like a keyboard (types the code, then presses Enter), so clicking into the Products page's scan field and scanning "just works."

## 4. API endpoints

### Products

| Method | Path | Description |
|---|---|---|
| POST | `/api/products` | Create a product |
| GET | `/api/products` | List all products (each includes a computed `totalStock`, summed across all locations) |
| GET | `/api/products/{id}` | Get a product by id |
| GET | `/api/products/barcode/{barcode}` | Get a product by barcode (used by the scanner) |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |

To link a product to a supplier, send `"supplier": { "id": 3 }` in the request body (or `"supplier": null` for no supplier). **Stock is no longer set on the product itself** — see Inventory below.

### Suppliers

| Method | Path | Description |
|---|---|---|
| POST | `/api/suppliers` | Create a supplier |
| GET | `/api/suppliers` | List all suppliers |
| GET | `/api/suppliers/{id}` | Get a supplier by id |
| PUT | `/api/suppliers/{id}` | Update a supplier |
| DELETE | `/api/suppliers/{id}` | Delete a supplier (rejected with 409 if products still reference it) |

### Locations (Branch / Store / Warehouse)

| Method | Path | Description |
|---|---|---|
| POST | `/api/locations` | Create a location |
| GET | `/api/locations` | List all locations |
| GET | `/api/locations/{id}` | Get a location by id |
| PUT | `/api/locations/{id}` | Update a location |
| DELETE | `/api/locations/{id}` | Delete a location (rejected with 409 if it still has inventory records) |

### Inventory / Stock

| Method | Path | Description |
|---|---|---|
| GET | `/api/inventory` | List every product-at-location stock row |
| GET | `/api/inventory/{id}` | Get one inventory row by id |
| GET | `/api/inventory/product/{productId}` | List stock for one product across all locations |
| PUT | `/api/inventory/product/{productId}/location/{locationId}` | Set the exact quantity on hand for a product at a location (manual stock-take/correction) — body: `{ "quantityOnHand": 42 }` |
| DELETE | `/api/inventory/{id}` | Delete an inventory row |

Stock is now tracked per product **and** per location. GRNs increase it, sales decrease it (and reject the sale with 409 if there isn't enough), and this endpoint lets you correct it directly.

### Roles

| Method | Path | Description |
|---|---|---|
| POST / GET / GET `{id}` / PUT `{id}` / DELETE `{id}` | `/api/roles` | Standard CRUD. Delete is rejected with 409 if any user still has that role. |

### Users

| Method | Path | Description |
|---|---|---|
| POST / GET / GET `{id}` / PUT `{id}` / DELETE `{id}` | `/api/users` | Standard CRUD. `"role": { "id": 2 }` is required. Passwords are salted + SHA-256 hashed server-side and never returned in responses; leave `password` blank on an update to keep it unchanged. |

### Purchase Orders (PO)

| Method | Path | Description |
|---|---|---|
| POST | `/api/purchase-orders` | Create a PO (with a supplier and one or more line items) |
| GET | `/api/purchase-orders` | List all POs |
| GET | `/api/purchase-orders/{id}` | Get a PO by id |
| PUT | `/api/purchase-orders/{id}` | Update a PO (only allowed while status is `PENDING`, except for cancelling and recording an approver) |
| DELETE | `/api/purchase-orders/{id}` | Delete a PO (rejected with 409 if any GRN already references it) |

A PO body looks like:
```json
{
  "poNumber": "PO-1001",
  "supplier": { "id": 3 },
  "orderDate": "2026-08-01",
  "createdBy": { "id": 1 },
  "approvedBy": { "id": 2 },
  "notes": "Monthly dairy restock",
  "items": [
    { "product": { "id": 7 }, "orderedQuantity": 50, "unitPrice": 210.00 }
  ]
}
```

`createdBy`/`approvedBy` are optional `User` references. `totalAmount` in the response is computed on the fly from the line items (`orderedQuantity × unitPrice`, summed).

Every PO carries a `status`, which the system moves through automatically as GRNs are recorded against it — you never set it directly except to cancel:

```
PENDING  --(some items received)-->  PARTIALLY_RECEIVED  --(all items fully received)-->  RECEIVED
   |                                          |
   \------------------(cancel)---------------->  CANCELLED
```

### Goods Received Notes (GRN)

| Method | Path | Description |
|---|---|---|
| POST | `/api/grns` | Record goods received against a PO into a chosen Location — increases Inventory there |
| GET | `/api/grns` | List all GRNs |
| GET | `/api/grns/{id}` | Get a GRN by id |
| PUT | `/api/grns/{id}` | Update a GRN's descriptive fields only (`receivedDate`, `receivedBy`, `status`) — the PO, location, and quantities are permanent once recorded |
| DELETE | `/api/grns/{id}` | Delete a GRN — reverses the Inventory and PO-received-quantity effects it caused |

A GRN body looks like:
```json
{
  "grnNumber": "GRN-2001",
  "purchaseOrder": { "id": 4 },
  "location": { "id": 1 },
  "receivedDate": "2026-08-05",
  "receivedBy": { "id": 3 },
  "items": [
    { "purchaseOrderItem": { "id": 12 }, "receivedQuantity": 30 }
  ]
}
```

Rules enforced by `GoodsReceivedNoteServiceImpl`:
- Can't receive against a `CANCELLED` or already fully `RECEIVED` purchase order.
- Can't receive more of an item than is still outstanding on the PO (`OverReceiptException` if you try).
- Each accepted item immediately adds to `Inventory` for that product **at the GRN's location** — this is the actual "goods entering the warehouse" stock update.
- After saving, the parent PO's status is recalculated (`PENDING` → `PARTIALLY_RECEIVED` → `RECEIVED`) based on how much of each line has now been received in total.
- Deleting a GRN reverses everything it did: subtracts the quantity back out of Inventory at that location, reduces the PO item's received-so-far count, and recalculates the PO's status again.

### Sales Receipts (Point of Sale)

| Method | Path | Description |
|---|---|---|
| POST | `/api/sales-receipts` | Ring up a sale (cashier, location, one or more items) — deducts Inventory at that location |
| GET | `/api/sales-receipts` | List all sales |
| GET | `/api/sales-receipts/{id}` | Get a sale by id |
| PUT | `/api/sales-receipts/{id}/void` | Void a sale — restores the Inventory it deducted |
| DELETE | `/api/sales-receipts/{id}` | Delete a sale — also restores Inventory if it wasn't already voided |

A sale body looks like:
```json
{
  "cashier": { "id": 1 },
  "location": { "id": 1 },
  "paymentMethod": "CASH",
  "discount": 50.00,
  "items": [
    { "product": { "id": 7 }, "quantity": 2, "unitPrice": 250.00 }
  ]
}
```

`receiptNumber` is auto-generated if omitted. `totalAmount` is computed as the sum of line subtotals minus `discount`. If there isn't enough stock at the chosen location for any line, the whole sale is rejected with 409 (`InsufficientStockException`) and nothing is deducted.

### Payments

| Method | Path | Description |
|---|---|---|
| POST | `/api/payments` | Record a payment against a sales receipt (invoice) — a receipt can be split across several payments |
| GET | `/api/payments` | List all payments |
| GET | `/api/payments/{id}` | Get a payment by id |
| GET | `/api/payments/receipt/{receiptId}` | List payments made against one receipt |
| DELETE | `/api/payments/{id}` | Delete a payment |

Once a receipt's payments sum to at least its `totalAmount`, its status automatically becomes `PAID`.

## 5. Project structure

```
src/main/java/com/bci/productcrud/
  controller/   -> @RestController - HTTP in, HTTP out, no business logic
  service/      -> business rules (e.g. "barcode must be unique", "not enough stock to sell")
  repository/   -> JpaRepository interfaces - DB access, no SQL written by hand
  model/        -> JPA-mapped domain entities: Product, Supplier, Location, Inventory, Role, User,
                    PurchaseOrder, PurchaseOrderItem, GoodsReceivedNote, GrnItem,
                    SalesReceipt, SalesReceiptItem, Payment
  exception/    -> centralized error handling (@RestControllerAdvice)
  config/       -> DataSeeder (seeds default roles/location/admin user on first run)
  util/         -> PasswordUtil (salted SHA-256 password hashing)
src/main/resources/
  application.properties -> config (DB connection, port, JPA settings)
  static/                -> the frontend: index.html + app.js + style.css
```

Request flow for a barcode scan: `app.js` → `GET /api/products/barcode/{code}` → `ProductController` → `ProductService` → `ProductRepository` → MySQL, and the `Product` JSON flows back the same path in reverse.

**Product ↔ Supplier link:** `Product` holds a `@ManyToOne` reference to `Supplier` (nullable — a product doesn't have to have a supplier). `ProductServiceImpl` resolves the supplier by id through `SupplierService` rather than trusting whatever partial supplier object the client sent. Deleting a supplier that's still referenced by any product is rejected (409 Conflict).

**Product ↔ Inventory ↔ Location:** stock is no longer a single number on `Product`. Instead, `Inventory` holds one row per `(product, location)` pair with a `quantityOnHand`. `ProductServiceImpl` populates a transient, read-only `totalStock` field on every `Product` it returns by summing `Inventory` across all locations, purely for display — it's never persisted on the `products` table.

**PO ↔ GRN ↔ Inventory chain:** `PurchaseOrder` holds a list of `PurchaseOrderItem` (one per product being ordered), each tracking both `orderedQuantity` and a running `receivedQuantity`, plus optional `createdBy`/`approvedBy` `User` references. `GoodsReceivedNote` now also holds a `Location` (where the goods were physically received) and a `User` (`receivedBy`, no longer free text). `GoodsReceivedNoteServiceImpl.create()` resolves the PO, its items, the location, and the user through the existing services (not from raw client input), checks the received amount doesn't exceed what's still outstanding, calls `InventoryService.adjustQuantity(...)` to add stock at that location, updates the PO item's received count, and recalculates the parent PO's overall status — all inside a single `@Transactional` method so a failure partway through rolls everything back together.

**Sale ↔ Inventory ↔ Payment chain:** `SalesReceiptServiceImpl.create()` resolves the cashier, location, and each line's product, then calls `InventoryService.adjustQuantity(...)` with a negative delta for each line — if that would take any product below zero at that location, the whole sale is rejected and nothing is committed. `PaymentServiceImpl` records payments against a `SalesReceipt` and flips its status to `PAID` once the sum of its payments meets or exceeds `totalAmount`.
