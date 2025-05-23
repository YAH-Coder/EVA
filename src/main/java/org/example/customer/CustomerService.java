package org.example.customer;

import org.example.utils.IDService;
import org.example.utils.IDServiceParallel;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.NoSuchElementException;

public class CustomerService implements CustomerServiceInterface {
    private final ConcurrentHashMap<Long, Customer> customers;
    private static final IDServiceParallel idService;
    private static final CustomerService INSTANCE;

    static {
        try {
            // Initialize idService first as CustomerService constructor might depend on it
            // if it were to use idService directly during its own construction,
            // though in this specific case, it's not strictly necessary as idService is used in methods.
            idService = IDServiceParallel.getInstance();
            INSTANCE = new CustomerService();
        } catch (RuntimeException e) { // Catching RuntimeException from IDServiceParallel.getInstance()
            throw new RuntimeException("Failed to initialize CustomerService", e);
        }
    }

    private CustomerService() { // No longer throws InterruptedException
        this.customers = new ConcurrentHashMap<>();
        // this.idService = new IDServiceParallel(1000); // Removed
    }

    public static CustomerService getInstance() {
        return INSTANCE;
    }


    @Override
    public synchronized Customer add(String username, String email, LocalDateTime birthday) throws InterruptedException {
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
    public synchronized void update(long id, String name, String email, LocalDateTime birthday) {
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
    public Customer[] getAll() {
        return customers.values().toArray(new Customer[customers.size()]);
    }

    @Override
    public synchronized void deleteAll() {
        for (Long id : customers.keySet()) {
            idService.delete(id);
        }
        customers.clear();
    }
}