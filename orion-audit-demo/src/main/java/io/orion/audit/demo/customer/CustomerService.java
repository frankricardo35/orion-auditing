package io.orion.audit.demo.customer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer create(CreateCustomerRequest request) {
        return customerRepository.save(new Customer(request.name(), request.email(), request.password()));
    }

    @Transactional
    public Customer update(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        customer.setName(request.name());
        customer.setEmail(request.email());
        if (request.password() != null) {
            customer.setPassword(request.password());
        }
        return customer;
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        customerRepository.delete(customer);
    }
}
