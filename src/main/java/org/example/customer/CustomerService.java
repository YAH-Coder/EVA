package org.example.customer;

// import org.example.utils.IDService; // IDService might be an interface, let's see if it's used.
import org.example.utils.SharedIDService; // Added import
import org.example.utils.StatisticsService;

import java.time.LocalDateTime;
import java.util.HashMap; // Will be replaced by ConcurrentHashMap
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap; // Added import

/**
 * Service class for managing customer-related operations.
 * This class provides functionalities to add, retrieve, update, and delete customers.
 * It uses a ConcurrentHashMap for thread-safe storage of customers and SharedIDService for generating unique IDs.
 * This class is implemented as a singleton.
 */
public class CustomerService implements CustomerServiceInterface {
    private final ConcurrentHashMap<Long, Customer> customers; // Changed to ConcurrentHashMap
    // private final IDServiceParallel idService; // Removed
    private static CustomerService INSTANCE;

    private CustomerService() { // Removed throws InterruptedException
        this.customers = new ConcurrentHashMap<>(); // Changed to ConcurrentHashMap
        // this.idService = new IDServiceParallel(1000); // Removed
    }

    // As per clarification, getInstance might not need it if constructor is clean
    // but add methods will. Let's keep it on getInstance for now as per "Simplification for worker".
    /**
     * Returns the singleton instance of CustomerService.
     *
     * @return The singleton CustomerService instance.
     */
    public static CustomerService getInstance() { // Removed throws InterruptedException
        if (INSTANCE == null) {
            // SharedIDService.getInstance().awaitInitialGeneration(); // REMOVED
            INSTANCE = new CustomerService();
        }
        return INSTANCE;
    }


    @Override
    public Customer add(String username, String email, LocalDateTime birthday) throws InterruptedException {
        long id = SharedIDService.getInstance().getNew(); // Changed to SharedIDService
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
        Customer customer = get(id); // Ensures customer exists or throws NoSuchElementException
        customer.setUsername(name);
        customer.setEmail(email);
        customer.setBirthday(birthday);
    }

    @Override
    public void delete(long id) {
        Customer existingCustomer = customers.remove(id); // Atomically removes and returns the customer
        if (existingCustomer == null) {
            throw new NoSuchElementException("No customer found with ID " + id);
        }
        SharedIDService.getInstance().delete(id); // Changed to SharedIDService
    }

    @Override
    public Customer[] getAll() {
        return customers.values().toArray(new Customer[0]); // More robust way to get an empty array if needed
    }

    @Override
    public void deleteAll() {
        // It's generally safer to iterate over a copy of the keyset if modification occurs elsewhere,
        // but here, clear() is called at the end, making it fine.
        // However, for SharedIDService.delete, we need to ensure all IDs are processed before clear.
        customers.keySet().forEach(SharedIDService.getInstance()::delete); // Changed to SharedIDService
        customers.clear();
    }
}