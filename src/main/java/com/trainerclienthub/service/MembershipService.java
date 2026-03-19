package com.trainerclienthub.service;

import com.trainerclienthub.DAO.MembershipDAO;
import com.trainerclienthub.model.Membership;
import com.trainerclienthub.model.MembershipPlan;
import com.trainerclienthub.model.MembershipStatus;
import com.trainerclienthub.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MembershipService {
    // Enforces membership business rules and DAO operations.

    private final MembershipDAO membershipDAO;

    public MembershipService() {
        this.membershipDAO = new MembershipDAO();
    }


    public MembershipPlan createPlan(MembershipPlan plan) {
        ValidationUtil.requireNonBlank(plan.getPlanName(), "Plan name");
        if (plan.getPlanType() == null) {
            throw new IllegalArgumentException("Plan type must not be null.");
        }
        ValidationUtil.requirePositiveInt(plan.getDurationDays(), "Plan duration (days)");
        if (plan.getPrice() == null || plan.getPrice().signum() < 0) {
            throw new IllegalArgumentException("Plan price must be zero or greater.");
        }
        membershipDAO.insertPlan(plan);
        return plan;
    }

    public List<MembershipPlan> findAllPlans() {
        return membershipDAO.findAllPlans();
    }

    public Optional<MembershipPlan> findPlanById(int planId) {
        ValidationUtil.requirePositiveInt(planId, "Plan ID");
        return membershipDAO.findPlanById(planId);
    }

    public void updatePlan(MembershipPlan plan) {
        ValidationUtil.requirePositiveInt(plan.getPlanId(), "Plan ID");
        ValidationUtil.requireNonBlank(plan.getPlanName(), "Plan name");
        ValidationUtil.requirePositiveInt(plan.getDurationDays(), "Plan duration (days)");
        membershipDAO.updatePlan(plan);
    }

    public void deletePlan(int planId) {
        ValidationUtil.requirePositiveInt(planId, "Plan ID");
        membershipDAO.deletePlan(planId);
    }



    public Membership assignMembership(int clientId, int planId,
                                       LocalDate startDate, LocalDate endDate) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        ValidationUtil.requirePositiveInt(planId,   "Plan ID");
        ValidationUtil.requireEndAfterStart(startDate, endDate);

        Optional<Membership> active = membershipDAO.findActiveByClient(clientId);
        if (active.isPresent()) {
            throw new IllegalStateException(
                    "Client already has an ACTIVE membership (ID: "
                    + active.get().getMembershipId()
                    + "). Cancel it before assigning a new one.");
        }

        Membership membership = new Membership(clientId, planId, startDate, endDate);
        membershipDAO.insert(membership);
        return membership;
    }


    public Optional<Membership> findById(int membershipId) {
        ValidationUtil.requirePositiveInt(membershipId, "Membership ID");
        return membershipDAO.findById(membershipId);
    }

    public List<Membership> findByClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        return membershipDAO.findByClient(clientId);
    }

    public Optional<Membership> findActiveByClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        return membershipDAO.findActiveByClient(clientId);
    }

    public List<Membership> findAll() {
        return membershipDAO.findAll();
    }


    public void renewMembership(int membershipId, LocalDate newEndDate) {
        ValidationUtil.requirePositiveInt(membershipId, "Membership ID");
        if (newEndDate == null) {
            throw new IllegalArgumentException("New end date must not be null.");
        }

        Membership membership = membershipDAO.findById(membershipId)
                .orElseThrow(() -> new IllegalStateException(
                        "Membership not found with ID: " + membershipId));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE memberships can be renewed. "
                    + "Current status: " + membership.getStatus());
        }

        ValidationUtil.requireEndAfterStart(membership.getEndDate(), newEndDate);

        membership.setEndDate(newEndDate);
        membershipDAO.update(membership);
    }

    public void cancelMembership(int membershipId) {
        ValidationUtil.requirePositiveInt(membershipId, "Membership ID");

        Membership membership = membershipDAO.findById(membershipId)
                .orElseThrow(() -> new IllegalStateException(
                        "Membership not found with ID: " + membershipId));

        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only ACTIVE memberships can be cancelled. "
                    + "Current status: " + membership.getStatus());
        }

        membershipDAO.updateStatus(membershipId, MembershipStatus.CANCELLED);
    }


    public void delete(int membershipId) {
        ValidationUtil.requirePositiveInt(membershipId, "Membership ID");
        membershipDAO.delete(membershipId);
    }
}
