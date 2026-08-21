package com.bci.productcrud.service;

import com.bci.productcrud.exception.DuplicateLocationNameException;
import com.bci.productcrud.exception.LocationInUseException;
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
        if (locationRepository.existsByLocationName(location.getLocationName())) {
            throw new DuplicateLocationNameException("A location named " + location.getLocationName() + " already exists");
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

        locationRepository.findByLocationName(request.getLocationName())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateLocationNameException("A location named " + request.getLocationName() + " already exists");
                });

        location.setLocationName(request.getLocationName());
        location.setAddress(request.getAddress());
        return locationRepository.save(location);
    }

    @Override
    public void delete(Long id) {
        Location location = findById(id);
        if (inventoryRepository.existsByLocationId(id)) {
            throw new LocationInUseException(
                    "Cannot delete location " + location.getLocationName() + " - it still has inventory records linked to it.");
        }
        locationRepository.delete(location);
    }
}
