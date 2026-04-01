package io.orion.audit.demo.customer;

public record CreateCustomerRequest(String name, String email, String password) {
}
