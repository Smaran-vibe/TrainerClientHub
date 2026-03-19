package com.trainerclienthub.service;

import com.trainerclienthub.DAO.TrainerDAO;
import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

public class TrainerService {
    // Encapsulates trainer authentication and profile access.

    private final TrainerDAO trainerDAO;

    public TrainerService() {
        this.trainerDAO = new TrainerDAO();
    }


    public Trainer authenticate(String identifier, String password) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException(
                    "Please enter your email address or phone number.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Please enter your password.");
        }

        String normalised = identifier.trim();

        Optional<Trainer> found = trainerDAO.findByEmailOrPhone(normalised);

        if (found.isEmpty()) {
            found = trainerDAO.findByEmailOrPhone(normalised.toLowerCase());
        }

        if (found.isEmpty()) {
            throw new IllegalArgumentException(
                    "Account does not exist. Check your email or phone number.");
        }

        Trainer trainer = found.get();

        if (!checkPassword(password, trainer.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "Incorrect password. Please try again.");
        }

        return trainer;
    }


    public Trainer register(String name, String email, String phone,
                            String plainPassword, String confirmPassword) {

        ValidationUtil.requireNonBlank(name, "Full name");
        ValidationUtil.requireValidEmail(email);
        ValidationUtil.requireValidNepalPhone(phone);
        ValidationUtil.requireValidPassword(plainPassword);
        ValidationUtil.requirePasswordsMatch(plainPassword, confirmPassword);

        if (trainerDAO.findByEmail(email.trim().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException(
                    "Email already registered. Please use a different email address.");
        }

        if (trainerDAO.findByEmailOrPhone(phone.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Phone number already registered. Please use a different number.");
        }

        Trainer trainer = new Trainer(name, email, phone, hashPassword(plainPassword));
        trainerDAO.insert(trainer);
        return trainer;
    }


    public Optional<Trainer> findById(int trainerId) {
        ValidationUtil.requirePositiveInt(trainerId, "Trainer ID");
        return trainerDAO.findById(trainerId);
    }

    public Optional<Trainer> findByEmail(String email) {
        ValidationUtil.requireValidEmail(email);
        return trainerDAO.findByEmail(email.trim().toLowerCase());
    }

    public List<Trainer> findAll() {
        return trainerDAO.findAll();
    }


    public void promoteToAdmin(int targetTrainerId, Trainer requestingAdmin) {
        requireAdminAccess(requestingAdmin, "promote accounts to admin");

        Trainer target = trainerDAO.findById(targetTrainerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Trainer not found with ID: " + targetTrainerId));

        if (target.isAdmin()) {
            throw new IllegalStateException(
                    "\"" + target.getName() + "\" is already an ADMIN.");
        }

        target.setRole(TrainerRole.ADMIN);
        trainerDAO.update(target);
    }

    public void demoteToTrainer(int targetTrainerId, Trainer requestingAdmin) {
        requireAdminAccess(requestingAdmin, "demote admin accounts");

        if (requestingAdmin.getTrainerId() == targetTrainerId) {
            throw new IllegalStateException(
                    "You cannot demote your own admin account. Ask another admin.");
        }

        Trainer target = trainerDAO.findById(targetTrainerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Trainer not found with ID: " + targetTrainerId));

        target.setRole(TrainerRole.TRAINER);
        trainerDAO.update(target);
    }

    public void updateProfile(Trainer trainer, Trainer requestingTrainer) {
        boolean isSelf  = trainer.getTrainerId() == requestingTrainer.getTrainerId();
        boolean isAdmin = requestingTrainer.isAdmin();

        if (!isSelf && !isAdmin) {
            throw new IllegalStateException(
                    "You can only edit your own profile unless you are an admin.");
        }

        ValidationUtil.requireNonBlank(trainer.getName(),  "Name");
        ValidationUtil.requireValidEmail(trainer.getEmail());
        ValidationUtil.requireValidNepalPhone(trainer.getPhone());

        Optional<Trainer> existing = trainerDAO.findByEmail(trainer.getEmail());
        if (existing.isPresent()
                && existing.get().getTrainerId() != trainer.getTrainerId()) {
            throw new IllegalArgumentException(
                    "Email already in use by another account: " + trainer.getEmail());
        }

        trainerDAO.update(trainer);
    }


    private void requireAdminAccess(Trainer trainer, String actionLabel) {
        if (trainer == null || !trainer.isAdmin()) {
            throw new IllegalStateException(
                    "Access denied — only admins can " + actionLabel + ".");
        }
    }

    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    private boolean checkPassword(String plain, String hash) {

        return BCrypt.checkpw(plain, hash);
    }
}
