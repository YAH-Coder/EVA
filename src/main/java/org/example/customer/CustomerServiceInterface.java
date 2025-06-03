package org.example.customer;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerServiceInterface {
    Customer add(String username, String email, LocalDateTime birthday);

    Customer get(long id);

    void update(long id, String name, String email, LocalDateTime birthday);

    void delete(long id);

    List<Customer> getAll();

    void deleteAll();
}
