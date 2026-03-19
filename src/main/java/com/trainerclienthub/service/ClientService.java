package com.trainerclienthub.service;

import com.trainerclienthub.DAO.ClientDAO;
import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Gender;
import com.trainerclienthub.util.ValidationUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ClientService {
    // Handles client validation and DAO coordination.

    private final ClientDAO clientDAO;

    public ClientService() {
        this.clientDAO = new ClientDAO();
    }


    public Client addClient(String name, int age, Gender gender, String phone,
                            String email, int sessionBalance,
                            BigDecimal weightKg, int trainerId) {

        ValidationUtil.requireNonBlank(name, "Client name");
        ValidationUtil.requireValidAge(age);
        if (gender == null) {
            throw new IllegalArgumentException("Gender must not be null.");
        }
        ValidationUtil.requireValidNepalPhone(phone);
        ValidationUtil.requireValidEmail(email);
        ValidationUtil.requireNonNegativeInt(sessionBalance, "Session balance");
        ValidationUtil.requireValidClientWeight(weightKg);
        ValidationUtil.requirePositiveInt(trainerId, "Trainer ID");

        if (clientDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException(
                    "A client with this email already exists: " + email);
        }

        Client client = new Client(name, age, gender, phone, email,
                                   sessionBalance, weightKg, trainerId);
        clientDAO.insert(client);
        return client;
    }


    public Optional<Client> findById(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        return clientDAO.findById(clientId);
    }

    public Optional<Client> findByEmail(String email) {
        ValidationUtil.requireValidEmail(email);
        return clientDAO.findByEmail(email);
    }

    public List<Client> findAll() {
        return clientDAO.findAll();
    }

    public List<Client> findByTrainer(int trainerId) {
        ValidationUtil.requirePositiveInt(trainerId, "Trainer ID");
        return clientDAO.findByTrainer(trainerId);
    }

    public List<Client> search(String keyword) {
        ValidationUtil.requireNonBlank(keyword, "Search keyword");
        return clientDAO.search(keyword.trim());
    }

    public void updateClient(Client client) {
        ValidationUtil.requirePositiveInt(client.getClientId(), "Client ID");
        ValidationUtil.requireNonBlank(client.getName(), "Client name");
        ValidationUtil.requireValidAge(client.getAge());
        ValidationUtil.requireValidNepalPhone(client.getPhone());
        ValidationUtil.requireValidEmail(client.getEmail());
        ValidationUtil.requireNonNegativeInt(client.getSessionBalance(), "Session balance");
        ValidationUtil.requireValidClientWeight(client.getWeightKg());


        Optional<Client> existing = clientDAO.findByEmail(client.getEmail());
        if (existing.isPresent() && existing.get().getClientId() != client.getClientId()) {
            throw new IllegalArgumentException(
                    "Another client is already registered with this email: " + client.getEmail());
        }

        clientDAO.update(client);
    }

    public void updateSessionBalance(int clientId, int newBalance) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        ValidationUtil.requireNonNegativeInt(newBalance, "Session balance");
        clientDAO.updateSessionBalance(clientId, newBalance);
    }


    public void deleteClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        if (clientDAO.findById(clientId).isEmpty()) {
            throw new IllegalStateException("No client found with ID: " + clientId);
        }
        clientDAO.delete(clientId);
    }
}
