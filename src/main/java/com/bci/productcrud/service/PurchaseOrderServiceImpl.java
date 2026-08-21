package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicatePoNumberException;
import com.bci.productcrud.exception.InvalidPurchaseOrderStatusException;
import com.bci.productcrud.exception.PurchaseOrderInUseException;
import com.bci.productcrud.exception.PurchaseOrderNotFoundException;
import com.bci.productcrud.model.Product;
import com.bci.productcrud.model.PurchaseOrder;
import com.bci.productcrud.model.PurchaseOrderItem;
import com.bci.productcrud.model.PurchaseOrderStatus;
import com.bci.productcrud.model.Supplier;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.GoodsReceivedNoteRepository;
import com.bci.productcrud.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceivedNoteRepository goodsReceivedNoteRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final UserService userService;

    public PurchaseOrderServiceImpl(PurchaseOrderRepository purchaseOrderRepository,
                                     GoodsReceivedNoteRepository goodsReceivedNoteRepository,
                                     SupplierService supplierService,
                                     ProductService productService,
                                     UserService userService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceivedNoteRepository = goodsReceivedNoteRepository;
        this.supplierService = supplierService;
        this.productService = productService;
        this.userService = userService;
    }

    @Override
    public PurchaseOrder create(PurchaseOrder request) {
        if (purchaseOrderRepository.existsByPoNumber(request.getPoNumber())) {
            throw new DuplicatePoNumberException("A purchase order with number " + request.getPoNumber() + " already exists");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(request.getPoNumber());
        po.setSupplier(resolveSupplier(request.getSupplier()));
        po.setOrderDate(request.getOrderDate());
        po.setNotes(request.getNotes());
        po.setStatus(PurchaseOrderStatus.PENDING);
        po.setCreatedBy(resolveUser(request.getCreatedBy()));
        po.setApprovedBy(resolveUser(request.getApprovedBy()));

        for (PurchaseOrderItem itemRequest : request.getItems()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(resolveProduct(itemRequest.getProduct()));
            item.setOrderedQuantity(itemRequest.getOrderedQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setReceivedQuantity(0);
            item.setPurchaseOrder(po);
            po.getItems().add(item);
        }

        return purchaseOrderRepository.save(po);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAll() {
        return purchaseOrderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found with id " + id));
    }

    @Override
    public PurchaseOrder update(Long id, PurchaseOrder request) {
        PurchaseOrder po = findById(id);

        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidPurchaseOrderStatusException("Cannot edit purchase order " + po.getPoNumber() + " - it is already fully received.");
        }

        // Cancelling is always allowed regardless of current (non-received) status.
        if (request.getStatus() == PurchaseOrderStatus.CANCELLED) {
            po.setStatus(PurchaseOrderStatus.CANCELLED);
            po.setNotes(request.getNotes());
            return purchaseOrderRepository.save(po);
        }

        // Approving is a lightweight status-free action: just record who approved it.
        if (request.getApprovedBy() != null && request.getApprovedBy().getId() != null
                && po.getApprovedBy() == null) {
            po.setApprovedBy(resolveUser(request.getApprovedBy()));
        }

        if (po.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new InvalidPurchaseOrderStatusException(
                    "Cannot change supplier or items for purchase order " + po.getPoNumber() +
                            " - receiving has already started against it. Only cancelling is allowed.");
        }

        purchaseOrderRepository.findByPoNumber(request.getPoNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicatePoNumberException("A purchase order with number " + request.getPoNumber() + " already exists");
                });

        po.setPoNumber(request.getPoNumber());
        po.setSupplier(resolveSupplier(request.getSupplier()));
        po.setOrderDate(request.getOrderDate());
        po.setNotes(request.getNotes());

        po.getItems().clear();
        for (PurchaseOrderItem itemRequest : request.getItems()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(resolveProduct(itemRequest.getProduct()));
            item.setOrderedQuantity(itemRequest.getOrderedQuantity());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setReceivedQuantity(0);
            item.setPurchaseOrder(po);
            po.getItems().add(item);
        }

        return purchaseOrderRepository.save(po);
    }

    @Override
    public void delete(Long id) {
        PurchaseOrder po = findById(id);
        if (!goodsReceivedNoteRepository.findByPurchaseOrderId(id).isEmpty()) {
            throw new PurchaseOrderInUseException(
                    "Cannot delete purchase order " + po.getPoNumber() +
                            " - one or more Goods Received Notes already reference it. Delete those GRNs first.");
        }
        purchaseOrderRepository.delete(po);
    }

    private Supplier resolveSupplier(Supplier requested) {
        if (requested == null || requested.getId() == null) {
            throw new IllegalArgumentException("A supplier must be selected for the purchase order");
        }
        return supplierService.findById(requested.getId());
    }

    private Product resolveProduct(Product requested) {
        if (requested == null || requested.getId() == null) {
            throw new IllegalArgumentException("A product must be selected for every purchase order line item");
        }
        return productService.findById(requested.getId());
    }

    private User resolveUser(User requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return userService.findById(requested.getId());
    }
}
