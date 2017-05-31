package jenkins.plugins.github.api.mock;

import java.util.Set;

public class MockRepository extends MockObject {
    private final MockOwner<?> owner;
    private final String name;
    private String description;
    private boolean _private;
    private boolean fork;
    private String homepage;
    private String language;
    private Set<String> topics;

    public MockRepository(MockGitHub app, MockOwner<?> owner, String name) {
        super(app);
        this.owner = owner;
        this.name = name;
    }

    public MockOwner<?> owner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public MockRepository withDescription(String description) {
        this.description = description;
        touch();
        return this;
    }

    public boolean isPrivate() {
        return _private;
    }

    public MockRepository withPrivate(boolean _private) {
        this._private = _private;
        touch();
        return this;
    }

    public boolean isFork() {
        return fork;
    }

    public MockRepository withFork(boolean fork) {
        this.fork = fork;
        touch();
        return this;
    }

    public String getHomepage() {
        return homepage;
    }

    public MockRepository withHomepage(String homepage) {
        this.homepage = homepage;
        touch();
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public MockRepository withLanguage(String language) {
        this.language = language;
        touch();
        return this;
    }

    public Set<String> getTopics() {
        return topics;
    }

    public MockRepository withTopics(Set<String> topics) {
        this.topics = topics;
        touch();
        return this;
    }
}
