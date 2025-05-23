package com.inn.cloud.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inn.cloud.entity.Cloud;

@Repository
public interface CloudRepository extends JpaRepository<Cloud, Integer> {

}
