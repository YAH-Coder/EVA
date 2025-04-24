package org.example;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class CustomerService {
    private final HashMap<Long, Customer> customers;
    private final IDService idService;
    private static CustomerService INSTANCE;

    private CustomerService() {
        this.customers = new HashMap<>();
        this.idService = new IDService();
    }

    public static CustomerService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomerService();
        }
        return INSTANCE;
    }


    public Customer add(String username, String email, LocalDateTime birthday) {
        long id = idService.getNew();
        Customer customer = new Customer(id, username, email, birthday);
        customers.put(id, customer);
        return customer;
    }

    public Customer get(long id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        return customer;
    }

    public void update(long id, String name, String email, LocalDateTime birthday) {
        Customer customer = get(id);
        customer.setUsername(name);
        customer.setEmail(email);
        customer.setBirthday(birthday);
    }

    public void delete(long id) {
        if (!customers.containsKey(id)) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        customers.remove(id);
        idService.delete(id);
    }



    public Customer[] getAllCustomers() {
        return customers.values().toArray(new Customer[customers.size()]);
    }

    public void deleteAll() {
        for (Long id : customers.keySet()) {
            idService.delete(id);
        }
        customers.clear();
    }

    public void printAll() {
        if (customers.isEmpty()) {
            System.out.println("No customers available.");
            return;
        }
        for (Customer customer : customers.values()) {
            System.out.println(customer);
            System.out.println("===============");
        }
    }
}