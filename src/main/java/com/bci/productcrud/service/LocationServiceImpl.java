package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateLocationNameException;
import com.bci.productcrud.exception.LocationNotFoundException;
import com.bci.productcrud.model.Location;
import com.bci.productcrud.repository.InventoryRepository;
import com.bci.productcrud.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;

    public LocationServiceImpl(LocationRepository locationRepository, InventoryRepository inventoryRepository) {
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public Location create(Location location) {
        if (locationRepository.existsByName(location.getName())) {
            throw new DuplicateLocationNameException("A location named " + location.getName() + " already exists");
        }
        location.setId(null);
        return locationRepository.save(location);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> findAll() {
        return locationRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Location findById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException("Location not found with id " + id));
    }

    @Override
    public Location update(Long id, Location request) {
        Location location = findById(id);
        locationRepository.findByName(request.getName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateLocationNameException("A location named " + request.getName() + " already exists");
                });
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        return locationRepository.save(location);
    }

    @Override
    public void delete(Long id) {
        Location location = findById(id);
        if (!inventoryRepository.findByLocationId(id).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete location " + location.getName() + " - it still has inventory recorded against it");
        }
        locationRepository.delete(location);
    }
}
