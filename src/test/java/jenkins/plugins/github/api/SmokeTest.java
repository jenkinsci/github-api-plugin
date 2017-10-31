package jenkins.plugins.github.api;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.TreeSet;
import jenkins.plugins.github.api.mock.MockGitHub;
import jenkins.plugins.github.api.mock.MockOrganization;
import jenkins.plugins.github.api.mock.MockUser;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SmokeTest {
    @Test
    public void given__veryBasicMockGitHub__when__connectingAnonymously__then__apiUrlValid() throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            GitHub.connectToEnterpriseAnonymously(mock.open()).checkApiUrlValidity();
        }
    }

    @Test
    public void given__veryBasicMockGitHub__when__listingRepos__then__reposListed() throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            mock.withOrg("org1").withPublicRepo("repo1").withPrivateRepo("repo2");
            mock.withOrg("org2").withPublicRepo("repo3");
            mock.withUser("user1").withPublicRepo("repo4").withPrivateRepo("repo5");
            Set<String> names = new TreeSet<>();
            for (GHRepository r: GitHub.connectToEnterpriseAnonymously(mock.open()).listAllPublicRepositories()) {
                names.add(r.getFullName());
            }
            assertThat(names, contains("org1/repo1", "org2/repo3", "user1/repo4"));
        }
    }

    @Test
    public void given__veryBasicMockGitHub__when__listingManyRepos__then__reposListed() throws Exception {
        try (MockGitHub mock = new MockGitHub()) {
            MockOrganization org1 = mock.withOrg("org1");
            Set<String> expected = new TreeSet<>();
            for (int i = 0; i < 95; i++) {
                org1.withPublicRepo("repo"+i);
                expected.add("org1/repo"+i);

            }
            Set<String> actual = new TreeSet<>();
            for (GHRepository r: GitHub.connectToEnterpriseAnonymously(mock.open()).listAllPublicRepositories()) {
                actual.add(r.getFullName());
            }
            assertThat(actual, is(actual));
        }
    }

    @Test
    public void given__veryBasicMockGitHub__when__gettingUser__then__userReturned() throws Exception {
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
            GHUser actual = GitHub.connectToEnterpriseAnonymously(mock.open()).getUser("user1");
            assertThat(actual.getLogin(), is(expected.getLogin()));
            assertThat(actual.getName(), is(expected.getName()));
            assertThat(actual.getAvatarUrl(), is(expected.getAvatarUrl()));
            assertThat(actual.getBlog(), is(expected.getBlog()));
            assertThat(actual.getCompany(), is(expected.getCompany()));
            assertThat(actual.getId(), is((long)expected.getId()));
            assertThat(actual.getPublicRepoCount(), is(expected.getPublicRepos()));
        }
    }
    @Test
    public void given__veryBasicMockGitHub__when__gettingOrg__then__orgReturned() throws Exception {
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
            GHOrganization actual = GitHub.connectToEnterpriseAnonymously(mock.open()).getOrganization("org1");
            assertThat(actual.getLogin(), is(expected.getLogin()));
            assertThat(actual.getName(), is(expected.getName()));
            assertThat(actual.getAvatarUrl(), is(expected.getAvatarUrl()));
            assertThat(actual.getBlog(), is(expected.getBlog()));
            assertThat(actual.getId(), is((long)expected.getId()));
            assertThat(actual.getPublicRepoCount(), is(expected.getPublicRepos()));
        }
    }
}
