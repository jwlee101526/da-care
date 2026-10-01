package com.dacare.server.service;

import com.dacare.server.domain.Engineer;
import com.dacare.server.repository.EngineerRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EngineerService {

  private final EngineerRepository engineers;

  public EngineerService(EngineerRepository engineers) {
    this.engineers = engineers;
  }

  public List<Engineer> all() {
    return engineers.findAll();
  }

  @Transactional
  public Engineer create(String name, String phone, String specialty, String region) {
    return engineers.save(new Engineer(name, phone, specialty, region));
  }

  @Transactional
  public Engineer update(Long id, String name, String phone, String specialty, String region) {
    Engineer engineer = engineers.findById(id)
        .orElseThrow(() -> new NoSuchElementException("기사를 찾을 수 없습니다."));
    engineer.update(name, phone, specialty, region);
    return engineer;
  }

  @Transactional
  public void delete(Long id) {
    engineers.deleteById(id);
  }
}
