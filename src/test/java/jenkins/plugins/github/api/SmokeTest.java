package jenkins.plugins.github.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import jenkins.plugins.github.api.mock.MockGitHub;
import jenkins.plugins.github.api.mock.MockOrganization;
import jenkins.plugins.github.api.mock.MockUser;
import okhttp3.OkHttpClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.connector.GitHubConnector;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class SmokeTest {

    @FunctionalInterface
    public interface IOFunction {
        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         * @throws IOException if I/O error occurs
         */
        GitHub apply(MockGitHub t) throws IOException;
    }

    static IOFunction[] connectFunctions() {
        OkHttpClient okHttpClient = new OkHttpClient();
        GitHubConnector okHttpGitHubConnector = new OkHttpGitHubConnector(okHttpClient);
        ArrayList<IOFunction> list = new ArrayList<>();
        list.add (mock -> GitHub.connectToEnterpriseAnonymously(mock.open()));
        list.add (mock -> new GitHubBuilder().withConnector(okHttpGitHubConnector).withEndpoint(mock.open()).build());

        return list.toArray(new IOFunction[] {});
    }

    public GitHub openAndConnect(IOFunction connectFunction, MockGitHub mock) throws IOException {
        return connectFunction.apply(mock);
    }

    @ParameterizedTest(name = "connectFunction={index}")
    @MethodSource("connectFunctions")
    void given__veryBasicMockGitHub__when__connectingAnonymously__then__apiUrlValid(IOFunction connectFunction) throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            openAndConnect(connectFunction, mock).checkApiUrlValidity();
        }
    }

    @ParameterizedTest(name = "connectFunction={index}")
    @MethodSource("connectFunctions")
    void given__veryBasicMockGitHub__when__listingRepos__then__reposListed(IOFunction connectFunction) throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            mock.withOrg("org1").withPublicRepo("repo1").withPrivateRepo("repo2");
            mock.withOrg("org2").withPublicRepo("repo3");
            mock.withUser("user1").withPublicRepo("repo4").withPrivateRepo("repo5");
            Set<String> names = new TreeSet<>();
            for (GHRepository r: openAndConnect(connectFunction, mock).listAllPublicRepositories()) {
                names.add(r.getFullName());
            }
            assertThat(names, contains("org1/repo1", "org2/repo3", "user1/repo4"));
        }
    }

    @ParameterizedTest(name = "connectFunction={index}")
    @MethodSource("connectFunctions")
    void given__veryBasicMockGitHub__when__listingManyRepos__then__reposListed(IOFunction connectFunction) throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            MockOrganization org1 = mock.withOrg("org1");
            Set<String> expected = new TreeSet<>();
            for (int i = 0; i < 95; i++) {
                org1.withPublicRepo("repo"+i);
                expected.add("org1/repo"+i);

            }
            Set<String> actual = new TreeSet<>();
            for (GHRepository r: openAndConnect(connectFunction, mock).listAllPublicRepositories()) {
                actual.add(r.getFullName());
            }
            assertThat(actual, is(expected));
        }
    }

    @ParameterizedTest(name = "connectFunction={index}")
    @MethodSource("connectFunctions")
    void given__veryBasicMockGitHub__when__gettingUser__then__userReturned(IOFunction connectFunction) throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            MockUser expected = mock.withUser("user1")
                    .withAvatarUrl("http://avatar.test/user1")
                    .withCompany("Testing Inc")
                    .withName("User One")
                    .withBlog("https://user1.test")
                    .withEmail("bob@test")
                    .withLocation("Unit test")
                    .withPrivateRepo("repo1")
                    .withPublicRepo("repo2")
                    .withPublicRepo("repo3");
            GHUser actual = openAndConnect(connectFunction, mock).getUser("user1");
            assertThat(actual.getLogin(), is(expected.getLogin()));
            assertThat(actual.getName(), is(expected.getName()));
            assertThat(actual.getAvatarUrl(), is(expected.getAvatarUrl()));
            assertThat(actual.getBlog(), is(expected.getBlog()));
            assertThat(actual.getCompany(), is(expected.getCompany()));
            assertThat(actual.getId(), is(expected.getId()));
            assertThat(actual.getPublicRepoCount(), is(expected.getPublicRepos()));
        }
    }

    @ParameterizedTest(name = "connectFunction={index}")
    @MethodSource("connectFunctions")
    void given__veryBasicMockGitHub__when__gettingOrg__then__orgReturned(IOFunction connectFunction) throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            MockOrganization expected = mock.withOrg("org1")
                    .withAvatarUrl("http://avatar.test/org1")
                    .withDescription("User One")
                    .withBlog("https://org1.test")
                    .withEmail("bob@test")
                    .withLocation("Unit test")
                    .withPrivateRepo("repo1")
                    .withPublicRepo("repo2")
                    .withPublicRepo("repo3");
            GHOrganization actual = openAndConnect(connectFunction, mock).getOrganization("org1");
            assertThat(actual.getLogin(), is(expected.getLogin()));
            assertThat(actual.getName(), is(expected.getName()));
            assertThat(actual.getAvatarUrl(), is(expected.getAvatarUrl()));
            assertThat(actual.getBlog(), is(expected.getBlog()));
            assertThat(actual.getId(), is(expected.getId()));
            assertThat(actual.getPublicRepoCount(), is(expected.getPublicRepos()));
        }
    }
}
