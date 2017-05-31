package jenkins.plugins.github.api.mock;

/**
 * @author Stephen Connolly
 */
public class MockOrganization extends MockOwner<MockOrganization> {
    private String description;

    MockOrganization(MockGitHub app, String login) {
        super(app, login);
    }

    @Override
    public String getType() {
        return "Organization";
    }

    public String getDescription() {
        return description;
    }

    public MockOrganization withDescription(String description) {
        this.description = description;
        touch();
        return this;
    }

}
