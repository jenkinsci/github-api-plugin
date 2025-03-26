package jenkins.plugins.github.api.mock;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

public class MockGitHub implements Closeable {
    private AtomicLong nextId = new AtomicLong();
    private Map<String, MockUser> users = new HashMap<>();
    private Map<String, MockOrganization> organizations = new HashMap<>();

    private HttpServer server;

    private String url;
    private JsonFactory factory = new JsonFactory();

    public String open() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", new RootHandler(this));
        server.createContext("/orgs", new OrgsHandler(this));
        server.createContext("/users", new UsersHandler(this));
        server.createContext("/repositories", new RepositoriesHandler(this));
        server.start();

        InetSocketAddress address = server.getAddress();
        url = String.format("http://%s:%d", address.getHostString(), address.getPort());
        return url;
    }

    @Override
    public void close() {
        server.stop(1);
    }

    public String getUrl() {
        return url;
    }

    public Map<String, MockUser> getUsers() {
        return users;
    }

    public Map<String, MockOrganization> getOrgs() {
        return organizations;
    }

    public long nextId() {
        return nextId.incrementAndGet();
    }

    public static String tz(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date(time));
    }

    public MockUser withUser(String login) {
        MockUser result = new MockUser(this, login);
        users.put(login, result);
        return result;
    }

    public MockOrganization withOrg(String login) {
        MockOrganization result = new MockOrganization(this, login);
        organizations.put(login, result);
        return result;
    }

    public List<MockOwner<?>> owners() {
        List<MockOwner<?>> result = new ArrayList<>(organizations.size() + users.size());
        result.addAll(users.values());
        result.addAll(organizations.values());
        return result;
    }

    private static class RootHandler implements HttpHandler {
        private final MockGitHub github;

        public RootHandler(MockGitHub github) {
            this.github = Objects.requireNonNull(github);
        }

        @Override
        public void handle(HttpExchange he) throws IOException {
            he.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            try (JsonGenerator o = github.factory.createGenerator(he.getResponseBody())) {
                o.writeStartObject();
                o.writeStringField("current_user_url", github.getUrl() + "/user");
                o.writeStringField(
                        "current_user_authorizations_html_url",
                        "https://github.com/settings/connections/applications{/client_id}");
                o.writeStringField("authorizations_url", github.getUrl() + "/authorizations");
                o.writeStringField(
                        "code_search_url", github.getUrl() + "/search/code?q={query}{&page,per_page,sort,order}");
                o.writeStringField(
                        "commit_search_url", github.getUrl() + "/search/commits?q={query}{&page,per_page,sort,order}");
                o.writeStringField("emails_url", github.getUrl() + "/user/emails");
                o.writeStringField("emojis_url", github.getUrl() + "/emojis");
                o.writeStringField("events_url", github.getUrl() + "/events");
                o.writeStringField("feeds_url", github.getUrl() + "/feeds");
                o.writeStringField("followers_url", github.getUrl() + "/user/followers");
                o.writeStringField("following_url", github.getUrl() + "/user/following{/target}");
                o.writeStringField("gists_url", github.getUrl() + "/gists{/gist_id}");
                o.writeStringField("hub_url", github.getUrl() + "/hub");
                o.writeStringField(
                        "issue_search_url", github.getUrl() + "/search/issues?q={query}{&page,per_page,sort,order}");
                o.writeStringField("issues_url", github.getUrl() + "/issues");
                o.writeStringField("keys_url", github.getUrl() + "/user/keys");
                o.writeStringField("notifications_url", github.getUrl() + "/notifications");
                o.writeStringField(
                        "organization_repositories_url",
                        github.getUrl() + "/orgs/{org}/repos{?type,page,per_page,sort}");
                o.writeStringField("organization_url", github.getUrl() + "/orgs/{org}");
                o.writeStringField("public_gists_url", github.getUrl() + "/gists/public");
                o.writeStringField("rate_limit_url", github.getUrl() + "/rate_limit");
                o.writeStringField("repository_url", github.getUrl() + "/repos/{owner}/{repo}");
                o.writeStringField(
                        "repository_search_url",
                        github.getUrl() + "/search/repositories?q={query}{&page,per_page,sort,order}");
                o.writeStringField(
                        "current_user_repositories_url", github.getUrl() + "/user/repos{?type,page,per_page,sort}");
                o.writeStringField("starred_url", github.getUrl() + "/user/starred{/owner}{/repo}");
                o.writeStringField("starred_gists_url", github.getUrl() + "/gists/starred");
                o.writeStringField("team_url", github.getUrl() + "/teams");
                o.writeStringField("user_url", github.getUrl() + "/users/{user}");
                o.writeStringField("user_organizations_url", github.getUrl() + "/user/orgs");
                o.writeStringField(
                        "user_repositories_url", github.getUrl() + "/users/{user}/repos{?type,page,per_page,sort}");
                o.writeStringField(
                        "user_search_url", github.getUrl() + "/search/users?q={query}{&page,per_page,sort,order}");
                o.writeEndObject();
            }
            he.close();
        }
    }

    private static class OrgsHandler implements HttpHandler {
        private final MockGitHub github;

        public OrgsHandler(MockGitHub github) {
            this.github = Objects.requireNonNull(github);
        }

        @Override
        public void handle(HttpExchange he) throws IOException {
            String path = he.getRequestURI().getPath();
            if (path.endsWith("/")) {
                // Handle /orgs/{org}/
                String orgName = path.substring("/orgs/".length(), path.length() - 1);
                MockOrganization org = github.getOrgs().get(orgName);
                if (org != null) {
                    he.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
                    he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                    try (JsonGenerator o = github.factory.createGenerator(he.getResponseBody())) {
                        o.writeStartObject();
                        o.writeStringField("login", org.getLogin());
                        o.writeNumberField("id", org.getId());
                        o.writeStringField("url", github.getUrl() + "/users/" + org.getLogin());
                        o.writeStringField("repos_url", github.getUrl() + "/orgs/" + org.getLogin() + "/repos");
                        o.writeStringField("events_url", github.getUrl() + "/orgs/" + org.getLogin() + "/events");
                        o.writeStringField("hooks_url", github.getUrl() + "/orgs/" + org.getLogin() + "/hooks");
                        o.writeStringField("issues_url", github.getUrl() + "/orgs/" + org.getLogin() + "/issues");
                        o.writeStringField(
                                "members_url", github.getUrl() + "/orgs/" + org.getLogin() + "/members{/member}");
                        o.writeStringField(
                                "public_members_url",
                                github.getUrl() + "/orgs/" + org.getLogin() + "/public_members{/member}");
                        o.writeStringField("avatar_url", org.getAvatarUrl());
                        o.writeStringField("description", org.getDescription());
                        o.writeStringField("name", org.getName());
                        o.writeNullField("company");
                        o.writeStringField("blog", org.getBlog());
                        o.writeStringField("location", org.getLocation());
                        o.writeStringField("email", org.getEmail());
                        o.writeBooleanField("has_organization_projects", true);
                        o.writeBooleanField("has_repository_projects", true);
                        o.writeNumberField("public_repos", org.getPublicRepos());
                        o.writeNumberField("public_gists", 0);
                        o.writeNumberField("followers", org.getFollowers());
                        o.writeNumberField("following", org.getFollowing());
                        o.writeStringField("created_at", tz(org.getCreated()));
                        o.writeStringField("updated_at", tz(org.getUpdated()));
                        o.writeStringField("type", org.getType());
                        o.writeEndObject();
                    }
                } else {
                    he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                }
            } else {
                // Handle /orgs/{org} (redirect to /orgs/{org}/)
                String orgName = path.substring("/orgs/".length());
                he.getResponseHeaders().set("Location", "/orgs/" + orgName + "/");
                he.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_TEMP, -1);
            }
            he.close();
        }
    }

    private static class UsersHandler implements HttpHandler {
        private final MockGitHub github;

        public UsersHandler(MockGitHub github) {
            this.github = Objects.requireNonNull(github);
        }

        @Override
        public void handle(HttpExchange he) throws IOException {
            String path = he.getRequestURI().getPath();
            if (path.endsWith("/")) {
                // Handle /users/{username}/
                String userName = path.substring("/users/".length(), path.length() - 1);
                MockOwner<?> owner = github.getUsers().get(userName);
                if (owner == null) {
                    owner = github.getOrgs().get(userName);
                }
                if (owner != null) {
                    he.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
                    he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                    try (JsonGenerator o = github.factory.createGenerator(he.getResponseBody())) {
                        o.writeStartObject();
                        o.writeStringField("login", owner.getLogin());
                        o.writeNumberField("id", owner.getId());
                        o.writeStringField("avatar_url", owner.getAvatarUrl());
                        o.writeStringField("gravatar_id", "");
                        o.writeStringField("url", github.getUrl() + "/users/" + owner.getLogin());
                        o.writeStringField("html_url", "https://github.com/" + owner.getLogin());
                        o.writeStringField(
                                "followers_url", github.getUrl() + "/users/" + owner.getLogin() + "/followers");
                        o.writeStringField(
                                "following_url",
                                github.getUrl() + "/users/" + owner.getLogin() + "/following{/other_user}");
                        o.writeStringField(
                                "gists_url", github.getUrl() + "/users/" + owner.getLogin() + "/gists{/gist_id}");
                        o.writeStringField(
                                "starred_url",
                                github.getUrl() + "/users/" + owner.getLogin() + "/starred{/owner}{/repo}");
                        o.writeStringField(
                                "subscriptions_url", github.getUrl() + "/users/" + owner.getLogin() + "/subscriptions");
                        o.writeStringField(
                                "organizations_url", github.getUrl() + "/users/" + owner.getLogin() + "/orgs");
                        o.writeStringField("repos_url", github.getUrl() + "/users/" + owner.getLogin() + "/repos");
                        o.writeStringField(
                                "events_url", github.getUrl() + "/users/" + owner.getLogin() + "/events{/privacy}");
                        o.writeStringField(
                                "received_events_url",
                                github.getUrl() + "/users/" + owner.getLogin() + "/received_events");
                        o.writeStringField("type", owner.getType());
                        if (owner instanceof MockUser user) {
                            o.writeBooleanField("site_admin", user.isSiteAdmin());
                        } else {
                            o.writeBooleanField("site_admin", false);
                        }
                        o.writeStringField("name", owner.getName());
                        if (owner instanceof MockUser user) {
                            o.writeStringField("company", user.getCompany());
                        } else {
                            o.writeNullField("company");
                        }
                        o.writeStringField("blog", owner.getBlog());
                        o.writeStringField("location", owner.getLocation());
                        o.writeStringField("email", owner.getEmail());
                        if (owner instanceof MockUser user) {
                            o.writeBooleanField("hireable", user.isHireable());
                            o.writeStringField("bio", user.getBio());
                        } else {
                            o.writeNullField("hireable");
                            o.writeNullField("bio");
                        }
                        o.writeNumberField("public_repos", owner.getPublicRepos());
                        o.writeNumberField("public_gists", 0);
                        o.writeNumberField("followers", owner.getFollowers());
                        o.writeNumberField("following", owner.getFollowing());
                        o.writeStringField("created_at", tz(owner.getCreated()));
                        o.writeStringField("updated_at", tz(owner.getUpdated()));
                        o.writeEndObject();
                    }
                } else {
                    he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                }
            } else {
                // Handle /users/{username} (redirect to /users/{username}/)
                String userName = path.substring("/users/".length());
                he.getResponseHeaders().set("Location", "/users/" + userName + "/");
                he.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_TEMP, -1);
            }
            he.close();
        }
    }

    private static class RepositoriesHandler implements HttpHandler {
        private final MockGitHub github;

        public RepositoriesHandler(MockGitHub github) {
            this.github = Objects.requireNonNull(github);
        }

        @Override
        public void handle(HttpExchange he) throws IOException {
            String query = he.getRequestURI().getQuery();
            long since = 0;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "since".equals(pair[0])) {
                        since = Long.parseLong(pair[1]);
                        break;
                    }
                }
            }
            List<MockRepository> repositories = new ArrayList<>();
            for (MockOwner<?> o : github.owners()) {
                for (MockRepository r : o.repositories().values()) {
                    if (r.getId() > since && !r.isPrivate()) {
                        repositories.add(r);
                    }
                }
            }
            repositories.sort(Comparator.comparingLong(MockObject::getId));
            if (repositories.size() > 30) {
                he.getResponseHeaders()
                        .set(
                                "Link",
                                String.format(
                                        "<%s/repositories?since=%d>; rel=\"next\", <%s/repositories{?since}>; rel=\"first\"",
                                        github.getUrl(), repositories.get(30).getId(), github.getUrl()));
            } else {
                he.getResponseHeaders()
                        .set("Link", String.format("<%s/repositories{?since}>; rel=\"first\"", github.getUrl()));
            }
            he.getResponseHeaders().set("Content-Type", "application/json;charset=utf-8");
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
            try (JsonGenerator o = github.factory.createGenerator(he.getResponseBody())) {
                o.writeStartArray();
                for (MockRepository r : repositories.subList(0, Math.min(30, repositories.size()))) {
                    o.writeStartObject();
                    o.writeNumberField("id", r.getId());
                    o.writeStringField("name", r.getName());
                    o.writeStringField("full_name", r.owner().getLogin() + "/" + r.getName());
                    o.writeFieldName("owner");
                    o.writeStartObject();
                    o.writeStringField("login", r.owner().getLogin());
                    o.writeNumberField("id", r.owner().getId());
                    o.writeStringField("avatar_url", r.owner().getAvatarUrl());
                    o.writeStringField("type", r.owner().getType());
                    o.writeEndObject();
                    o.writeBooleanField("private", r.isPrivate());
                    o.writeStringField(
                            "html_url", "https://github.com/" + r.owner().getLogin() + "/" + r.getName());
                    o.writeEndObject();
                }
                o.writeEndArray();
            }
            he.close();
        }
    }
}
