package com.bci.productcrud.service;

import com.bci.productcrud.model.Supplier;

import java.util.List;

public interface SupplierService {

    Supplier create(Supplier supplier);

    List<Supplier> findAll();

    Supplier findById(Long id);

    Supplier update(Long id, Supplier supplier);

    void delete(Long id);
}
