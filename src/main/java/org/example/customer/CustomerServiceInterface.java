package org.example.customer;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Interface defining the contract for customer management services.
 * Implementing classes should provide functionalities to add, retrieve,
 * update, and delete customers, as well as retrieve all customers.
 */
public interface CustomerServiceInterface {
    /**
     * Adds a new customer to the system.
     *
     * @param username The username of the customer.
     * @param email The email address of the customer.
     * @param birthday The birthday of the customer.
     * @return The newly created {@link Customer} object.
     * @throws InterruptedException If the ID generation process is interrupted.
     * @throws IllegalArgumentException If any of the provided customer details are invalid (e.g., blank username, invalid email format, underage).
     */
    Customer add(String username, String email, LocalDateTime birthday) throws InterruptedException;

    /**
     * Retrieves a customer by their unique ID.
     *
     * @param id The ID of the customer to retrieve.
     * @return The {@link Customer} object associated with the given ID.
     * @throws NoSuchElementException If no customer is found with the specified ID.
     */
    Customer get(long id);

    /**
     * Updates the details of an existing customer.
     *
     * @param id The ID of the customer to update.
     * @param name The new username for the customer.
     * @param email The new email address for the customer.
     * @param birthday The new birthday for the customer.
     * @throws NoSuchElementException If no customer is found with the specified ID.
     * @throws IllegalArgumentException If any of the provided customer details for update are invalid.
     */
    void update(long id, String name, String email, LocalDateTime birthday);

    /**
     * Deletes a customer from the system by their ID.
     *
     * @param id The ID of the customer to delete.
     * @throws NoSuchElementException If no customer is found with the specified ID.
     */
    void delete(long id);

    /**
     * Retrieves all customers currently in the system.
     *
     * @return An array of {@link Customer} objects. The array will be empty if there are no customers.
     */
    Customer[] getAll();

    /**
     * Deletes all customers from the system.
     * This operation will also release their associated IDs.
     */
    void deleteAll();
}
