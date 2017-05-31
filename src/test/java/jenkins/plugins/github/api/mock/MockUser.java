package jenkins.plugins.github.api.mock;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MockUser extends MockOwner<MockUser> {
    private Set<String> organizations = new HashSet<>();
    private boolean siteAdmin;
    private String company;
    private boolean hireable;
    private String bio;

    MockUser(MockGitHub app, String login) {
        super(app, login);
    }

    public boolean isSiteAdmin() {
        return siteAdmin;
    }

    public MockUser withSiteAdmin(boolean siteAdmin) {
        this.siteAdmin = siteAdmin;
        touch();
        return this;
    }

    public String getCompany() {
        return company;
    }

    public MockUser withCompany(String company) {
        this.company = company;
        touch();
        return this;
    }

    public boolean isHireable() {
        return hireable;
    }

    public MockUser withHireable(boolean hireable) {
        this.hireable = hireable;
        touch();
        return this;
    }

    public String getBio() {
        return bio;
    }

    public MockUser withBio(String bio) {
        this.bio = bio;
        touch();
        return this;
    }

    public String getType() {
        return "User";
    }

}
