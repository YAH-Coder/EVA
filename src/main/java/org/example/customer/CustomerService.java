package org.example.customer;

import org.example.utils.IDService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

public class CustomerService implements CustomerServiceInterface {
    private final ConcurrentHashMap<Long, Customer> customers;
    private final IDService idService;
    private static CustomerService INSTANCE;

    private CustomerService() {
        this.customers = new ConcurrentHashMap<>();
        this.idService = new IDService();
    }

    public static CustomerService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomerService();
        }
        return INSTANCE;
    }


    @Override
    public Customer add(String username, String email, LocalDateTime birthday) {
        long id = idService.getNew();
        Customer customer = new Customer(id, username, email, birthday);
        customers.put(id, customer);
        return customer;
    }

    @Override
    public Customer get(long id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        return customer;
    }

    @Override
    public void update(long id, String name, String email, LocalDateTime birthday) {
        Customer customer = get(id);
        customer.setUsername(name);
        customer.setEmail(email);
        customer.setBirthday(birthday);
    }

    @Override
    public synchronized void delete(long id) {
        if (!customers.containsKey(id)) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        customers.remove(id);
        idService.delete(id);
    }

    @Override
    public List<Customer> getAll() {
        return new ArrayList<>(customers.values());
    }

    @Override
    public void deleteAll() {
        for (Long id : customers.keySet()) {
            idService.delete(id);
        }
        customers.clear();
    }
}