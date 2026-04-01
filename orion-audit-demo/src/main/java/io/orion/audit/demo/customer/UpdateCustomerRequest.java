package io.orion.audit.demo.customer;

public record UpdateCustomerRequest(String name, String email, String password) {
}
