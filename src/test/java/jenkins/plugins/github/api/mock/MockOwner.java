package jenkins.plugins.github.api.mock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class MockOwner<T> extends MockObject {
    private final Map<String, MockRepository> repositories = new HashMap<>();
    private final String login;
    private String name;
    private String avatarUrl;
    private String blog;
    private String location;
    private String email;
    private Set<String> following = new HashSet<>();

    MockOwner(MockGitHub app, String login) {
        super(app);
        this.login = login;
    }

    public Map<String, MockRepository> repositories() {
        return repositories;
    }

    public T withPublicRepo(String name) {
        return withRepo(name, false);
    }

    public T withPrivateRepo(String name) {
        return withRepo(name, true);
    }

    public T withRepo(String name, boolean isPrivate) {
        MockRepository repo = new MockRepository(app(), this, name).withPrivate(isPrivate);
        repositories.put(name, repo);
        return (T) this;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public T withName(String name) {
        this.name = name;
        touch();
        return (T) this;
    }

    public abstract String getType();

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public T withAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        touch();
        return (T) this;
    }

    public String getBlog() {
        return blog;
    }

    public T withBlog(String blog) {
        this.blog = blog;
        touch();
        return (T) this;
    }

    public String getLocation() {
        return location;
    }

    public T withLocation(String location) {
        this.location = location;
        touch();
        return (T) this;
    }

    public String getEmail() {
        return email;
    }

    public T withEmail(String email) {
        this.email = email;
        touch();
        return (T) this;
    }

    public int getPublicRepos() {
        int result = 0;
        for (MockRepository r : repositories.values()) {
            if (!r.isPrivate()) {
                result++;
            }
        }
        return result;
    }

    public int getFollowers() {
        int count = 0;
        for (MockOwner<?> owner : app().owners()) {
            if (owner.following.contains(login)) {
                count++;
            }
        }
        return 0;
    }

    public int getFollowing() {
        return following.size();
    }

    public T withFollowing(Set<String> following) {
        this.following = following;
        touch();
        return (T) this;
    }
}
