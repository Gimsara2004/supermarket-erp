package com.bci.productcrud.repository;

import com.bci.productcrud.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductIdAndLocationId(Long productId, Long locationId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByLocationId(Long locationId);

    @Query("select coalesce(sum(i.quantityOnHand), 0) from Inventory i where i.product.id = :productId")
    Integer sumQuantityByProductId(@Param("productId") Long productId);

    boolean existsByLocationId(Long locationId);
}
