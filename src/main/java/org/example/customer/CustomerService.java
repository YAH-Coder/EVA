package org.example.customer;

import org.example.utils.SharedIDService;
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for managing customer-related operations.
 * This class provides functionalities to add, retrieve, update, and delete customers.
 * It uses a ConcurrentHashMap for thread-safe storage of customers and SharedIDService for generating unique IDs.
 * This class is implemented as a singleton.
 */
public class CustomerService implements CustomerServiceInterface {
    private final ConcurrentHashMap<Long, Customer> customers;
    private static CustomerService INSTANCE;

    private CustomerService() {
        this.customers = new ConcurrentHashMap<>();
    }

    /**
     * Returns the singleton instance of CustomerService.
     *
     * @return The singleton CustomerService instance.
     */
    public static CustomerService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CustomerService();
        }
        return INSTANCE;
    }


    @Override
    public Customer add(String username, String email, LocalDateTime birthday) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew();
        Customer customer = new Customer(id, username, email, birthday);
        customers.put(id, customer);
        StatisticsService.getInstance().recordIdAssigned("Customer", customer.getId());
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
    public void delete(long id) {
        Customer existingCustomer = customers.remove(id);
        if (existingCustomer == null) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        SharedIDService.getInstance().delete(id);
    }

    @Override
    public Customer[] getAll() {
        return customers.values().toArray(new Customer[0]);
    }

    @Override
    public void deleteAll() {
        customers.keySet().forEach(SharedIDService.getInstance()::delete);
        customers.clear();
    }
}