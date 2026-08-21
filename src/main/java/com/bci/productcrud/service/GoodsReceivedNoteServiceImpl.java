package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateGrnNumberException;
import com.bci.productcrud.exception.GrnNotFoundException;
import com.bci.productcrud.exception.InvalidPurchaseOrderStatusException;
import com.bci.productcrud.exception.OverReceiptException;
import com.bci.productcrud.model.GoodsReceivedNote;
import com.bci.productcrud.model.GrnItem;
import com.bci.productcrud.model.Location;
import com.bci.productcrud.model.PurchaseOrder;
import com.bci.productcrud.model.PurchaseOrderItem;
import com.bci.productcrud.model.PurchaseOrderStatus;
import com.bci.productcrud.model.User;
import com.bci.productcrud.repository.GoodsReceivedNoteRepository;
import com.bci.productcrud.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class GoodsReceivedNoteServiceImpl implements GoodsReceivedNoteService {

    private final GoodsReceivedNoteRepository goodsReceivedNoteRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final InventoryService inventoryService;
    private final LocationService locationService;
    private final UserService userService;

    public GoodsReceivedNoteServiceImpl(GoodsReceivedNoteRepository goodsReceivedNoteRepository,
                                         PurchaseOrderRepository purchaseOrderRepository,
                                         PurchaseOrderService purchaseOrderService,
                                         InventoryService inventoryService,
                                         LocationService locationService,
                                         UserService userService) {
        this.goodsReceivedNoteRepository = goodsReceivedNoteRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderService = purchaseOrderService;
        this.inventoryService = inventoryService;
        this.locationService = locationService;
        this.userService = userService;
    }

    @Override
    public GoodsReceivedNote create(GoodsReceivedNote request) {
        if (goodsReceivedNoteRepository.existsByGrnNumber(request.getGrnNumber())) {
            throw new DuplicateGrnNumberException("A GRN with number " + request.getGrnNumber() + " already exists");
        }
        if (request.getPurchaseOrder() == null || request.getPurchaseOrder().getId() == null) {
            throw new IllegalArgumentException("A purchase order must be selected for this GRN");
        }
        if (request.getLocation() == null || request.getLocation().getId() == null) {
            throw new IllegalArgumentException("A location must be selected to receive this GRN into");
        }

        PurchaseOrder po = purchaseOrderService.findById(request.getPurchaseOrder().getId());

        if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new InvalidPurchaseOrderStatusException("Cannot receive goods against a cancelled purchase order (" + po.getPoNumber() + ").");
        }
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new InvalidPurchaseOrderStatusException("Purchase order " + po.getPoNumber() + " has already been fully received.");
        }

        Location location = locationService.findById(request.getLocation().getId());

        GoodsReceivedNote grn = new GoodsReceivedNote();
        grn.setGrnNumber(request.getGrnNumber());
        grn.setPurchaseOrder(po);
        grn.setLocation(location);
        grn.setReceivedDate(request.getReceivedDate());
        grn.setReceivedBy(resolveUser(request.getReceivedBy()));
        grn.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "RECEIVED" : request.getStatus());

        for (GrnItem itemRequest : request.getItems()) {
            if (itemRequest.getPurchaseOrderItem() == null || itemRequest.getPurchaseOrderItem().getId() == null) {
                throw new IllegalArgumentException("Each GRN line must reference a purchase order item");
            }

            PurchaseOrderItem poItem = po.getItems().stream()
                    .filter(i -> i.getId().equals(itemRequest.getPurchaseOrderItem().getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Purchase order item " + itemRequest.getPurchaseOrderItem().getId() + " does not belong to purchase order " + po.getPoNumber()));

            int newlyReceived = itemRequest.getReceivedQuantity();
            int alreadyReceived = poItem.getReceivedQuantity();
            int ordered = poItem.getOrderedQuantity();

            if (alreadyReceived + newlyReceived > ordered) {
                throw new OverReceiptException(
                        "Cannot receive " + newlyReceived + " of \"" + poItem.getProduct().getName() +
                                "\" - only " + (ordered - alreadyReceived) + " remaining on purchase order " + po.getPoNumber() + ".");
            }

            // Update the PO line's received-so-far count.
            poItem.setReceivedQuantity(alreadyReceived + newlyReceived);

            // The actual stock effect: goods physically arrived at this location, so add to Inventory.
            inventoryService.adjustQuantity(poItem.getProduct().getId(), location.getId(), newlyReceived);

            GrnItem grnItem = new GrnItem();
            grnItem.setPurchaseOrderItem(poItem);
            grnItem.setReceivedQuantity(newlyReceived);
            grnItem.setGrn(grn);
            grn.getItems().add(grnItem);
        }

        recomputePoStatus(po);
        purchaseOrderRepository.save(po);

        return goodsReceivedNoteRepository.save(grn);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceivedNote> findAll() {
        return goodsReceivedNoteRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceivedNote findById(Long id) {
        return goodsReceivedNoteRepository.findById(id)
                .orElseThrow(() -> new GrnNotFoundException("GRN not found with id " + id));
    }

    @Override
    public GoodsReceivedNote update(Long id, GoodsReceivedNote request) {
        // Quantities and location are intentionally not editable here - a GRN is a permanent receiving record.
        // Only the descriptive fields can be corrected after the fact.
        GoodsReceivedNote grn = findById(id);
        grn.setReceivedDate(request.getReceivedDate());
        grn.setReceivedBy(resolveUser(request.getReceivedBy()));
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            grn.setStatus(request.getStatus());
        }
        return goodsReceivedNoteRepository.save(grn);
    }

    @Override
    public void delete(Long id) {
        GoodsReceivedNote grn = findById(id);
        PurchaseOrder po = grn.getPurchaseOrder();

        // Reverse the stock and PO-received effects this GRN caused.
        for (GrnItem grnItem : grn.getItems()) {
            PurchaseOrderItem poItem = grnItem.getPurchaseOrderItem();
            poItem.setReceivedQuantity(poItem.getReceivedQuantity() - grnItem.getReceivedQuantity());

            inventoryService.adjustQuantity(poItem.getProduct().getId(), grn.getLocation().getId(), -grnItem.getReceivedQuantity());
        }

        recomputePoStatus(po);
        purchaseOrderRepository.save(po);

        goodsReceivedNoteRepository.delete(grn);
    }

    private User resolveUser(User requested) {
        if (requested == null || requested.getId() == null) {
            return null;
        }
        return userService.findById(requested.getId());
    }

    private void recomputePoStatus(PurchaseOrder po) {
        if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            return;
        }
        boolean anyReceived = po.getItems().stream().anyMatch(i -> i.getReceivedQuantity() > 0);
        boolean allFullyReceived = po.getItems().stream().allMatch(i -> i.getReceivedQuantity() >= i.getOrderedQuantity());

        if (allFullyReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
        } else if (anyReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        } else {
            po.setStatus(PurchaseOrderStatus.PENDING);
        }
    }
}
