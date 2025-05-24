package org.example.customer;

// import org.example.utils.IDService; // IDService might be an interface, let's see if it's used.
import org.example.utils.SharedIDService; // Added import

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class CustomerService implements CustomerServiceInterface {
    private final HashMap<Long, Customer> customers;
    // private final IDServiceParallel idService; // Removed
    private static CustomerService INSTANCE;

    private CustomerService() { // Removed throws InterruptedException
        this.customers = new HashMap<>();
        // this.idService = new IDServiceParallel(1000); // Removed
    }

    // As per clarification, getInstance might not need it if constructor is clean
    // but add methods will. Let's keep it on getInstance for now as per "Simplification for worker".
    public static CustomerService getInstance() throws InterruptedException {
        if (INSTANCE == null) {
            INSTANCE = new CustomerService();
        }
        return INSTANCE;
    }


    @Override
    public Customer add(String username, String email, LocalDateTime birthday) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew(); // Changed to SharedIDService
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
    public void delete(long id) {
        if (!customers.containsKey(id)) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        customers.remove(id);
        SharedIDService.getInstance().delete(id); // Changed to SharedIDService
    }

    @Override
    public Customer[] getAll() {
        return customers.values().toArray(new Customer[customers.size()]);
    }

    @Override
    public void deleteAll() {
        for (Long id : customers.keySet()) {
            SharedIDService.getInstance().delete(id); // Changed to SharedIDService
        }
        customers.clear();
    }
}