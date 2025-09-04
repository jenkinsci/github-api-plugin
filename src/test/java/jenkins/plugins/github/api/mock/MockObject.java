package jenkins.plugins.github.api.mock;

/**
 * @author Stephen Connolly
 */
public class MockObject {
    private final MockGitHub app;

    private final long id;
    private final long created;
    private long updated;

    public MockObject(MockGitHub app) {
        this.app = app;
        this.id = app.nextId();
        this.created = System.currentTimeMillis();
        this.updated = System.currentTimeMillis();
    }

    public MockGitHub app() {
        return app;
    }

    public long getId() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public long getUpdated() {
        return updated;
    }

    public void touch() {
        updated = System.currentTimeMillis();
    }
}
