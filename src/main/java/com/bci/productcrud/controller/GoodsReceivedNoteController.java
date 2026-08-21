package com.bci.productcrud.controller;

import com.bci.productcrud.model.GoodsReceivedNote;
import com.bci.productcrud.service.GoodsReceivedNoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grns")
public class GoodsReceivedNoteController {

    private final GoodsReceivedNoteService goodsReceivedNoteService;

    public GoodsReceivedNoteController(GoodsReceivedNoteService goodsReceivedNoteService) {
        this.goodsReceivedNoteService = goodsReceivedNoteService;
    }

    @PostMapping
    public ResponseEntity<GoodsReceivedNote> create(@Valid @RequestBody GoodsReceivedNote grn) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goodsReceivedNoteService.create(grn));
    }

    @GetMapping
    public List<GoodsReceivedNote> findAll() {
        return goodsReceivedNoteService.findAll();
    }

    @GetMapping("/{id}")
    public GoodsReceivedNote findById(@PathVariable Long id) {
        return goodsReceivedNoteService.findById(id);
    }

    @PutMapping("/{id}")
    public GoodsReceivedNote update(@PathVariable Long id, @Valid @RequestBody GoodsReceivedNote grn) {
        return goodsReceivedNoteService.update(id, grn);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goodsReceivedNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
