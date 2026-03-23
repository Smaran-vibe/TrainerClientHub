package com.trainerclienthub.model;

import java.time.LocalDateTime;

public class Report {

    //  Fields

    private int reportId;
    private int generatedBy;
    private ReportType reportType;
    private LocalDateTime generatedDate;
    private String content;

    // Constructors

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Report() {}

    /**
     * Constructor used when generating a new report.
     */
    public Report(int generatedBy, ReportType reportType, String content) {
        setGeneratedBy(generatedBy);
        setReportType(reportType);
        setContent(content);
        this.generatedDate = LocalDateTime.now();
    }

    public Report(int reportId, int generatedBy, ReportType reportType,
                  LocalDateTime generatedDate, String content) {
        this.reportId = reportId;
        setGeneratedBy(generatedBy);
        setReportType(reportType);
        setGeneratedDate(generatedDate);
        setContent(content);
    }

    // Getters & Setters

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(int generatedBy) {
        if (generatedBy <= 0) {
            throw new IllegalArgumentException("GeneratedBy must reference a valid trainer ID (> 0).");
        }
        this.generatedBy = generatedBy;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        if (reportType == null) {
            throw new IllegalArgumentException("Report type must not be null.");
        }
        this.reportType = reportType;
    }

    public LocalDateTime getGeneratedDate() {
        return generatedDate;
    }

    public void setGeneratedDate(LocalDateTime generatedDate) {
        if (generatedDate == null) {
            throw new IllegalArgumentException("Generated date must not be null.");
        }
        this.generatedDate = generatedDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Report content must not be blank.");
        }
        this.content = content;
    }

    // Object overrides

    @Override
    public String toString() {
        return "Report{" +
                "reportId=" + reportId +
                ", generatedBy=" + generatedBy +
                ", reportType=" + reportType +
                ", generatedDate=" + generatedDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        Report other = (Report) o;
        return reportId == other.reportId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(reportId);
    }
}
