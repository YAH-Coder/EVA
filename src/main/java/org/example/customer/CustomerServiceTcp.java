package org.example.customer;

import org.example.client.TcpClient;

import java.time.LocalDateTime;
import java.util.List;

public class CustomerServiceTcp implements CustomerServiceInterface {
    private TcpClient client;

    public CustomerServiceTcp(TcpClient client) {
        this.client = client;
    }

    @Override
    public Customer add(String username, String email, LocalDateTime birthday) {
        client.connect();
        client.sendMessage(String.join(";", "cc", username, email, birthday.toString()));
        Customer customer = (Customer) client.receiveObject();
        client.disconnect();
        return customer;
    }

    @Override
    public Customer get(long id) {
        return null;
    }

    @Override
    public void update(long id, String name, String email, LocalDateTime birthday) {

    }

    @Override
    public void delete(long id) {

    }

    @Override
    public List<Customer> getAll() {
        client.connect();
        client.sendMessage("gac");
        List<Customer> customers = (List<Customer>) client.receiveObject();
        client.disconnect();
        return customers;
    }

    @Override
    public void deleteAll() {

    }
}
