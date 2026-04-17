package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Membership;
import com.trainerclienthub.model.MembershipPlan;
import com.trainerclienthub.model.MembershipStatus;
import com.trainerclienthub.model.PlanType;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.util.SessionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MembershipDAO {


    private static final String PLAN_INSERT =
            "INSERT INTO membership_plan (plan_name, plan_type, duration_days, price, sessions_included) VALUES (?, ?, ?, ?, ?)";
    private static final String PLAN_SELECT_BY_ID = "SELECT * FROM membership_plan WHERE plan_id = ?";
    private static final String PLAN_SELECT_ALL = "SELECT * FROM membership_plan ORDER BY plan_name";
    private static final String PLAN_UPDATE =
            "UPDATE membership_plan SET plan_name = ?, plan_type = ?, duration_days = ?, price = ?, sessions_included = ? WHERE plan_id = ?";
    private static final String PLAN_DELETE = "DELETE FROM membership_plan WHERE plan_id = ?";


    private static final String INSERT =
            "INSERT INTO membership (client_id, plan_id, start_date, end_date, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT * FROM membership WHERE membership_id = ?";
    private static final String SELECT_BY_CLIENT = "SELECT * FROM membership WHERE client_id = ? ORDER BY created_at DESC";
    private static final String SELECT_ACTIVE_BY_CLIENT =
            "SELECT * FROM membership WHERE client_id = ? AND status = 'ACTIVE' LIMIT 1";
    private static final String SELECT_ALL = "SELECT * FROM membership ORDER BY created_at DESC";
    private static final String SELECT_EXPIRING_BEFORE =
            "SELECT * FROM membership WHERE status = 'ACTIVE' AND end_date <= ?";

    private static final String SELECT_EXPIRING_BEFORE_BY_TRAINER =
            "SELECT m.* FROM membership m JOIN client c ON m.client_id = c.client_id " +
                    "WHERE m.status = 'ACTIVE' AND m.end_date <= ? AND c.trainer_id = ?";

    private static final String COUNT_ACTIVE = "SELECT COUNT(*) FROM membership WHERE status = 'ACTIVE'";

    private static final String COUNT_ACTIVE_BY_TRAINER =
            "SELECT COUNT(*) FROM membership m JOIN client c ON m.client_id = c.client_id " +
                    "WHERE m.status = 'ACTIVE' AND c.trainer_id = ?";
    private static final String UPDATE =
            "UPDATE membership SET plan_id = ?, start_date = ?, end_date = ?, status = ? WHERE membership_id = ?";
    private static final String UPDATE_STATUS = "UPDATE membership SET status = ? WHERE membership_id = ?";
    private static final String DELETE = "DELETE FROM membership WHERE membership_id = ?";


    public void insertPlan(MembershipPlan plan) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(PLAN_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, plan.getPlanName());
            ps.setString(2, plan.getPlanType().name());
            ps.setInt(3, plan.getDurationDays());
            ps.setBigDecimal(4, plan.getPrice());
            ps.setInt(5, plan.getSessionsIncluded());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) plan.setPlanId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert membership plan: " + plan.getPlanName(), e);
        }
    }

    public Optional<MembershipPlan> findPlanById(int planId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(PLAN_SELECT_BY_ID)) {

            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapPlan(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find membership plan id: " + planId, e);
        }
    }

    public List<MembershipPlan> findAllPlans() {
        List<MembershipPlan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(PLAN_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(mapPlan(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch membership plans.", e);
        }
        return list;
    }

    public void updatePlan(MembershipPlan plan) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(PLAN_UPDATE)) {

            ps.setString(1, plan.getPlanName());
            ps.setString(2, plan.getPlanType().name());
            ps.setInt(3, plan.getDurationDays());
            ps.setBigDecimal(4, plan.getPrice());
            ps.setInt(5, plan.getSessionsIncluded());
            ps.setInt(6, plan.getPlanId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update membership plan id: " + plan.getPlanId(), e);
        }
    }

    public void deletePlan(int planId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(PLAN_DELETE)) {

            ps.setInt(1, planId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete membership plan id: " + planId, e);
        }
    }


    public void insert(Membership membership) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, membership.getClientId());
            ps.setInt(2, membership.getPlanId());
            ps.setDate(3, Date.valueOf(membership.getStartDate()));
            ps.setDate(4, Date.valueOf(membership.getEndDate()));
            ps.setString(5, membership.getStatus().name());
            ps.setTimestamp(6, Timestamp.valueOf(
                    membership.getCreatedAt() != null ? membership.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) membership.setMembershipId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert membership for client id: " + membership.getClientId(), e);
        }
    }

    public Optional<Membership> findById(int membershipId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, membershipId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find membership id: " + membershipId, e);
        }
    }

    public List<Membership> findByClient(int clientId) {
        List<Membership> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CLIENT)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch memberships for client id: " + clientId, e);
        }
        return list;
    }


    public Optional<Membership> findActiveByClient(int clientId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_BY_CLIENT)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active membership for client id: " + clientId, e);
        }
    }

    public List<Membership> findAll() {
        var role = SessionManager.getInstance().getRole();
        if (role == TrainerRole.TRAINER) {
            var trainer = SessionManager.getInstance().getCurrentTrainer();
            return trainer != null ? findAllByTrainer(trainer.getTrainerId()) : new ArrayList<>();
        }
        List<Membership> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all memberships.", e);
        }
        return list;
    }

    private List<Membership> findAllByTrainer(int trainerId) {
        String sql = "SELECT m.* FROM membership m JOIN client c ON m.client_id = c.client_id " +
                "WHERE c.trainer_id = ? ORDER BY m.created_at DESC";
        List<Membership> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch memberships for trainer id: " + trainerId, e);
        }
        return list;
    }

    public int countActiveMemberships() {
        var role = SessionManager.getInstance().getRole();
        if (role == TrainerRole.TRAINER) {
            var trainer = SessionManager.getInstance().getCurrentTrainer();
            return trainer != null ? countActiveMembershipsByTrainer(trainer.getTrainerId()) : 0;
        }
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count active memberships.", e);
        }
    }

    private int countActiveMembershipsByTrainer(int trainerId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE_BY_TRAINER)) {
            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count active memberships for trainer id: " + trainerId, e);
        }
    }


    public List<Membership> findExpiringBefore(Date date) {
        var role = SessionManager.getInstance().getRole();
        if (role == TrainerRole.TRAINER) {
            var trainer = SessionManager.getInstance().getCurrentTrainer();
            if (trainer == null) return new ArrayList<>();
            List<Membership> list = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_EXPIRING_BEFORE_BY_TRAINER)) {
                ps.setDate(1, date);
                ps.setInt(2, trainer.getTrainerId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to fetch expiring memberships for trainer.", e);
            }
            return list;
        }
        List<Membership> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_EXPIRING_BEFORE)) {
            ps.setDate(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch expiring memberships.", e);
        }
        return list;
    }

    public void update(Membership membership) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setInt(1, membership.getPlanId());
            ps.setDate(2, Date.valueOf(membership.getStartDate()));
            ps.setDate(3, Date.valueOf(membership.getEndDate()));
            ps.setString(4, membership.getStatus().name());
            ps.setInt(5, membership.getMembershipId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update membership id: " + membership.getMembershipId(), e);
        }
    }


    public void updateStatus(int membershipId, MembershipStatus status) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {

            ps.setString(1, status.name());
            ps.setInt(2, membershipId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for membership id: " + membershipId, e);
        }
    }

    public void delete(int membershipId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, membershipId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete membership id: " + membershipId, e);
        }
    }


    private MembershipPlan mapPlan(ResultSet rs) throws SQLException {
        return new MembershipPlan(
                rs.getInt("plan_id"),
                rs.getString("plan_name"),
                PlanType.valueOf(rs.getString("plan_type").toUpperCase()),
                rs.getInt("duration_days"),
                rs.getBigDecimal("price"),
                rs.getInt("sessions_included")
        );
    }

    private Membership map(ResultSet rs) throws SQLException {
        return new Membership(
                rs.getInt("membership_id"),
                rs.getInt("client_id"),
                rs.getInt("plan_id"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                MembershipStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
