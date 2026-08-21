package com.bci.productcrud.service;

import com.bci.productcrud.model.GoodsReceivedNote;

import java.util.List;

public interface GoodsReceivedNoteService {
    GoodsReceivedNote create(GoodsReceivedNote grn);
    List<GoodsReceivedNote> findAll();
    GoodsReceivedNote findById(Long id);
    GoodsReceivedNote update(Long id, GoodsReceivedNote grn);
    void delete(Long id);
}
