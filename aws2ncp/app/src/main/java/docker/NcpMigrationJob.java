package docker;

public class NcpMigrationJob {

    private String title;
    private NcpMigrationJobSource source;
    private NcpMigrationJobTarget target;

    NcpMigrationJob(String title, NcpMigrationJobSource source, NcpMigrationJobTarget target) {
        this.title = title;
        this.source = source;
        this.target = target;
    }

    public String getTitle(){
        return title;
    }
    public NcpMigrationJobSource getSource() { return source; }
    public NcpMigrationJobTarget getTarget() { return target; }
}
