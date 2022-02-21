package Report;

public enum ReportType {
    NEW("Automation New"),
    COVERAGE("Automation Coverage");

    private String reportType;

    ReportType(String reportType) {
        this.reportType = reportType;
    }

    public String toString() {
        return reportType;
    }
}