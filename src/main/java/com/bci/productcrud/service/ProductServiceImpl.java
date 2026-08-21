package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateBarcodeException;
import com.bci.productcrud.exception.ProductNotFoundException;
import com.bci.productcrud.model.Product;
import com.bci.productcrud.model.Supplier;
import com.bci.productcrud.repository.InventoryRepository;
import com.bci.productcrud.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SupplierService supplierService;
    private final InventoryRepository inventoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, SupplierService supplierService,
                               InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.supplierService = supplierService;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Product create(Product product) {
        if (productRepository.existsByBarcode(product.getBarcode())) {
            throw new DuplicateBarcodeException("A product with barcode " + product.getBarcode() + " already exists");
        }
        product.setId(null);
        product.setSupplier(resolveSupplier(product.getSupplier()));
        Product saved = productRepository.save(product);
        saved.setTotalStock(0);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::attachTotalStock);
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id " + id));
        attachTotalStock(product);
        return product;
    }

    @Override
    @Transactional(readOnly = true)
    public Product findByBarcode(String barcode) {
        Product product = productRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with barcode " + barcode));
        attachTotalStock(product);
        return product;
    }

    @Override
    public Product update(Long id, Product request) {
        Product product = findById(id);

        productRepository.findByBarcode(request.getBarcode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateBarcodeException("A product with barcode " + request.getBarcode() + " already exists");
                });

        product.setBarcode(request.getBarcode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setSize(request.getSize());
        product.setColor(request.getColor());
        product.setPurchasePrice(request.getPurchasePrice());
        product.setSellingPrice(request.getSellingPrice());
        product.setStatus(request.getStatus());
        product.setSupplier(resolveSupplier(request.getSupplier()));
        Product saved = productRepository.save(product);
        attachTotalStock(saved);
        return saved;
    }

    @Override
    public void delete(Long id) {
        Product product = findById(id);
        productRepository.delete(product);
    }

    private void attachTotalStock(Product product) {
        Integer sum = inventoryRepository.sumQuantityByProductId(product.getId());
        product.setTotalStock(sum == null ? 0 : sum);
    }

    /**
     * The client only sends {"id": X} for the supplier reference, so look up the
     * managed Supplier by id rather than persisting whatever partial object arrived
     * over JSON. Returns null if no supplier was selected.
     */
    private Supplier resolveSupplier(Supplier requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return supplierService.findById(requested.getId());
    }
}
