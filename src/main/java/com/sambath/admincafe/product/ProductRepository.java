package com.sambath.admincafe.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countByCategory(String category);

    List<Product> findAllByLockedFalse();
}
