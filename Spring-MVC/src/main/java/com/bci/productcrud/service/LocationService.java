package com.bci.productcrud.service;

import com.bci.productcrud.model.Location;

import java.util.List;

public interface LocationService {
    Location create(Location location);
    List<Location> findAll();
    Location findById(Long id);
    Location update(Long id, Location location);
    void delete(Long id);
}
